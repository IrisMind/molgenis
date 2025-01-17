# Using an IDE (Intellij)

A rough guide on how to develop molgenis in IntelliJ IDEA is described below.
This was created using IntelliJ IDEA 2016.1 Ultimate, courteously provided to us by
JetBrains on an open source license. Most of the time we use the latest IntelliJ version.

**[Deploy backend services](guide-development.md)**

## Set the maven home dir in IntelliJ
* Open Preferences, Build Execution Deployment, Maven
* Set the home dir to where your >= 3.6.0 maven installation lives.

## Get the molgenis sourcecode
* File, New, Project from version control, Git (or Github)
* Pick `https://github.com/molgenis/molgenis.git` for the repository URL.
* In the Event log: Non-managed pom.xml file found. Choose "Add as Maven Project". (You'll also see 'JPA framework is detected'. Ignore that.)

You now should have a Project called molgenis with a module for each maven module.

## Set the project JDK
Open File, Project Structure. For the entire project:
* (Add and) select Java 11 JDK.
* Set language level to Java 11.

## Create maven run configuration
For normal operation, IntelliJ can compile the sources for you. But some classes get
generated so you'll need to run the maven build at least once.
* Run, Edit Configurations...
* Press `+`, add new maven run config, call it `molgenis [clean install]`
* Set the project location as run directory
* Command line is `clean install`
* In the tool bar, select the `molgenis[clean install]` configuration and press the play button.
Molgenis now should compile and test. In particular the molgenis-core-ui module may take a while the first time it is run.

## Build in IntelliJ
Build, Make project should work fine now and spew out no errors, only warnings.

## Java Code Style settings
We format our code according to the Google Java Format.
You can run the formatter in maven using target `fmt:format`.
Maven will invoke `fmt:check` at build time and fail if the code isn't formatted properly.

* Install and enable the [IntelliJ plugin which replaces the Reformat Code action](https://plugins.jetbrains.com/plugin/8527-google-java-format).
* Download and import the [IntelliJ Java Google Style file](https://github.com/molgenis/molgenis/blob/master/intellij-molgenis-style.xml) to fix the import order.
* Uncheck the reformat checkbox in the git commit dialog, [it is broken](https://github.com/google/google-java-format/issues/228).

## Git commit hook
If you miss the reformat checkbox, you can configure a git commit hook in the folder where you checked out the molgenis repository.
Add a file called `.git/hooks/pre-commit` with the following contents:

```#!/bin/sh
mvn fmt:format
```

## Use MOLGENIS file-templates

* Make sure that your .idea project folder lives inside your molgenis git project root! Only then will we be able to auto-share templates.
* Goto 'New / Edit file templates' and switch from 'Default' to 'Project'-scope.
* Restart IntelliJ after setting the .idea directory and changing the settings

### Usage
Adds two file templates for creating Package and EntityType classes. The templates can be accessed from the 'New' menu, when right-clicking a java package:

![Pick file template](images/intellij/pick-file-template.png?raw=true, "pick-file-template")

When choosing EntityType and filling out the form...

![Set filename in  template](images/intellij/set-filename-in-template.png?raw=true, "set-file-name-in-template")

... a file is created with the necessary boilerplate:


```java
package org.molgenis.test;

import org.molgenis.data.Entity;
import org.molgenis.data.meta.SystemEntityType;
import org.molgenis.data.meta.model.EntityType;
import org.molgenis.data.support.StaticEntity;
import org.springframework.stereotype.Component;
import org.molgenis.data.AbstractSystemEntityFactory;
import org.molgenis.data.populate.EntityPopulator;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.meta.model.EntityType.AttributeRole.ROLE_ID;
import static org.molgenis.data.meta.model.Package.PACKAGE_SEPARATOR;
import static org.molgenis.test.PigeonEncounterMetadata.ID;
import static org.molgenis.test.InterestingDataPackage.PACKAGE_INTERESTING_DATA;

@Component
public class PigeonEncounterMetadata extends SystemEntityType
{
	private static final String SIMPLE_NAME = "PigeonEncounter";
	public static final String PIGEON_ENCOUNTER = PACKAGE_INTERESTING_DATA + PACKAGE_SEPARATOR + SIMPLE_NAME;

	public static final String ID = "id";

	private final InterestingDataPackage interestingDataPackage;

	public PigeonEncounterMetadata(InterestingDataPackage interestingDataPackage)
	{
		super(SIMPLE_NAME, PACKAGE_INTERESTING_DATA);
		this.interestingDataPackage = requireNonNull(interestingDataPackage);
	}

	@Override
	public void init()
	{
		setPackage(interestingDataPackage);

		setLabel("Pigeon Encounter");
		//TODO setDescription("");

		addAttribute(ID, ROLE_ID).setLabel("Identifier");
	}
}

public class PigeonEncounter extends StaticEntity
{
	public PigeonEncounter(Entity entity)
	{
		super(entity);
	}

	public PigeonEncounter(EntityType entityType)
	{
		super(entityType);
	}

	public PigeonEncounter(String id, EntityType entityType)
	{
		super(entityType);
		setId(id);
	}

	public void setId(String id)
	{
		set(ID, id);
	}

	public String getId()
	{
		return getString(ID);
	}
}

@Component
public class PigeonEncounterFactory extends AbstractSystemEntityFactory<PigeonEncounter, PigeonEncounterMetadata, String>
{
	PigeonEncounterFactory(PigeonEncounterMetadata pigeonEncounterMetadata, EntityPopulator entityPopulator)
	{
		super(PigeonEncounter.class, pigeonEncounterMetadata, entityPopulator);
	}
}
```

Sadly, it's not possible to generate multiple files from one template, so the Metadata and Factory classes have to be manually moved to separate files. (Tip: Set your cursor to the class name and use Refactor > Move... or press F6)


## Deploy / Run in Tomcat server
* Run, Edit configurations..., `+`, Tomcat, Local.
* Call it `molgenis-app [exploded]`
* (Add and) select your Tomcat installation
* VM options: `-Dmolgenis.home=<path to dir containing molgenis-server.properties> -Xmx4g -Des.discovery.zen.ping.multicast.enabled=false -Des.network.host=localhost`
* Deployment: Select `+` -> `artifact` -> `molgenis-app:war exploded`
* Application context: Select `/`
* Go back to the first tab, you should now have more options in the update and frame deactivation pulldowns.
* Select On Update: Redeploy, don't ask (This is the action that will triggered when you press the blue reload button in the run view)
* Select On frame deactivation: Update resources (This is the action that will be triggered whenever you tab out of IntelliJ)
(If you get update classes and resources to work, let us know!!)
* Select your favorite browser in the pulldown.
* Save the configuration
* In the tool bar, select the `molgenis-app [exploded]` configuration and press the play button.

This'll build and deploy molgenis to tomcat and open it in the browser.
Whenever you tab from molgenis to the browser, all modified resources will be copied to the deployed exploded war.
A browser reload should display the changes.

Note: In some cases InteliJ might not pick up all changes in the file system made during the
 build process. This may result in an error referencing a missing jar file. This can be fixed by
 selecting the 'Synchronize molgenis' option from the project action menu.

## Security
See also the [MOLGENIS Security settings](guide-security.md)

## Webpack watch run configuration
* Open molgenis-core-ui/target/classes/js.
* Right-click the dist folder, Mark directory as..., cancel exclusion.
* Open molgenis-core-ui/package.json
* Select the scripts / watch key in the json configuration
* Right-click it and select the NPM `create watch` option from the menu.
* SaveIn the tool bar, select the npm `watch` configuration and press the play button to start webpack watch.

Now, whenever you make changes to one or more JavaScript files, the corresponding js bundle file will get rebuilt by
the npm watch task. Once it's built, tab out of IntelliJ to trigger a refresh of the exploded war.
As soon as IntelliJ loses focus, you'll see a task progress bar in the bottom right corner of IntelliJ.
Wait for that task to finish and then refresh the browser. The changes will be loaded.

## Speed up building the project by skipping the javascript build

Building the project can be sped up by not building the javascript. To skip building the javascript, use:

`-Dskip.js=true`

to the maven build command.

* Note:
    - If javascript was not already built, javascript will be missing and the application will not function properly.
    - If changes were made to the javascript these changes will not be built into the application (as building will be skipped).
