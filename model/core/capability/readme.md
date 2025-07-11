Nasdanika Capability framework allows to discover/load capabilities which meet a requirement.
Capabilities are provided by [CapabilityFactory](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/CapabilityFactory.html) ``create()`` method. 
Capability factories may request other capabilities they need.
As such, capabilities can be chained.
Factories create [CapabilityLoader](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/CapabilityLoader.html)s which provide [Flux](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html) reactive streams of capabilities.
It allows to have an infinite stream of capabilities which are consumed (and produced) as needed.
Capability providers may furnish additional information about capabilities.
This information can be used for filtering or sorting providers. 
Capability providers may also provide functionality such as:

* Implement ``Autocloseable`` and release resources associated with capabilities upon closing.
* Implement ``Lock`` or ``ReadWriteLock`` to guard access to provided capabilities.
* Extending on the above, a capability provider may implement Domain/Realm with a command stack - obtain, execute commands with locking, close.
 
A non-technical example of requirement/capability chain graph is a food chain/graph. 
Food is a requirement. Or "I want to eat" is a requirement.
Bread and, say fried eggs are two capabilities meeting/addressing the requirement. 
Bread requires "wheat", "water", and "bake" capabilities. 
Fried eggs require "egg", "oil", and "fry" capabilities.
"bake" capability is provided by an oven which may have a command stack or a lock because only one thing can be baked at a time.
Bread capability provider may implement ``Vegan`` marker interface which can be used for filtering.
All food capabilities may implement ``NutritionalInformation`` interface - it can be used for filtering or sorting.

A more technical example is Java [ServiceLoader](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/ServiceLoader.html)
with service type being a requirement and an instance of the service class being a capability.   

