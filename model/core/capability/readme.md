Nasdanika Capability framework[^javadoc] allows to discover/load capabilities which meet a requirement.
Capabilities are provided by [CapabilityFactory](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/CapabilityFactory.html) ``create()`` method. 
Capability factories may request other capabilities they need.
As such, capabilities can be chained.
Factories create [CapabilityLoader](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/CapabilityLoader.html)s which provide [Flux](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html) reactive streams of capabilities.
It allows to have an infinite stream of capabilities which are consumed (and produced) as needed.
Capability providers may furnish additional information about capabilities.
This information can be used for filtering or sorting providers. 

[^javadoc]: [Javadoc](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/package-summary.html)

A non-technical example of requirement/capability chain graph is a food chain/graph. 
Food is a requirement. Or "I want to eat" is a requirement.
Bread and, say fried eggs are two capabilities meeting/addressing the requirement. 
Bread requires "wheat", "water", and "bake" capabilities. 
Fried eggs require "egg", "oil", and "fry" capabilities.
Bread capability provider may implement ``Vegan`` marker interface which can be used for filtering.
All food capabilities may implement ``NutritionalInformation`` interface - it can be used for filtering or sorting.

A more technical example is Java [ServiceLoader](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/ServiceLoader.html)
with service type being a requirement and an instance of the service class being a capability.   

Nasdanika capability framework can operate on top of ``ServiceLoader`` and may be thought of as a generalization of service loading.
In essence, the capability framework is a [backward chaining](https://en.wikipedia.org/wiki/Backward_chaining) engine as shown in one of the example below.

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

## EMF

Most of Nasdanika capabilities are based on [Eclipse Modeling Framework](https://eclipse.dev/modeling/emf/) (EMF)[^vogella_emf], Ecore[^ecore] models in particular.
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

