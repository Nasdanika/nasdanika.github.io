Nasdanika Maven module uses [Maven Resolver Supplier](https://javadoc.io/doc/org.apache.maven.resolver/maven-resolver-supplier/1.9.22/) to provide
[capabilities](../capability/index.html) for [Dependency](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/requirements/DependencyRequestRecord.html) and [ClassLoader](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/requirements/ClassLoaderRequirement.html) requirements.

* [Sources](https://github.com/Nasdanika/core/tree/master/maven)
* [Javadoc](https://javadoc.io/doc/org.nasdanika.core/maven/latest/org.nasdanika.maven/module-summary.html)

## Dependency 

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
DependencyRequestRecord requirement = new DependencyRequestRecord(
		new String[] { "org.apache.groovy:groovy-all:pom:4.0.23" }, 
		null, 
		null, 
		"target/test-repo");
		
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
Collection<File> result = capabilityLoader.loadOne(requirement, progressMonitor);
```

The above code snippet loads [org.apache.groovy:groovy-all:pom:4.0.23](https://mvnrepository.com/artifact/org.apache.groovy/groovy-all/4.0.23) and its dependencies into ``target/test-repo`` directory and returns a list of jar files.

## ClassLoader 

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ClassLoaderRequirement requirement = new ClassLoaderRequirement(
		null, // String[] modulePath,
		null, // String[] rootModules,
		new ModuleLayer[] { getClass().getModule().getLayer() }, 
		getClass().getClassLoader(), // ClassLoader parentClassLoader,
		true, // boolean singleLayerClassLoader,				
		new String[] { "org.apache.groovy:groovy-all:pom:4.0.23" }, 
		null, 
		null, 
		"target/test-repo",
		System.out::println);
		
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
ClassLoader result = capabilityLoader.loadOne(
		ServiceCapabilityFactory.createRequirement(ClassLoader.class, null, requirement),
		progressMonitor);
		
Class<?> scriptEngineFactoryClass = result.loadClass("org.codehaus.groovy.jsr223.GroovyScriptEngineFactory");
```

The above code snippet:

* Loads [org.apache.groovy:groovy-all:pom:4.0.23](https://mvnrepository.com/artifact/org.apache.groovy/groovy-all/4.0.23) and its dependencies into ``target/test-repo`` directory
* Creates a ClassLoader
* Loads [org.codehaus.groovy.jsr223.GroovyScriptEngineFactory](https://javadoc.io/doc/org.apache.groovy/groovy-jsr223/4.0.23/org/codehaus/groovy/jsr223/GroovyScriptEngineFactory.html) class


## Default configuration

[DependencyCapabilityFactory](https://github.com/Nasdanika/core/blob/master/maven/src/main/java/org/nasdanika/maven/DependencyCapabilityFactory.java), which is responsible for resolving and downloading dependencies, loads default configuration in the following way:

* If ``org.nasdanika.maven.DependencyCapabilityFactory.config.yml`` system property is set, then it is treated as a URL of a configuration YAML resource. The URL is resolved relative to the current directory. 
* If ``org.nasdanika.maven.DependencyCapabilityFactory.config.json`` system property is set, then it is treated as a URL of a configuration JSON resource. The URL is resolved relative to the current directory. 
* If ``NSD_DEPENDENCY_RESOLVER_CONFIG_YAML_URL`` environment variable is set, then it is treated as an absolute URL of a configuration YAML resource. 
* If ``NSD_DEPENDENCY_RESOLVER_CONFIG_JSON_URL`` environment variable is set, then it is treated as an absolute URL of a configuration JSON resource. 
* If ``NSD_DEPENDENCY_RESOLVER_CONFIG_YAML`` environment variable is set, then it is treated as YAML configuration. 
* If ``NSD_DEPENDENCY_RESOLVER_CONFIG_JSON`` environment variable is set, then it is treated as JSON configuration. 
* If ``dependency-reolver-config.yml`` file exists in the current directory the it is loaded as YAML.
* If ``dependency-reolver-config.json`` file exists in the current directory the it is loaded as JSON.

The loaded configuration is interpolated with system properties and environment variables. 
E.g. ``${my-property}`` will be expanded to the value of ``my-property`` system property if it is set. 
Respectively, ``${env.MY_ENV_VAR}`` will be expanded to the value of ``MY_ENV_VAR`` environment variable if it is set.
Property expansion can be escaped with additional ``{}`` e.g. ``${{my-property}}`` will be expanded to ``${my-property}`` regardless of whether ``my-properety`` system property is set or not.

## Configuration specification

* ``modulePath`` - optional String or List, module path. If null, derived from root modules if they are present
* ``rootModules`` - optional String or List, root modules. The first root module is used to obtain the class loader
* ``oneLayerClassLoader`` - optional boolean indicating whether a single class loader shall be used for all modules in in the layer
* ``dependencies`` - optional String or List of dependencies in ``<groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}`` format. E.g. ``org.apache.groovy:groovy-all:pom:4.0.23``
* ``managedDependencies`` - optional String or List of dependencies in ``<groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}`` format
* ``remoteRepositories`` - Map (single remote repository) or List of Maps of remote repository definitions loaded into [RemoteRepoRecord](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/requirements/RemoteRepoRecord.html):
    * ``id`` - String, repo ID
    * ``type`` - String, optional repo type
    * ``url`` - String, repository URL
    * ``proxy`` - optional Map:
        * ``type`` - String, ``http`` or ``https``
        * ``host`` - String
        * ``port`` - integer
        * ``auth`` - authentication (see below)   
    * ``auth`` - Map:
        * ``username`` - String
        * ``password`` - String    
    * ``mirroredRepositories`` - Map or List, mirrored repositories
* ``localRepository`` - optional String, path to the local repository to download dependencies to. Defaults to ``repository``.

## URI Handler

Maven URIHandler handles URIs of the following format: ``maven://<groupId>/<artifactId>/<extension>/<version>/<resource path>[?classifier=<classifier>]``.

For example, for ``maven://org.nasdanika.models.architecture/model/jar/2024.8.0/model/architecture.ecore?classifier=model`` URI ``model/architecture.ecore`` resource would be loaded from ``model-2024.8.0-model.jar`` in 
``org.nasdanika.models.architecture`` group ``model`` artifact version ``2024.8.0`` as shown in the snippet below:

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
Requirement<ResourceSetRequirement, ResourceSet> requirement = ServiceCapabilityFactory.createRequirement(ResourceSet.class);		
ResourceSet resourceSet = capabilityLoader.loadOne(requirement, progressMonitor);
URI modeURI = URI.createURI("maven://org.nasdanika.models.architecture/model/jar/2024.8.0/model/architecture.ecore?classifier=model");
Resource resource = resourceSet.getResource(modeURI, true);
System.out.println(resource.getContents());
```
