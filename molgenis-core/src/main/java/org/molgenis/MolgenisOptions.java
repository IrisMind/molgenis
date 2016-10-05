package org.molgenis;

import org.molgenis.util.cmdline.CmdLineException;
import org.molgenis.util.cmdline.CmdLineParser;
import org.molgenis.util.cmdline.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Option to parameterize Molgenis and the MolgenisServer
 *
 * @author Morris Swertz
 */
public class MolgenisOptions implements Serializable
{
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(MolgenisOptions.class);

	final public static String CLASS_PER_TABLE = "class_per_table";
	final public static String SUBCLASS_PER_TABLE = "subclass_per_table";
	final public static String HIERARCHY_PER_TABLE = "hierarchy_per_table";

	/**
	 * Properties file where this data came from
	 */
	private String molgenis_properties = "";

	/**
	 * relative paths to the data model XML files. Discussion: is COLLECTION good enough here?
	 */
	@Option(name = "model_database", param = Option.Param.COLLECTION, type = Option.Type.REQUIRED_ARGUMENT, usage = "File with data structure specification (in MOLGENIS DSL). Default: new ArrayList<String>()")
	public ArrayList<String> model_database = new ArrayList<String>();

	/**
	 * relative paths to the data model XML files, objects in these files are used only, not generated
	 */
	@Option(name = "import_model_database", param = Option.Param.COLLECTION, type = Option.Type.REQUIRED_ARGUMENT, usage = "File with data structure specification (in MOLGENIS DSL). Default: new ArrayList<String>()")
	public ArrayList<String> import_model_database = new ArrayList<String>();

	@Option(name = "output_dir", param = Option.Param.DIRPATH, type = Option.Type.OPTIONAL_ARGUMENT, usage = "Directory where all generated code is stored")
	public String output_dir = "generated";

	/**
	 * Source directory for generated java
	 */
	@Option(name = "output_src", param = Option.Param.DIRPATH, type = Option.Type.REQUIRED_ARGUMENT, usage = "Output-directory for the generated Java classes. Default: 'generated/java'")
	public String output_src = output_dir + "/java";

	/**
	 * Source directory for handwritten java
	 */
	@Option(name = "output_hand", param = Option.Param.DIRPATH, type = Option.Type.REQUIRED_ARGUMENT, usage = "Source directory for handwritten java. Default: 'handwritten/java'")
	public String output_hand = "handwritten/java";

	/**
	 * Source directory for generated sql
	 */
	@Option(name = "output_sql", param = Option.Param.DIRPATH, type = Option.Type.REQUIRED_ARGUMENT, usage = "Output-directory for the generated sql files. Default: 'generated/sql'")
	public String output_sql = output_dir + "/sql";

	/**
	 * Source directory for generated doc
	 */
	@Option(name = "output_doc", param = Option.Param.DIRPATH, type = Option.Type.REQUIRED_ARGUMENT, usage = "Output-directory for the generated documentation. Default: 'WebContent/generated-doc'")
	public String output_doc = "WebContent/generated-doc";

	/**
	 * Source directory for web content
	 */
	@Option(name = "output_web", param = Option.Param.DIRPATH, type = Option.Type.REQUIRED_ARGUMENT, usage = "Output-directory for any generated web resources. Default: 'WebContent'")
	public String output_web = "WebContent";

	/**
	 * Class folder with overrides for decorators
	 */
	@Option(name = "decorator_overriders", param = Option.Param.CLASS, type = Option.Type.OPTIONAL_ARGUMENT, usage = "Points to an application package with overriding classes for entity decorators, mapped by name. Default: ''")
	public String decorator_overriders = "";

	/**
	 * Path where file attachments (&lt;field type="file" ... &gt;) should be stored.
	 */
	@Option(name = "db_filepath", param = Option.Param.DIRPATH, type = Option.Type.REQUIRED_ARGUMENT, usage = "Path where the database should store file attachements. Default: 'data'")
	public String db_filepath = "data";

