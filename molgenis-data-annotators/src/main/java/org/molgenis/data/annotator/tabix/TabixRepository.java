package org.molgenis.data.annotator.tabix;

import au.com.bytecode.opencsv.CSVParser;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import org.molgenis.data.*;
import org.molgenis.data.support.AbstractRepository;
import org.molgenis.data.support.MapEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.vcf.VcfRepository.CHROM;
import static org.molgenis.data.vcf.VcfRepository.POS;

public class TabixRepository extends AbstractRepository
{
	private static final Logger LOG = LoggerFactory.getLogger(TabixRepository.class);

	private TabixReader reader;
	private EntityMetaData entityMetaData;
	private final String chromosomeAttributeName;
	private final String positionAttributeName;
	private Jedis jedis;

	/**
	 * Creates a new {@link TabixRepository}
	 * 
	 * @param file
	 *            the Tabix file
	 * @param entityMetaData
	 *            {@link EntityMetaData} for the tabix file. Attributes should be in the order of the file's columns
	 * @throws IOException
	 *             if something goes wrong creating the {@link TabixReader} for the file
	 */
	public TabixRepository(File file, EntityMetaData entityMetaData) throws IOException
	{
		this(file, -1, entityMetaData, CHROM, POS);
	}

	public TabixRepository(File file, Integer redisDBIndex, EntityMetaData entityMetaData) throws IOException
	{
		this(file, redisDBIndex, entityMetaData, CHROM, POS);
	}

	public TabixRepository(File file, Integer redisDBIndex, EntityMetaData entityMetaData, String chromosomeAttributeName,
			String positionAttributeName) throws IOException
	{
		this.entityMetaData = entityMetaData;
		this.reader = new TabixReader(file.getAbsolutePath());
		this.chromosomeAttributeName = requireNonNull(chromosomeAttributeName);
		this.positionAttributeName = requireNonNull(positionAttributeName);
		this.jedis = new Jedis();
		jedis.select(redisDBIndex);
	}

	TabixRepository(TabixReader reader, Integer redisDBIndex, EntityMetaData entityMetaData, String chromosomeAttributeName,
			String positionAttributeName)
	{
		this.reader = requireNonNull(reader);
		this.entityMetaData = requireNonNull(entityMetaData);
		this.chromosomeAttributeName = requireNonNull(chromosomeAttributeName);
		this.positionAttributeName = requireNonNull(positionAttributeName);
		this.jedis = new Jedis();
		jedis.select(redisDBIndex);
	}

	public static CSVParser getCsvParser()
	{
		return new CSVParser('\t');
	}

	@Override
	public Set<RepositoryCapability> getCapabilities()
	{
		return Collections.singleton(RepositoryCapability.QUERYABLE);
	}

	@Override
	public EntityMetaData getEntityMetaData()
	{
		return entityMetaData;
	}

	@Override
	public Stream<Entity> findAll(Query q)
	{
		Object posValue = getFirstEqualsValueFor(positionAttributeName, q);
		Object chromValue = getFirstEqualsValueFor(chromosomeAttributeName, q);
		List<Entity> result = new ArrayList<Entity>();

		// if one of both required attributes is null, skip the query and return an empty list
		if (posValue != null && chromValue != null)
		{
			long posLongValue = Long.parseLong(posValue.toString());
			String chromStringValue = chromValue.toString();
			result = query(chromStringValue, Long.valueOf(posLongValue));
		}
		return result.stream();
	}

	/**
	 * Queries the tabix reader.
	 * 
	 * @param chrom
	 *            name of the chromosome
	 * @param pos
	 *            position
	 * @return {@link ImmutableList} of entities found
	 */
	private synchronized ImmutableList<Entity> query(String chrom, long pos)
	{
		String queryString = String.format("%s:%s-%2$s", chrom, pos);
		LOG.debug("query({})", queryString);
		Builder<Entity> builder = ImmutableList.<Entity> builder();
		try
		{
			String redisResult = null;
			try {
				redisResult = jedis.get(String.format("%s:%s", chrom, pos));
			} catch (redis.clients.jedis.exceptions.JedisConnectionException e){
				LOG.error("Unable to query Redis, falling back to Tabix Query : " + e.getStackTrace());
			} catch (redis.clients.jedis.exceptions.JedisDataException e){
				LOG.error("Unable to query Redis, falling back to Tabix Query : " + e.getStackTrace());
			}
			if (redisResult != null)
			{
				String[] lines = redisResult.split("\n", 0);
				for(int i = 0; i < lines.length; i++){
					Entity entity = toEntity(String.format("%s\t%s\t%s", chrom, pos, lines[i]));
					if (entity.getLong(positionAttributeName) == pos)
					{
						builder.add(entity);
					}
					else
					{
						LOG.warn("RedisReader returns entity that does not match the query!");
					}
				}
			} else {
				org.molgenis.data.annotator.tabix.TabixReader.Iterator iterator = reader.query(queryString);
				if (iterator != null)
				{
					LOG.error(String.format("Redis miss: %s", queryString));
					String line = iterator.next();
					List<String> list = new ArrayList<>();
					while (line != null)
					{
						Entity entity = toEntity(line);
						if (entity.getLong(positionAttributeName) == pos)
						{
							builder.add(entity);
						}
						else
						{
							LOG.warn("TabixReader returns entity that does not match the query!");
						}
						list.add(line.substring(line.indexOf("\t", line.indexOf("\t")+1)+1));
						line = iterator.next();
					}

					String result = String.join("\n", list);
					jedis.set(String.format("%s:%s", chrom, pos), result);
				}
				else
				{
					return ImmutableList.of(); // empty list
				}
			}
		}
		catch (IOException e)
		{
			LOG.error("Error reading from tabix resource", e);
		}
		catch (NullPointerException e)
		{
			LOG.warn("Unable to read from tabix resource for query: " + queryString
					+ " (Position not present in resource file?)");
			LOG.debug("", e);
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			LOG.warn("Unable to read from tabix resource for query: " + queryString
					+ " (Chromosome not present in resource file?)");
			LOG.debug("", e);
		}
		return builder.build();
	}

	private static Object getFirstEqualsValueFor(String attributeName, Query q)
	{
		return q.getRules().stream().filter(rule -> attributeName.equals(rule.getField())).findFirst().get().getValue();
	}

	protected Entity toEntity(String line) throws IOException
	{
		Entity result = new MapEntity(entityMetaData);
		CSVParser csvParser = getCsvParser();
		String[] columns = csvParser.parseLine(line);
		int i = 0;
		for (AttributeMetaData amd : entityMetaData.getAtomicAttributes())
		{
			if (i < columns.length)
			{
				result.set(amd.getName(), DataConverter.convert(columns[i++], amd));
			}
		}
		return result;
	}

	private class TabixRepositoryIterator implements Iterator<Entity>
	{
		private String nextLine = null;

		@Override
		public boolean hasNext()
		{
			if (nextLine != null)
			{
				return true;
			}
			try
			{
				nextLine = reader.readLine();
				return nextLine != null;
			}
			catch (IOException e)
			{
				return false;
			}
		}

		@Override
		public Entity next()
		{
			if (!hasNext())
			{
				throw new NoSuchElementException();
			}
			try
			{
				return toEntity(nextLine);
			}
			catch (IOException e)
			{
				throw new NoSuchElementException();
			}
			finally
			{
				nextLine = null;
			}
		}
	}

	@Override
	public Iterator<Entity> iterator()
	{
		return new TabixRepositoryIterator();
	}
}