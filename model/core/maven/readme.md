Nasdanika Maven module uses [Maven Resolver Supplier](https://javadoc.io/doc/org.apache.maven.resolver/maven-resolver-supplier/1.9.22/) to provide
capabilities for [Dependency](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/requirements/DependencyRequestRecord.html) and [ClassLoader](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/requirements/ClassLoaderRequirement.html) requirements.

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