Nasdanika capability framework can operate on top of ``ServiceLoader`` and may be thought of as a generalization of service loading.
In essence, the capability framework is a [backward chaining](https://en.wikipedia.org/wiki/Backward_chaining) engine as shown in one of the examples below.

* [Sources](https://github.com/Nasdanika/core/tree/master/capability)
* [Javadoc](https://javadoc.io/doc/org.nasdanika.core/capability/)

----

[TOC levels=6]

## Client code - requesting a capability

Capabilities are loaded by [CapabilityLoader](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/CapabilityLoader.html). 
Capability loader can take an iterable of capability factories in its constructor, or it can load them using ``ServiceLoader``
as shown in the below code snippet:

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
capabilityLoader.getFactories().add(new TestServiceFactory<Object>());
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		
for (CapabilityProvider<?> cp: capabilityLoader.load(new TestCapabilityFactory.Requirement("Hello World"), progressMonitor)) {
	System.out.println(cp);
	Flux<?> publisher = cp.getPublisher();
			
	publisher.subscribe(System.out::println);
}
```

Factories can also be added post-construction with ``getFactories().add(factory)``.

### Service capabilities

Service requirements and capabilities provide functionality similar to ServiceLoader - requesting instances of specific type, but extend it with ability to provide additional service requirement.
This functionality is provided by [ServiceCapabilityFactory](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/ServiceCapabilityFactory.html)
and ``ServiceCapabilityFactory.Requirement``.

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
capabilityLoader.getFactories().add(new TestServiceFactory<Object>());
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		
@SuppressWarnings({ "unchecked", "rawtypes" })
ServiceCapabilityFactory.Requirement<List<Double>, Double> requirement = (ServiceCapabilityFactory.Requirement) ServiceCapabilityFactory.createRequirement(List.class, null,  33.0);
for (CapabilityProvider<?> cp: capabilityLoader.load(requirement, progressMonitor)) {
	System.out.println(cp);
	Flux<?> publisher = cp.getPublisher();
			
	publisher.subscribe(System.out::println);
}
```

It is also possible to load services from ``ServiceLoader`` using subclasses of [Service](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/ServiceFactory.html). 
You'd need to subclass ``ServiceFactory`` in a module which uses a particular service and override ``stream(Class<S> service)`` method as shown below:

```java
@Override
protected Stream<Provider<S>> stream(Class<S> service) {
	return ServiceLoader.load(service).stream();
}
```

Then you'd need to add the factory to the loader:

```java
capabilityLoader.getFactories().add(new TestServiceFactory<Object>());
```

## Providing a capability

As it was mentioned above, capability factories can be explicitly added to ``CapabilityLoader`` or loaded using ``ServiceLoader``.

Below is an example of a capability factory:

```java
public class TestCapabilityFactory implements CapabilityFactory<TestCapabilityFactory.Requirement, Integer> {
	
	public record Requirement(String value){};
	
	@Override
	public boolean canHandle(Object requirement) {
		return requirement instanceof Requirement;
	}

	@Override
	public CompletionStage<Iterable<CapabilityProvider<Integer>>> create(
			Requirement requirement,
			BiFunction<Object, ProgressMonitor, CompletionStage<Iterable<CapabilityProvider<Object>>>> resolver,
			ProgressMonitor progressMonitor) {
		
		return resolver.apply(MyService.class, progressMonitor).thenApply(cp -> {;
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Flux<MyService> myServiceCapabilityPublisher = (Flux) cp.iterator().next().getPublisher();
			
			return Collections.singleton(new CapabilityProvider<Integer>() {
	
				@Override
				public Flux<Integer> getPublisher() {
					Function<MyService, Integer> mapper = ms -> ms.count(((Requirement) requirement).value());
					return myServiceCapabilityPublisher.map(mapper);
				}
				
			});
		});
	}

}
```

There is a number of implementations of ``CapabilityFactory`` is different Nasdanika modules, most of them extending ``ServiceCapability``.
In Eclipse or other IDE open ``CapabilityFactory`` type hierarchy to discover available implementations.

## Loading Invocables from URIs

The capability framework allows to create/load implementations of [Invocable](https://javadoc.io/doc/org.nasdanika.core/common/latest/org.nasdanika.common/org/nasdanika/common/Invocable.html) from [URI](https://javadoc.io/doc/org.eclipse.emf/org.eclipse.emf.common/latest/org/eclipse/emf/common/util/URI.html)s:

* In conjunction with the [Maven](../maven/index.html) module implementations can be loaded from Maven repositories.
* Invocables can be implemented in [scripting languages](https://docs.oracle.com/en/java/javase/17/docs/api/java.scripting/javax/script/package-summary.html), e.g. [Groovy](https://groovy-lang.org/). Scripts may use dependencies loaded from Maven repositories. Script engine themselves can be loaded from Maven repositories.
* [Drawio](../drawio/index.html) diagrams can be made executable by adding invocable URI properties to diagram elements. They can then be wrapped into a [dynamic proxy](https://docs.oracle.com/javase/8/docs/technotes/guides/reflection/proxy.html) and invocable URI's.   

### Examples

#### String value

##### URL encoded

``data:value/String,Hello+World`` [data URL](https://developer.mozilla.org/en-US/docs/Web/URI/Schemes/data) is converted to an Invocable which takes zero arguments and returns URL-decoded data part of the URL (after comma).

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
URI requirement = URI.createURI("data:value/String,Hello+World");
Invocable invocable = capabilityLoader.loadOne(
		ServiceCapabilityFactory.createRequirement(Invocable.class, null, requirement),
		progressMonitor);
Object result = invocable.invoke();
System.out.println(result);
```

##### URL and Base64 encoded

``data:value/String;base64,SGVsbG8=`` is converted to an Invocable which takes zero arguments and returns URL-decoded and then [Base64](https://en.wikipedia.org/wiki/Base64) decoded data part converted to String.

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
URI requirement = URI.createURI("data:value/String;base64,SGVsbG8=");
Invocable invocable = capabilityLoader.loadOne(
		ServiceCapabilityFactory.createRequirement(Invocable.class, null, requirement),
		progressMonitor);
Object result = invocable.invoke();
System.out.println(result);
```

#### Java

##### Constructor

``data:java/org.nasdanika.capability.tests.MyTestClass;base64,SGVsbG8=`` is converted to an Invocable which invokes [MyTestClass](https://github.com/Nasdanika/core/blob/master/capability-tests/src/main/java/org/nasdanika/capability/tests/MyTestClass.java) constructor.

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
URI requirement = URI.createURI("data:java/org.nasdanika.capability.tests.MyTestClass;base64,SGVsbG8=");
Invocable invocable = capabilityLoader.loadOne(
		ServiceCapabilityFactory.createRequirement(Invocable.class, null, requirement),
		progressMonitor);
Object result = invocable.invoke();
System.out.println(result);
```

In the above code snippet invocable is invoked with no arguments, which matches the below constructor passing the decoded data part of the URL in ``binding`` argument:

```java
public MyTestClass(
		CapabilityFactory.Loader loader, 
		ProgressMonitor progressMonitor, 
		byte[] binding,
		String fragment) {
	...
}	
```

The below snippet passes ``33`` argument to ``invoke()``:

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
URI requirement = URI.createURI("data:java/org.nasdanika.capability.tests.MyTestClass;base64,SGVsbG8=");
Invocable invocable = capabilityLoader.loadOne(
		ServiceCapabilityFactory.createRequirement(Invocable.class, null, requirement),
		progressMonitor);
Object result = invocable.invoke(33);
System.out.println(result);
```

Which matches the below constructor:

```java
public MyTestClass(
		CapabilityFactory.Loader loader, 
		ProgressMonitor progressMonitor, 
		byte[] binding, 
		int arg) {
	...
}
```

``33`` is passed via the ``arg`` argument.

##### Static method

Static methods can be addresed by adding ``::`` and method name after the class name as in this URL: ``data:java/org.nasdanika.capability.tests.MyTestClass::factory;base64,SGVsbG8=``. The resulting Invocable will select the best matching ``factory`` method.

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
URI requirement = URI.createURI("data:java/org.nasdanika.capability.tests.MyTestClass::factory;base64,SGVsbG8=");
Invocable invocable = capabilityLoader.loadOne(
		ServiceCapabilityFactory.createRequirement(Invocable.class, null, requirement),
		progressMonitor);
Object result = invocable.invoke();
System.out.println(result);
```

In the above code snippet ``invoke()`` has no arguments and therefore the below method matches:

```java
public static MyTestClass factory(
		CapabilityFactory.Loader loader, 
		ProgressMonitor progressMonitor, 
		byte[] binding) {
	...
}
```

As with constructors, the decoded data part is passed to the method as ``binding`` argument.

In the below snippet ``invoke()`` takes ``55`` argument:

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
URI requirement = URI.createURI("data:java/org.nasdanika.capability.tests.MyTestClass::factory;base64,SGVsbG8=");
Invocable invocable = capabilityLoader.loadOne(
		ServiceCapabilityFactory.createRequirement(Invocable.class, null, requirement),
		progressMonitor);
Object result = invocable.invoke(55);
System.out.println(result);
```

Which matches the below method:

```java 
public static MyTestClass factory(
		CapabilityFactory.Loader loader, 
		ProgressMonitor progressMonitor, 
		byte[] binding,
		int arg) {
	...
}
```

##### @Parameter annotation

[Parameter](https://javadoc.io/doc/org.nasdanika.core/common/latest/org.nasdanika.common/org/nasdanika/common/Invocable.Parameter.html) annotation can be used on method and constructor parameters to provide parameter name and optionally narrow parameter type.

#### Script

The below snippet exectutes ``test.groovy`` script in the current directory. [ScriptEngineManger](https://docs.oracle.com/en/java/javase/17/docs/api/java.scripting/javax/script/ScriptEngineManager.html) is used to get a [ScriptEngine](https://docs.oracle.com/en/java/javase/17/docs/api/java.scripting/javax/script/ScriptEngine.html) by extension. 
Therefore, the engine factory shall be registered with the script engine manager.

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
URI requirement = URI.createFileURI(new File("test.groovy").getCanonicalPath());
Invocable invocable = capabilityLoader.loadOne(
		ServiceCapabilityFactory.createRequirement(Invocable.class, null, requirement),
		progressMonitor);
Object result = invocable.invoke();
System.out.println(result);
```

This is the test script: 

```groovy
import org.nasdanika.capability.CapabilityFactory.Loader
import org.nasdanika.common.ProgressMonitor

// Script arguments for reference
Loader loader = args[0];
ProgressMonitor loaderProgressMonitor = args[1];
Object data = args[2];

System.out.println(args);
"I've got " + args.length + " arguments!"
```

Similar to Java constructors and static methods, it takes the following arguments:

* [CapabilityFactory.Loader](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/CapabilityFactory.Loader.html) to request additional capabilities if needed
* [ProgressMonitor](https://javadoc.io/doc/org.nasdanika.core/common/latest/org.nasdanika.common/org/nasdanika/common/ProgressMonitor.html) to report progress and pass to the loader methods
* URI's fragment value or null
* Invocable arguments

In the below code the script receives ``Hello`` as its third argument (binding) and ``Universe`` as its fourth argument:

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
URI requirement = URI.createFileURI(new File("test.groovy").getCanonicalPath()).appendFragment("Hello");
Invocable invocable = capabilityLoader.loadOne(
		ServiceCapabilityFactory.createRequirement(Invocable.class, null, requirement),
		progressMonitor);
Object result = invocable.invoke("Universe");
System.out.println(result);
```

#### Spec

Spec URI's allow to specify Maven dependencies to construct a [ClassLoader](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ClassLoader.html) for loading Java classes including script engine factories.
It is also to specify a module path to construct a [module layer](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ModuleLayer.html).

##### JSON

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
URI specUri = URI.createFileURI(new File("test-specs/java.json").getCanonicalPath()).appendFragment("Hello+World");
Invocable invocable = capabilityLoader.loadOne(
		ServiceCapabilityFactory.createRequirement(Invocable.class, null, new URIInvocableRequirement(specUri)),
		progressMonitor);
Object result = invocable.invoke();
System.out.println(result);
```

The above snippet uses the below spec to create an instance of ``MyTestClass``:

```json
{
	"type": "org.nasdanika.capability.tests.MyTestClass",
	"bind": [
		"data:value/String,Some+other+value"
	]
}	
```

Similar to data URL's a matching constructor is found for the following arguments:

* CapabilityFactory.Loader
* ProgressMonitor
* String - URL decoded URI fragment, may be null
* Bindings loaded from the ``bind`` array of URI's

Below is the matching constructor:

```java	
public MyTestClass(
		CapabilityFactory.Loader loader, 
		ProgressMonitor progressMonitor, 
		String fragment,
		String bind) {
	...;
}	
```

##### YAML

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor(true);
URI specUri = URI.createFileURI(new File("test-specs/groovy.yml").getCanonicalPath()).appendFragment("Hello+World");
Invocable invocable = capabilityLoader.loadOne(
		ServiceCapabilityFactory.createRequirement(Invocable.class, null, new URIInvocableRequirement(specUri)),
		progressMonitor);
Object result = invocable.invoke();
System.out.println(result);
```

The above snippet executes Groovy script specified inline in the below YAML:

```yaml
script:
  engineFactory: org.codehaus.groovy.jsr223.GroovyScriptEngineFactory
  source: |
    "Hello, world! " + myBinding + " " + args[2]
  bindings:
    myBinding: data:value/String,Some+value
dependencies: org.apache.groovy:groovy-all:pom:4.0.23
localRepository: target/groovy-test-repo
```

In this case ``org.apache.groovy:groovy-all:pom:4.0.23`` Maven coordinates are used to load Groovy with all dependencies and construct a ClassLoader.
Because the engine was loaded at runtime, it is not known to the ScriptEngineManager and has to be explicitly specified.

The script gets an ``args`` array with loader, progress monitor, decoded fragment and arguments passed to ``invoke()``. 
It also gets named bindings loaded from the ``bindings`` map entries. 

#### Drawio diagram

Below is a YAML spec with an embedded diagram:

```yaml
diagram:
  source: |
    <mxfile ...abridged... </mxfile>
  processor: processor
  bind: bind
  interfaces: java.util.function.Function
```  

And this is a YAML specification which references a diagram:

```yaml
diagram:
  location: diagram.drawio
  processor: processor
  bind: bind
  interfaces: java.util.function.Function
```

The below code loads the spec, passes the fragment to the diagram as properties in addition to the properties from the spec, creates a dynamic proxy which invokes diagram element processors, and uses the proxy to execute the diagram logic:

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
URI specUri = URI.createFileURI(new File("diagram-function.yml").getCanonicalPath()).appendFragment("my-property=Hello");
Invocable invocable = capabilityLoader.loadOne(
		ServiceCapabilityFactory.createRequirement(Invocable.class, null, new URIInvocableRequirement(specUri)),
		progressMonitor);
Function<String,Object> result = invocable.invoke();
System.out.println(result);
System.out.println(result.apply("YAML"));
```

See [Capability tests](https://github.com/Nasdanika/core/tree/master/capability-tests/src/test/java/org/nasdanika/capability/tests/tests) and [Executable Diagrams Dynamic Proxy](https://github.com/Nasdanika-Demos/executable-diagram-dynamic-proxy/tree/main) demo for more examples.

### Specification

#### URI

This section explains supported URI formats. 
Please note that using a custom [URIHandler](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/requirements/URIHandler.html)
you may:

* Normalize "logical" URI's to supported "physical" URI's. 
* Implement ``openStream()`` 

For example normalized ``my-building-blocks://gen-ai/chat-completions`` to a specification of a chat completions component, say, ``gitlab://my-shared-components/open-ai/chat-completions.yml`` and then implement ``openStream()`` which supports ``gitlab`` scheme[^gitlab_urihandler]

[^gilab_urihandler]: You can use [GitLabURIHandler](https://github.com/Nasdanika-Models/gitlab/blob/main/model/src/main/java/org/nasdanika/models/gitlab/util/GitLabURIHandler.java) as a starting point.

##### Data

Data URI has the following format: ``data:[<mediatype>][;base64],<data>[#fragment]``. 
The following sections describe supported media types.

###### value/<class name>

an instance of the class is constructed from the data part bytes, fragment is ignored. if the class name does not contain dots then ``java.lang.`` prefix is added to the class name. Examples: 
    * ``data:value/String,Hello+World``
    * ``data:value/String;base64,SGVsbG8=``

###### java/<class name or method reference>

Class constructors or static methods are wrapped into an Invocable and the matching constructor/method is invoked from the resulting ``Invocable.invoke()``. 
Constructors/methods shall have the following signature:

* [CapabilityFactory.Loader](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/CapabilityFactory.Loader.html) to request additional capabilities if needed
* [ProgressMonitor](https://javadoc.io/doc/org.nasdanika.core/common/latest/org.nasdanika.common/org/nasdanika/common/ProgressMonitor.html) to report progress and pass to the loader methods
* byte[] - the data part
* String - fragment
* Optional additional parameters for arguments passed to the result Invocable

Examples:

* ``data:java/org.nasdanika.capability.tests.MyTestClass;base64,SGVsbG8=#World``
* ``data:java/org.nasdanika.capability.tests.MyTestClass::factory;base64,SGVsbG8=``

###### spel/<URL-encoded SpEL expression>

Evaluates a [Spring Expression Langauge](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions) (SpEL) expression.
Binding by name sets variables. Arguments array is passed to the expression as the root object. It the array is of size 1 then its single element is used as the root object.

Example: ``data:spel/%23myVar+%2B+%23this``

This [online URL Decoder/Encoder](https://meyerweb.com/eric/tools/dencoder/) can be used to encode expressions.

###### application/<format>/invocable

With format being either ``yaml`` or ``json``. 
YAML or JSON specification (see below) encoded into the data part.

Example: ``data:application/yaml/invocable;base64,c2Nya...abridged...1yZXBv``

##### Hierarchical

If the hierarchical URI's last segment ends with ``.yml`` or ``.yaml`` (case insensitive) it is treated as a YAML specification (see below). 
If the last segment ends with ``.json`` (also case insensitive) it is treated as a JSON specification. 
Otherwise a script engine is looked up by extension (the part of the last segments after the last dot). E.g. ``groovy``.

Scripts receive ``args`` binding (variable) of type ``Object[]`` with the following elements:

* [CapabilityFactory.Loader](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/CapabilityFactory.Loader.html) to request additional capabilities if needed
* [ProgressMonitor](https://javadoc.io/doc/org.nasdanika.core/common/latest/org.nasdanika.common/org/nasdanika/common/ProgressMonitor.html) to report progress and pass to the loader methods
* String - fragment
* Arguments passed to the result Invocable

#### YAML/JSON specification

YAML/JSON specification is pre-processed and then loaded into [InvocableRequirement](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/requirements/InvocableRequirement.html).

The specification supports the following configuration entries:

* ``diagram`` - Map, loaded into [DiagramRecord](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/requirements/DiagramRecord.html):
    * ``location`` - String, URI of the diagram location relative to the specification location.
    * ``source`` - String, diagram source. Either ``location`` or ``source`` shall be used.
    * ``base`` - String, base URI if ``source`` is used
    * ``properties`` - Map, properties to pass to the diagram. Nested properties can be addressed using "." (dot) separator. For arrays index is used as key. E.g. people.3.name. If URI fragment is present it is parsed into name/value pairs in the same way as query strings are parsed. Fragment properties overwrite spec properties.
    * ``processor`` - optional String, property to load processor specifications from. One diagram element may have multiple processor specifications in different properties. Also, property expansion can be used to customize processor specification. E.g. ``%env%/storage.yaml`` would point to different specifications depending on the value of ``%env%`` property. 
    * ``bind`` - optional String, property for a dynamic proxy method name or signature
    * ``interfaces`` - optional String or List, dynamic proxy interfaces
* ``type`` - String, class name or method reference is ends with ``::<static method name>``
* ``script`` - Map, loaded into [ScriptRecord](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/requirements/ScriptRecord.html):
    * ``location`` - String, URI of script sources
    * ``source`` - String, script source. Either ``location`` or ``source`` shall be used
    * ``language`` - String, script language (mime type) for source. For location if language is not specified it is derived from extension.
    * ``engineFactory`` - String, fully qualified name of a script engine factory implementation. Use if the engine is loaded from dependencies and therefore is not visible to the script engine manager. If ``engineFactory`` is specified ``language`` and location extension are ignored.
    * ``bindings`` - Map, values are treated as Invocable URIs providing binding values. Loader, progress monitor, fragment and invocable arguments are available to the script via the ``args`` binding of type ``Object[]``. 
* ``bind`` - String or List, Invocable URIs to bind to the ``type`` Invocable. Not supported by ``diagram`` and ``script``.   
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

Maven dependency resolution uses default values as explained in the [Maven module documentation](../maven/index.html).
``diagram``, ``type`` and ``script`` are mutually exclusive.     

Note: ``extends`` key is reserved for future releases to support spec inheritance.

Configuration is pre-pThe by interpolating system properties and environment variables. 
E.g. ``${my-property}`` will be expanded to the value of ``my-property`` system property if it is set. 
Respectively, ``${env.MY_ENV_VAR}`` will be expanded to the value of ``MY_ENV_VAR`` environment variable if it is set.
Property expansion can be escaped with additional ``{}`` e.g. ``${{my-property}}`` will be expanded to ``${my-property}`` regardless of whether ``my-properety`` system property is set or not.

## EMF

Many of Nasdanika capabilities are based on [Eclipse Modeling Framework](https://eclipse.dev/modeling/emf/) (EMF)[^vogella_emf], Ecore[^ecore] models in particular.
One of key objects in EMF Ecore is a [ResourceSet](https://javadoc.io/static/org.eclipse.emf/org.eclipse.emf.ecore/2.33.0/org/eclipse/emf/ecore/resource/ResourceSet.html).
Resource set has a package registry, resource factory registry, and URI converter.
``org.nasdanika.capability.emf`` packages provides capability factories for contributing to resource set. 
It allows to request resource set from a capability loader and the returned resource set would be configured with registered EPackages, resource factories, adapter factories and URIHandlers.  

[^vogella_emf]: See [Eclipse Modeling Framework (EMF) - Tutorial](https://www.vogella.com/tutorials/EclipseEMF/article.html) and [EMF Eclipse Modeling Framework](https://www.amazon.com/EMF-Eclipse-Modeling-Framework-2nd/dp/0321331885) book for more details.
[^ecore]: See EMF Ecore chapter in [Beyond Diagrams](https://leanpub.com/beyond-diagrams) book for a high-level overview of EMF Ecore.

### Requesting a ResourceSet

#### With all packages and factories

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
Requirement<ResourceSetRequirement, ResourceSet> requirement = ServiceCapabilityFactory.createRequirement(ResourceSet.class);		
for (CapabilityProvider<?> capabilityProvider: capabilityLoader.load(requirement, progressMonitor)) {
	ResourceSet resourceSet = (ResourceSet) capabilityProvider.getPublisher().blockFirst();
}
```

#### Selecting contributors

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();

Predicate<ResourceSetContributor> contributorPredicate = ...;
ResourceSetRequirement serviceRequirement = new ResourceSetRequirement(null, contributorPredicate);
		
Requirement<ResourceSetRequirement, ResourceSet> requirement = ServiceCapabilityFactory.createRequirement(ResourceSet.class, null, serviceRequirement);		

for (CapabilityProvider<?> capabilityProvider: capabilityLoader.load(requirement, progressMonitor)) {
	ResourceSet resourceSet = (ResourceSet) capabilityProvider.getPublisher().blockFirst();
}
```

#### Providing ResourceSet instance

You may provide an instance of ResourceSet to configure in the requirement. 

### Contributing
#### EPackages

Create a class extending ``EPackageCapabilityFactory``:

```java
public class NcoreEPackageResourceSetCapabilityFactory extends EPackageCapabilityFactory {

	@Override
	protected EPackage getEPackage() {
		return NcorePackage.eINSTANCE;
	}

	@Override
	protected URI getDocumentationURI() {
		return URI.createURI("https://ncore.models.nasdanika.org/");
	}

}
```

and add it to ``module-info.java`` provides:

```java
provides CapabilityFactory with NcoreEPackageResourceSetCapabilityFactory;
```

#### Resource factories

Create a class extending ``ResourceFactoryCapabilityFactory``:

```java
public class XMIResourceFactoryCapabilityFactory extends ResourceFactoryCapabilityFactory {

	@Override
	protected Factory getResourceFactory() {
		return new XMIResourceFactoryImpl();
	}
	
	@Override
	protected String getExtension() {
		return Resource.Factory.Registry.DEFAULT_EXTENSION;
	}

}
```

and add it to ``module-info.java`` provides CapabilityFactory.

#### URI handlers

Create a class extending ``URIConverterContributorCapabilityFactory``:

```java
public class ClassPathURIHandlerResourceSetCapabilityFactory extends URIConverterContributorCapabilityFactory {

	@Override
	protected void contribute(URIConverter uriConverter, ProgressMonitor progressMonitor) {	
		uriConverter.getURIHandlers().add(0, new URIHandlerImpl() {

			@Override
			public boolean canHandle(URI uri) {
				return uri != null && Util.CLASSPATH_SCHEME.equals(uri.scheme());
			}

			@Override
			public InputStream createInputStream(URI uri, Map<?, ?> options) throws IOException {
				return DefaultConverter.INSTANCE.toInputStream(uri);
			}
			
		});
		
	}
	
}
```

and add it to ``module-info.java`` provides CapabilityFactory.

## Loading configuration

``org.nasdanika.capability.ConfigurationRequirement`` can be used to load configuration resources/files. 
Its canonical constructor takes:

* Module name
* Configuration name
* Configuration type

All the above elements can be null. 
There is also a number of "shortcut" constructors.

The base configuration name is ``config`` it can be changed by setting ``org.nasdanika.config.base`` system property.
If the module name is not null or blank then it is appended to the base configuration name separated with a slash (``/``).
If the configuration name is not null or blank then it is also appended to the base configuration name separated with a slash.
After that ``.yml``, ``.yaml``, ``.json`` extensions are appended in order to the configuration name is used to create a URL 
which is then resolved relative to the current directory. 
Then an attempt is made to load YAML or JSON (depending on the extension) from the resulting URL.

If there is a configuration resource and configuration type is null, then ``Function<String,Object>`` is returned.
If there is no configuration resource, then an "empty" function is returned - it always returns ``null`.

If configuration type is not null, then it is instantiated by wrapping the configuration class into [Invocable]((https://javadoc.io/doc/org.nasdanika.core/common/latest/org.nasdanika.common/org/nasdanika/common/Invocable.html) with ``Invocable.of(Class)``
method and then invoking its ``call(Map)`` method with loaded YAML or JSON converted to map or with an empty map if there are no configuration resources[^loading_java_records].

### Examples

#### Untyped default configuration for the caller module

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
    
ConfigurationRequirement req = new ConfigurationRequirement();
Function<String,Object> config = capabilityLoader.loadOne(req, progressMonitor);
```

For ``org.myorg.mymodule`` module configuration would be loaded from the first available resource in the below list:

* ``config/org/myorg/mymodule.yml``
* ``config/org/myorg/mymodule.yaml``
* ``config/org/myorg/mymodule.json``


#### Untyped default configuration for a specific module

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
    
ConfigurationRequirement req = new ConfigurationRequirement(getClass().getModule());
Function<String,Object> config = capabilityLoader.loadOne(req, progressMonitor);
```

#### Global typed configuration

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
    
ConfigurationRequirement req = new ConfigurationRequirement((String) null, "global", ConfigRecord.class);
ConfigRecord config = capabilityLoader.loadOne(req, progressMonitor);
```

In the above snippet configuration is loaded from ``config/global.<extension>`` resource where extension is ``yml``, ``yaml`` or ``json``.
If the second requirement constructor parameter was null, then resource name would be ``config.<extension>``.

#### Named typed requirement

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
    
ConfigurationRequirement req = new ConfigurationRequirement(ConfigRecord.class);
ConfigRecord config = capabilityLoader.loadOne(req, progressMonitor);
```

#### Named untyped requirement

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
    
ConfigurationRequirement req = new ConfigurationRequirement("my-app");
Function<String,Object> config = capabilityLoader.loadOne(req, progressMonitor);
```

[^loading_java_records]: See [Loading Java records from Map, YAML, JSON, …](https://medium.com/nasdanika/loading-java-records-from-map-yaml-json-399662d8e90f) Medium story for additional 
information about loading records from YAML and JSON.

## Applications

### Services

Service capabilities explained above a used by [Graph](https://graph.models.nasdanika.org/index.html) and [Function Flow](https://function-flow.models.nasdanika.org/index.html) for loading [node processors](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/function/BiFunctionProcessorFactory.NodeProcessor.html) and [connection processors](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/function/BiFunctionProcessorFactory.ConnectionProcessor.html) for a specific requirement using [NodeProcessorFactory](https://javadoc.io/doc/org.nasdanika.core/graph-model/latest/org.nasdanika.graph.model/org/nasdanika/graph/model/util/NodeProcessorFactory.html) and [ConnectionProcessorFactory](https://javadoc.io/doc/org.nasdanika.core/graph-model/latest/org.nasdanika.graph.model/org/nasdanika/graph/model/util/ConnectionProcessorFactory.html) respectively.
For example, code generation, execution, simulation.

### Solutions for architectures

One of future application of the capability framework is creation a list of solution alternatives for an architecture/pattern.
For example, there might be multiple [RAG](https://rag.nasdanika.org/) embodiments with different key types, key extractors, stores, ...
Some of "design dimensions" are listed below:

* Key type:
    * Bag of words. Multiple options - just words, words with frequency, tokenized words, word stems.
    * Embedding vector - different embedding models, different dimensions.
* Store - multiple stores for multiple key types. Multiple indexing and retrieval methods. Chunk size, chunk overlap, chunking algorithm.     
* Generator - multiple models and prompts

As you can see a number of potential combinations can easily go into thousands or even be infinite. 
Reactive approach with filtering and sorting may be helpful in selecting a solution which is a good fit for a particular use case - number and type of data sources etc. 
For example, if the total size of data is under a few gigabytes an in-memory store may be a better choice than, say, an external (vector) database.
Also an old good bag of words might be better than embeddings. E.g. it might be cheaper. 

Solution alternatives may include temporal aspect or monetary aspects.
For example, version X of Y is available at time Z. 
Z might be absolute or relative. Say, Z days after project kick-off or license fee payment.
Identified solutions meeting requirements can have different quality attributes - costs (to build, to run), timeline, etc.
These quality attributes can be used for solution analysis.
E.g. one solution can be selected as a transition architecture and another as the target architecture.   

### Backward chaining

[Family reasoning](https://github.com/Nasdanika-Models/family/tree/main/demos/reasoning) demonstrates application of the capability framework as a backward chaining engine.
Family relationships such as ``grandfather`` and ``cousin`` are constructed by requiring and combining relationships such as ``child`` and ``sibling``. 

### Stream processing

This possible application is similar to backward reasoning. 
Imagine an algorithmic trading strategy which uses several technical indicators, such as moving averages, to make trading decisions.
Such a strategy would submit requirements for technical indicators which would include symbol, indicator configuration, time frame size.
Technical indicators in turn would submit a requirement for raw trading data. 
A technical indicator such as moving average would start publishing its events once it receives enough trading data frames to compute its average.

A trading engine would submit a requirement for strategies. 
A strategy factory may produce multiple strategies with different configurations.
The trading engine would perform "paper" trades, select well-performing strategies and discard ones which perform poorly.
This can be an ongoing process - if a strategy deteriorates then it is discarded and a new strategy is requested from strategy publishers - this process can be infinite.  

### AI model training/fine-tuning

This application is similar to stream processing and may be combined with backward reasoning.
Let's say we want to train a model to answer questions about [family relationships](https://family.models.nasdanika.org/demos/mapping/) for a specific family. 
For example, "Who is [Alan's](https://family.models.nasdanika.org/demos/mapping/references/members/alain/index.html) great grandmother?"
A single relationship in the model can be expressed in multiple ways in natural language. 
And multiple relationships can be expressed in a single sentence.
For example:

* Elias is a person
* Elias is a man
* Elias is a male
* Elias is a parent of Fiona
* Fiona is a child of Elias
* Elias is a father of Fiona
* Fiona is a daughter of Elias
* Paul and Isa are parents of Lea and Elias
* ...

So, on top of a model there might be a collection of text generators. Output of those generators can be fed to a model:

* Supervised - question and answer
    * "How many sisters does Bryan have?" - "Two"
    * "Who are Bryan's sisters?" - "Clara and Fiona"
* Unsupervised - factual statements

A similar approach can be applied to other models - customer/accounts, organization or [architecture](https://architecture.models.nasdanika.org/) model, etc.

For example, from the [Internet Banking System](https://architecture.models.nasdanika.org/demo/internet-banking-system/index.html) we can generate something like "[Accounts Summary Controller](https://architecture.models.nasdanika.org/demo/internet-banking-system/r0/internet-banking-system/rh/api-application/rh/accounts-summary-controller/index.html) uses [Mainframe Banking System Facade](https://architecture.models.nasdanika.org/demo/internet-banking-system/r0/internet-banking-system/rh/api-application/rh/mainframe-banking-system-facade/index.html) to make API calls to the [Mainframe Banking System](https://architecture.models.nasdanika.org/demo/internet-banking-system/r0/mainframe-banking-system/index.html) over XML/HTTPS".
"make API calls" may also be generated as "connect" or "make requests".

In a similar fashion a number of questions/answers can be generated.

