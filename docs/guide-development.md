# Developing MOLGENIS
To develop MOLGENIS on you machine we recommend to use IntelliJ as IDE. 
You need to install the prerequisites as well.

## Prerequisites for MOLGENIS development
The components needed to run MOLGENIS locally are:

![MOLGENIS components](images/install/molgenis_architecture.svg?raw=true)


**
You can download, install and use MOLGENIS for free under license [LGPLv3]().
**

* [OpenJDK 11](https://adoptopenjdk.net/)
* [Apache Tomcat v9.0.x](http://tomcat.apache.org/)
* [PostgreSQL v11.1](https://www.postgresql.org/download/)
* [Elasticsearch v5.5](https://www.elastic.co/downloads/elasticsearch)
* [Minio v6](https://minio.io/)
* Optional: [OpenCPU 2.1](https://www.opencpu.org/download.html) and [R 3.5.x](https://www.r-project.org/) (enables R scripting feature)
* Optional: [Python 3.6](https://www.python.org/downloads/) (enables Python scripting feature)

Deploy Apache Tomcat, and place the molgenis-app WAR as the ROOT.war in your apache-tomcat/webapps folder. If you are unfamiliar with Apache Tomcat, follow one of their [Apache Tomcat installation guides](https://tomcat.apache.org/tomcat-9.0-doc/deployer-howto.html).

Now that your Apache Tomcat is running and MOLGENIS is deployed, you will notice it will not work yet. This is because your database needs to be configured, and a single properties file needs to be set.

## Setting your molgenis-server.properties   
The properties file supplies information to the application regarding the database URL, and the initial administrator password. To make it clear to Tomcat where to find your properties file, you have to edit the setenv.sh file in the apache-tomcat folder.

```
echo 'CATALINA_OPTS="-Xmx2g -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -Dmolgenis.home=<put your molgenis home here>"' > <path to tomcat>/bin/setenv.sh
```

The **-Dmolgenis.home** property tells tomcat where to find your properties file. Replace the ${molgenis_home_folder} with the location of your molgenis home. Note that you should **NOT** use relative paths in your apache-tomcat configuration. Always use absolute paths to point to your molgenis-server.properties.

Inside the specified molgenis home folder, create a file called *molgenis-server.properties*, and depending on the version of your MOLGENIS, write the following:

```
db_user=molgenis
db_password=molgenis
db_uri=jdbc\:postgresql\://localhost/molgenis
admin.password=admin
user.password=admin
python_script_executable=<python3_executable_path>
MINIO_ACCESS_KEY=molgenis
MINIO_SECRET_KEY=molgenis
```

Remember the *molgenis* specified in your db_uri, because this will be the name of the database you will create later on in PostgreSQL. This effectively means that whatever you call your database, your db_uri should point to it.

**Setting up your PostgreSQL**  
If you are unfamiliar with PostgreSQL, follow one of their [PostgreSQL installation guides](https://www.postgresql.org/docs/11/static/index.html). Once you have a PostgreSQL server running, open up the included pgAdmin application that is supplied with most PostgreSQL installations, and perform the following actions:

- Add a database 'molgenis'
- Add a user 'molgenis' (password 'molgenis') under Login Roles
- Add 'can create databases' privilege to user 'molgenis'

For example, in psql terminal type:
```
CREATE DATABASE molgenis;
CREATE USER molgenis WITH PASSWORD 'molgenis';

-- optional for creation of databases by molgenis user
-- this is needed when you run integration tests 
ALTER USER molgenis CREATEDB;

GRANT ALL PRIVILEGES ON SCHEMA molgenis TO molgenis;
```

Set the credentials of the database in the *molgenis-server.properties*. See [Setting your molgenis-server.properties](#setting-your-molgenis-server-properties)

## Configuring Elasticsearch  
Open elasticsearch.yml in the Elasticsearch config directory and set the following properties:
```
cluster.name: molgenis
node.name: node-1
indices.query.bool.max_clause_count: 131072
```
Start Elasticsearch from the command line:
```
./bin/elasticsearch
```

By default MOLGENIS will try to connect to a node within cluster 'molgenis' on address 127.0.0.1:9300 (not to be confused with port 9200 which is used for REST communication). If the cluster is named differently or is running on another address then you will have to specify these properties in *molgenis-server.properties*:
```
elasticsearch.cluster.name=my-cluster-name
elasticsearch.transport.addresses=127.1.2.3:9300,127.3.4.5:9301
```

## Get the code
Create account on github.com. 

Copy the clone URL.

```bash
mkdir -p ~/git
cd ~/git 
git clone http://github.com/molgenis/molgenis
``` 

*Optionally select stable molgenis version:*

```bash
git fetch --tags origin
git checkout <tag name: see https://github.com/molgenis/molgenis/releases>
```

## Start MOLGENIS
We use [IntelliJ](guide-using-an-ide.md) as IDE to run the code in.