	/**
	 * Advanced option: Type of object relational mapping.
	 */
	@Option(name = "object_relational_mapping", param = Option.Param.STRING, type = Option.Type.OPTIONAL_ARGUMENT, usage = "Expert option: Choosing OR strategy. Either 'class_per_table', 'subclass_per_table', 'hierarchy_per_table'. Default: SUBCLASS_PER_TABLE")
	public String object_relational_mapping = SUBCLASS_PER_TABLE;

	@Option(name = "generate_persistence", param = Option.Param.BOOLEAN, type = Option.Type.OPTIONAL_ARGUMENT, usage = "Expert option: Choosing whether persistence.xml is generated by molgenis (true) or supplied by user (false). Default: true")
	public boolean generate_persistence = true;

	@Option(name = "generate_jpa", param = Option.Param.BOOLEAN, type = Option.Type.OPTIONAL_ARGUMENT, usage = "Generate JPA related classes. Default: true")
	public boolean generate_jpa = true;

	@Option(name = "jpa_use_sequence", param = Option.Param.BOOLEAN, type = Option.Type.OPTIONAL_ARGUMENT, usage = "Expert option: Choosing whether sequence are used to generate primary key (true) or auto (false: default)")
	public boolean jpa_use_sequence = false;

	@Option(name = "generate_db", param = Option.Param.BOOLEAN, type = Option.Type.OPTIONAL_ARGUMENT, usage = "generate database. Default: true")
	public boolean generate_db = true;

	/**
	 * Advanced option: skip entities marked as 'system="true"'
	 */
	@Option(name = "exclude_system", param = Option.Param.BOOLEAN, type = Option.Type.REQUIRED_ARGUMENT, usage = "Expert option: Whether system tables should be excluded from generation. Default: true")
	public boolean exclude_system = true;

	@Option(name = "generate_entityio", param = Option.Param.BOOLEAN, type = Option.Type.OPTIONAL_ARGUMENT, usage = "Should entity importers and exporters be generated. Default: true.")
	public boolean generate_entityio = true;

	@Option(name = "generate_tests", param = Option.Param.BOOLEAN, type = Option.Type.OPTIONAL_ARGUMENT, usage = "Should test code for generated code be generated. Default: false.")
	public boolean generate_tests = false;

	@Option(name = "generate_model", param = Option.Param.BOOLEAN, type = Option.Type.OPTIONAL_ARGUMENT, usage = "Generate any SQL related classes. Default: true")
	public boolean generate_model = true;

	@Option(name = "delete_generated_folder", param = Option.Param.BOOLEAN, type = Option.Type.OPTIONAL_ARGUMENT, usage = "delete generated Folder before generators are executed. Default: true")
	// default set to false as partial generation leads to compile problems in
	// for example molgenis_apps
	public boolean delete_generated_folder = true;

	@Option(name = "authorizable", param = Option.Param.COLLECTION, type = Option.Type.OPTIONAL_ARGUMENT, usage = "For use in molgenis_apps! Tells the generator on which entities to append an implements='Authorizable'. Default: new ArrayList<String>()")
	public ArrayList<String> authorizable = new ArrayList<String>();

	@Option(name = "disable_decorators", param = Option.Param.BOOLEAN, type = Option.Type.OPTIONAL_ARGUMENT, usage = "disables all decorators for generated test")
	public boolean disable_decorators = false;

	@Option(name = "block_webspiders", param = Option.Param.BOOLEAN, type = Option.Type.OPTIONAL_ARGUMENT, usage = "Expert option: Block webcrawler user agents in FrontController")
	public boolean block_webspiders = false;

	@Option(name = "generate_jpa_repository_source", param = Option.Param.BOOLEAN, type = Option.Type.OPTIONAL_ARGUMENT, usage = "generate JpaRepositorySource. Default: false")
	public boolean generate_jpa_repository_source = false;

