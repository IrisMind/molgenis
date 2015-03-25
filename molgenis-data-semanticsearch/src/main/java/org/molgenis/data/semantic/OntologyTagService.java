package org.molgenis.data.semantic;

import static java.util.stream.StreamSupport.stream;
import static org.molgenis.data.meta.EntityMetaDataMetaData.ATTRIBUTES;
import static org.molgenis.data.meta.EntityMetaDataMetaData.ENTITY_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.molgenis.data.AttributeMetaData;
import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.IdGenerator;
import org.molgenis.data.Package;
import org.molgenis.data.UnknownEntityException;
import org.molgenis.data.meta.AttributeMetaDataMetaData;
import org.molgenis.data.meta.EntityMetaDataMetaData;
import org.molgenis.data.meta.PackageMetaData;
import org.molgenis.data.meta.TagMetaData;
import org.molgenis.data.support.DefaultEntity;
import org.molgenis.data.support.QueryImpl;
import org.molgenis.ontology.OntologyService;
import org.molgenis.ontology.repository.model.Ontology;
import org.molgenis.ontology.repository.model.OntologyTerm;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * Service to tag metadata with ontology terms.
 */
public class OntologyTagService implements TagService<OntologyTerm, Ontology>
{
	private final DataService dataService;
	private final TagRepository tagRepository;
	private final OntologyService ontologyService;
	private final IdGenerator idGenerator;

	public OntologyTagService(DataService dataService, OntologyService ontologyService, TagRepository tagRepository,
			IdGenerator idGenerator)
	{
		this.dataService = dataService;
		this.tagRepository = tagRepository;
		this.ontologyService = ontologyService;
		this.idGenerator = idGenerator;
	}

	private Entity findAttributeEntity(String entityName, String attributeName)
	{
		Entity entityMetaDataEntity = dataService.findOne(ENTITY_NAME, entityName);
		return stream(entityMetaDataEntity.getEntities(ATTRIBUTES).spliterator(), false)
				.filter(att -> attributeName.equals(att.getString(AttributeMetaDataMetaData.NAME))).findFirst().get();
	}

	public void removeAttributeTag(String entity, String attribute, String relationIRI, String ontologyTermIRI)
	{
		Entity attributeEntity = findAttributeEntity(entity, attribute);
		Iterable<Entity> tags = attributeEntity.getEntities(AttributeMetaDataMetaData.TAGS);
		Iterable<Entity> newTags = Iterables.filter(tags, e -> !isSameTag(relationIRI, ontologyTermIRI, e));
		attributeEntity.set(AttributeMetaDataMetaData.TAGS, newTags);
		dataService.update(AttributeMetaDataMetaData.ENTITY_NAME, attributeEntity);
		updateEntityMetaDataEntityWithNewAttributeEntity(entity, attribute, attributeEntity);
	}

	private boolean isSameTag(String relationIRI, String ontologyTermIRI, Entity e)
	{
		return ontologyTermIRI.equals(e.getString(TagMetaData.OBJECT_IRI))
				&& relationIRI.equals(e.getString(TagMetaData.RELATION_IRI));
	}

	@Override
	public void removeAttributeTag(EntityMetaData entityMetaData,
			Tag<AttributeMetaData, OntologyTerm, Ontology> removeTag)
	{
		AttributeMetaData attributeMetaData = removeTag.getSubject();
		Entity attributeEntity = findAttributeEntity(entityMetaData.getName(), attributeMetaData.getName());
		List<Entity> tags = new ArrayList<Entity>();
		for (Entity tagEntity : attributeEntity.getEntities(AttributeMetaDataMetaData.TAGS))
		{
			Tag<AttributeMetaData, OntologyTerm, Ontology> tag = asTag(attributeMetaData, tagEntity);
			if (!removeTag.equals(tag))
			{
				tags.add(tagEntity);
			}
		}
		attributeEntity.set(AttributeMetaDataMetaData.TAGS, tags);
		dataService.update(AttributeMetaDataMetaData.ENTITY_NAME, attributeEntity);
	}

	@Override
	public Multimap<Relation, OntologyTerm> getTagsForAttribute(EntityMetaData entityMetaData,
			AttributeMetaData attributeMetaData)
	{
		Entity entity = findAttributeEntity(entityMetaData.getName(), attributeMetaData.getName());
		Multimap<Relation, OntologyTerm> tags = ArrayListMultimap.<Relation, OntologyTerm> create();
		for (Entity tagEntity : entity.getEntities(AttributeMetaDataMetaData.TAGS))
		{
			Tag<AttributeMetaData, OntologyTerm, Ontology> tag = asTag(attributeMetaData, tagEntity);
			tags.put(tag.getRelation(), tag.getObject());
		}
		return tags;
	}

	@Override
	public Iterable<Tag<Package, OntologyTerm, Ontology>> getTagsForPackage(Package p)
	{
		Entity packageEntity = dataService.findOne(PackageMetaData.ENTITY_NAME,
				new QueryImpl().eq(PackageMetaData.FULL_NAME, p.getName()));

		if (packageEntity == null)
		{
			throw new UnknownEntityException("Unknown package [" + p.getName() + "]");
		}

		List<Tag<Package, OntologyTerm, Ontology>> tags = Lists.newArrayList();
		for (Entity tagEntity : packageEntity.getEntities(PackageMetaData.TAGS))
		{
			tags.add(asTag(p, tagEntity));
		}

		return tags;
	}