	/**
	 * Initialize with the defaults
	 */
	public MolgenisOptions()
	{

	}

	/**
	 * Get the options as a map, used in the UsedMolgenisOptionsGen.ftl template
	 *
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> getOptionsAsMap() throws Exception
	{
		HashMap<String, Object> result = new HashMap<String, Object>();
		// use reflection to get the Fields
		Field[] fields = this.getClass().getDeclaredFields();

		for (int i = 0; i < fields.length; i++)
		{
			// only include the annotated fields
			if (fields[i].isAnnotationPresent(Option.class))
			{
				Option opt = fields[i].getAnnotation(Option.class);
				if (opt.param() == Option.Param.PASSWORD)
				{
					result.put(opt.name(), "xxxxxx");
				}
				else
				{
					result.put(opt.name(), fields[i].get(this));
				}
			}
		}
		return result;
	}

	/**
	 * Initialize options from properties object
	 *
	 * @param properties
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws CmdLineException
	 */
	public MolgenisOptions(Properties properties)
	{
		CmdLineParser parser;
		try
		{
			parser = new CmdLineParser(this);
			parser.parse(properties);
			LOG.debug("parsed properties file.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException("Cannot find property file: " + e.getMessage());
		}
	}

	@Override
	public String toString()
	{
		try
		{
			return new CmdLineParser(this).toString(this);
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
		}
		catch (CmdLineException e)
		{
			e.printStackTrace();
		}
		return "";
	}

	public ArrayList<String> getModelDatabase()
	{
		return model_database;
	}

	public void setModelDatabase(ArrayList<String> model_database)
	{
		this.model_database = model_database;
	}

	public void setModelDatabase(String model_database)
	{
		ArrayList<String> v = new ArrayList<String>();
		v.add(model_database);
		this.model_database = v;
	}

	public String getOutputSrc()
	{
		return output_src;
	}

	public void setOutputSrc(String output_src)
	{
		this.output_src = output_src;
	}

	public String getOutputHand()
	{
		return output_hand;
	}

	public void setOutputHand(String output_hand)
	{
		this.output_hand = output_hand;
	}

	public String getOutputSql()
	{
		return output_sql;
	}

	public void setOutputSql(String output_sql)
	{
		this.output_sql = output_sql;
	}

	public String getOutputDoc()
	{
		return output_doc;
	}

	public void setOutputDoc(String output_doc)
	{
		this.output_doc = output_doc;
	}

	public String getOutputWeb()
	{
		return output_web;
	}

	public void setOutputWeb(String output_web)
	{
		this.output_web = output_web;
	}

	public String getDbFilepath()
	{
		return db_filepath;
	}

	public void setDbFilepath(String db_filepath)
	{
		this.db_filepath = db_filepath;
	}

	public String getObjectRelationalMapping()
	{
		return object_relational_mapping;
	}

	public void setObjectRelationalMapping(String object_relational_mapping)
	{
		this.object_relational_mapping = object_relational_mapping;
	}

	public boolean isExcludeSystem()
	{
		return exclude_system;
	}

	public void setExcludeSystem(boolean exclude_system)
	{
		this.exclude_system = exclude_system;
	}

	// internal
	public String path = "";

	public String getPath()
	{
		return path;
	}

	public void setPath(String path)
	{
		this.path = path;
	}

	public String getMolgenis_properties()
	{
		return molgenis_properties;
	}

	public void setMolgenis_properties(String molgenisProperties)
	{
		molgenis_properties = molgenisProperties;
	}

	public boolean isDisable_decorators()
	{
		return disable_decorators;
	}

	public void setDisable_decorators(boolean disable_decorators)
	{
		this.disable_decorators = disable_decorators;
	}

	public void setGenerateTests(boolean generate_tests)
	{
		this.generate_tests = generate_tests;
	}
}