	private <SubjectType> TagImpl<SubjectType, OntologyTerm, Ontology> asTag(SubjectType subjectType, Entity tagEntity)
	{
		String identifier = tagEntity.getString(TagMetaData.IDENTIFIER);
		Relation relation = asRelation(tagEntity);
		Ontology ontology = asOntology(tagEntity);
		OntologyTerm ontologyTerm = asOntologyTerm(tagEntity);
		if (relation == null || ontologyTerm == null)
		{
			return null;
		}
		return new TagImpl<SubjectType, OntologyTerm, Ontology>(identifier, subjectType, relation, ontologyTerm,
				ontology);
	}

	private static Relation asRelation(Entity tagEntity)
	{
		String relationIRI = tagEntity.getString(TagMetaData.RELATION_IRI);
		if (relationIRI == null)
		{
			return null;
		}
		return Relation.forIRI(relationIRI);
	}

	private OntologyTerm asOntologyTerm(Entity tagEntity)
	{
		String objectIRI = tagEntity.getString(TagMetaData.OBJECT_IRI);
		if (objectIRI == null)
		{
			return null;
		}
		return ontologyService.getOntologyTerm(objectIRI);
	}

	private Ontology asOntology(Entity tagEntity)
	{
		String codeSystemIRI = tagEntity.getString(TagMetaData.CODE_SYSTEM);
		if (codeSystemIRI == null)
		{
			return null;
		}
		return ontologyService.getOntology(codeSystemIRI);
	}

	@Override
	public void addAttributeTag(EntityMetaData entityMetaData, Tag<AttributeMetaData, OntologyTerm, Ontology> tag)
	{
		Entity entity = findAttributeEntity(entityMetaData.getName(), tag.getSubject().getName());
		List<Entity> tags = new ArrayList<Entity>();
		for (Entity tagEntity : entity.getEntities(AttributeMetaDataMetaData.TAGS))
		{
			tags.add(tagEntity);
		}
		tags.add(getTagEntity(tag));
		entity.set(AttributeMetaDataMetaData.TAGS, tags);
		dataService.update(AttributeMetaDataMetaData.ENTITY_NAME, entity);
	}

	public void addAttributeTag(String entity, String attribute, String relationIRI, List<String> ontologyTermIRIs)
	{
		Entity attributeEntity = findAttributeEntity(entity, attribute);
		List<Entity> tags = Lists.<Entity> newArrayList(attributeEntity.getEntities(AttributeMetaDataMetaData.TAGS));
		Entity tagEntity = new DefaultEntity(TagRepository.META_DATA, dataService);
		Stream<OntologyTerm> terms = ontologyTermIRIs.stream().map(ontologyService::getOntologyTerm);
		OntologyTerm combinedOntologyTerm = OntologyTerm.and(terms.toArray(OntologyTerm[]::new));
		Relation relation = Relation.forIRI(relationIRI);
		tagEntity.set(TagMetaData.IDENTIFIER, idGenerator.generateId());
		tagEntity.set(TagMetaData.CODE_SYSTEM, null);
		tagEntity.set(TagMetaData.RELATION_IRI, relation.getIRI());
		tagEntity.set(TagMetaData.RELATION_LABEL, relation.getLabel());
		tagEntity.set(TagMetaData.LABEL, combinedOntologyTerm.getLabel());
		tagEntity.set(TagMetaData.OBJECT_IRI, combinedOntologyTerm.getIRI());
		dataService.add(TagMetaData.ENTITY_NAME, tagEntity);
		tags.add(tagEntity);
		attributeEntity.set(AttributeMetaDataMetaData.TAGS, tags);
		dataService.update(AttributeMetaDataMetaData.ENTITY_NAME, attributeEntity);
		updateEntityMetaDataEntityWithNewAttributeEntity(entity, attribute, attributeEntity);
	}

	/**
	 * The attribute just got updated, but the entity does not know this yet. To reindex this document in elasticsearch,
	 * update it.
	 * 
	 * @param entity
	 *            name of the entity
	 * @param attribute
	 *            the name of the attribute that got changed
	 * @param attributeEntity
	 *            the entity of the attribute that got changed
	 */
	private void updateEntityMetaDataEntityWithNewAttributeEntity(String entity, String attribute,
			Entity attributeEntity)
	{
		Entity entityEntity = dataService.findOne(EntityMetaDataMetaData.ENTITY_NAME, entity);
		Iterable<Entity> attributes = entityEntity.getEntities(ATTRIBUTES);
		entityEntity.set(ATTRIBUTES, Iterables.transform(attributes,
				att -> att.getString(AttributeMetaDataMetaData.NAME).equals(attribute) ? attributeEntity : att));
		dataService.update(EntityMetaDataMetaData.ENTITY_NAME, entityEntity);
	}

	Entity getTagEntity(Tag<?, OntologyTerm, Ontology> tag)
	{
		return tagRepository.getTagEntity(tag.getObject().getIRI(), tag.getObject().getLabel(), tag.getRelation(), tag
				.getCodeSystem().getIRI());
	}

	@Override
	public void addEntityTag(Tag<EntityMetaData, OntologyTerm, Ontology> tag)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void removeEntityTag(Tag<EntityMetaData, OntologyTerm, Ontology> tag)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Iterable<Tag<EntityMetaData, LabeledResource, LabeledResource>> getTagsForEntity(
			EntityMetaData entityMetaData)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
