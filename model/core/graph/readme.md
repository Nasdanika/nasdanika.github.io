Nasdanika Graph module provides classes for visiting and processing [graphs](https://en.wikipedia.org/wiki/Graph_(discrete_mathematics)) with two types of relationships between graph elements:

* Containment - one element is contained by another
* Connection - one element (node) connecting to another node via a connection.

On the diagram below containment relationships are shown in bold black and connections in blue

```drawio-resource
./core/graph/overview.drawio
```

Examples of such graphs:

* A file system with directories containing files and other directories. Connections may take multiple forms such as symbolic links or files, e.g. HTML files, referencing other files.
* Organizational structure with a hierarchy of organizational units and connections between them. For example, one unit may pass work product to another unit, or a unit may provide services to other units.
* Country, state, county, city, street, house, people living in that house; family relationships between people and ownership relationships between people and houses.
* Diagrams, such as [Drawio](https://www.diagrams.net/) diagrams with a diagram file (resource) containing a [Document](https://javadoc.io/doc/org.nasdanika.core/drawio/latest/org.nasdanika.drawio/org/nasdanika/drawio/Document.html) which contains [pages](https://javadoc.io/doc/org.nasdanika.core/drawio/latest/org.nasdanika.drawio/org/nasdanika/drawio/Page.html), pages containing [layers](https://javadoc.io/doc/org.nasdanika.core/drawio/latest/org.nasdanika.drawio/org/nasdanika/drawio/Layer.html), and layers containing [nodes](https://javadoc.io/doc/org.nasdanika.core/drawio/latest/org.nasdanika.drawio/org/nasdanika/drawio/Node.html) and [connections](https://javadoc.io/doc/org.nasdanika.core/drawio/latest/org.nasdanika.drawio/org/nasdanika/drawio/Connection.html). Nodes may be nested. [Nasdanika Drawio](../drawio/index.html) is a module for working with Drawio diagrams. It is built on top of this module.
* Processes/(work)flows - processes consist of activities and nested processes. Activities are connected by transitions.
* Distributed systems, such as cloud solutions - availability zones, data centers, clusters, nodes, pods, containers, processes inside containers. All of them communicating to each other via network connections.
* Work hierarchy and dependencies - in issue trackers issues may be organized into a hierarchy (e.g. Initiative, Epic, Story, Sub-Task in Jira) and have different types of dependencies.
* In [Java](https://java.models.nasdanika.org/) a jar contains packages containing sub-packages and classes. Classes contain fields and methods. Fields reference their types, methods call methods of other classes, ...
* [EMF Ecore](https://www.eclipse.org/modeling/emf/) models contain packages. Packages contain sub-packages and classifiers including classes. Classes contain references to other classes. References may be configured as containment (composition) or non-containment.

   
---

[TOC levels=6]

## Resources

* [Sources](https://github.com/Nasdanika/core/tree/master/graph)
* [Javadoc](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/module-summary.html)
* [Medium stories](https://medium.com/nasdanika):
    * [General purpose executable graphs and diagrams](https://medium.com/nasdanika/general-purpose-executable-graphs-and-diagrams-8663deae5248)
    * [Concurrent Executable Diagrams](https://medium.com/nasdanika/concurrent-executable-diagrams-0cd3bac61e2b)
    * [Executable (computational) graphs & diagrams](https://medium.com/nasdanika/executable-computational-graphs-diagrams-1eeffc80976d)

## Graph API

The graph API has 3 interfaces:

* [Element](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/Element.html) - super-interface for Connection and Node below. Elements may contain other elements. Containment is implemented with ``<T> T accept(BiFunction<? super Element, Map<? extends Element, T>, T> visitor)``, which can be thought of as a hierarchical bottom-up [reduce](https://docs.oracle.com/javase/tutorial/collections/streams/reduction.html) - the visitor function is invoked with an element being visited as its first argument and a map of element's children to results returned by the visitor as the second argument. For leaf elements the second argument may be either an empty map or null. Depending on the map type used by implementations they may also need to implement ``equals()`` and ``hashCode()``.
* [Node](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/Node.html) extends Element and may have incoming and outgoing connections.
* [Connection](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/Connection.html) extends Element and has source and target nodes.

## Processing

Graph processing means associating some behavior with graph elements. 
That behavior (code execution) may modify the graph or perform other actions.

Examples of graph processing:

* Generate code (HTML site) from a diagram. Demos:
    * [Internet Banking System](https://architecture.models.nasdanika.org/demo/internet-banking-system/index.html)
    * [Sample Family](https://family.models.nasdanika.org/demos/mapping/)
    * [Living beings](https://graph.models.nasdanika.org/demo/living-beings/index.html)
* Update a diagram with information from external source. For example, there might be a diagram of a (software) system. Diagram elements can be updated as follows:
    * During development - colors may reflect completion status. Say, in progress elements in blue, completed elements in green, elements with issues in red or amber.
    * In production - color elements based on their monitoring status. Offline - grey, good - green, overloaded - amber, broken - red.
    
The above two examples may be combined - a documentation site might be generated from a system diagram.
The diagram may be updated with statuses as part of the generation process and embedded to the home page.
A click on a diagram element would navigate to an element documentation page, which may contain detailed status information pulled from tracking/monitoring systems during generation.          
    
### Dispatching

One form of graph processing is dispatching of graph elements to Java methods annotated with [Handler](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/Handler.html) annotation.
The annotation takes a [Spring boolean expression](https://docs.spring.io/spring-framework/reference/core/expressions.html). 
Graph elements are passed to methods for which the expression is blank or evaluates to true.

Below is a code snippet from [AliceBobHandlers](https://github.com/Nasdanika/core/blob/master/drawio/src/test/java/org/nasdanika/drawio/tests/AliceBobHandlers.java) class:

```java
@Handler("getProperty('my-property') == 'xyz'")
public String bob(Node bob) {
	System.out.println(bob.getLabel());
	return bob.getLabel();
}
```	

Below is a test method from [TestDrawio.testDispatch()](https://github.com/Nasdanika/core/blob/master/drawio/src/test/java/org/nasdanika/drawio/tests/TestDrawio.java#L630) test method which dispatches to the above handler method:

```java
Document document = Document.load(getClass().getResource("alice-bob.drawio"));
		
AliceBobHandlers aliceBobHandlers = new AliceBobHandlers();		
Object result = document.dispatch(aliceBobHandlers);
System.out.println(result);
```

Dispatching is suitable for processing where processing logic for different graph elements does not need to access processing logic of other elements.
An example of such logic would be updating diagram elements based on statuses retrieved from tracking/monitoring systems - each element is updated individually.

### Processors and processor factories

[Processor](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/package-summary.html) package provides means for creating graph element processors and wiring them together so they can interact.

One area of where such functionality would be needed is executable diagrams. For example, a [flow](https://docs.nasdanika.org/demo-drawio-flow-actions/) processor/simulator.
Activity processors would need to pass control to connected activities via connection processors.
Activity processors may also need to access facilities of their parent processors.

The below diagram shows interaction of two nodes via a connection.
Connections are bi-directional - source processor may interact with the target processor and vice versa.

```drawio-resource
./core/graph/processing.drawio
```

Some connections may be "pass-through" - just passing interactions without doing any processing. 
A pass-through connection is depicted below.


```drawio-resource
./core/graph/pass-through-processing.drawio
```

Graph element processors are wired together with handlers and endpoints:

* A handler is a java object provided by a processor for receiving interactions from other processors via endpoints.
* An endpoint is a java object provided to a processor for interacting with other processors.

An endpoint may be of the same type as a handler or a handler may be used as an endpoint. 
This might be the case if processing is performed sequentially in a single JVM.

Alternatively, an endpoint may be of different type than the handler it passes interactions to. 
For example:

* Endpoint methods may return [Futures](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/Future.html) or [CompletionStages](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/CompletionStage.html) of counterpart handler methods - when an endpoint method is invoked it would invoke handler's method asynchronously. 
* Endpoint methods may take different parameters. E.g. an endpoint method can take [InputStream](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/InputStream.html), save it to some storage and pass a [URL](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/URL.html) to the handler method.

Processors can also interact by looking up other processors in the processor registry.
Endpoints are created by implementations 

Processors are created in two steps:

* Processor configs are created by subclasses of [ProcessorConfigFactory](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/ProcessorConfigFactory.html), e.g. [NopEndpointProcessorConfigFactory](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/NopEndpointProcessorConfigFactory.html)
* Processors are created from configs by subclasses of [ProcessorFactory](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/ProcessorFactory.html) overriding ``createProcessor()`` method. Client code creates processors by calling ``createProcessors()`` method. This method return a registry - ``Map<Element,ProcessorInfo<P>>``. The registry allows the client code to interact with the handler/endpoint/processor wiring created from the graph.    

[TestDrawio.testProcessor()](https://github.com/Nasdanika/core/blob/master/drawio/src/test/java/org/nasdanika/drawio/tests/TestDrawio.java#L380) method provides an example of using an anonymous implementation of ``NopEndpointProcessorFactory`` for graph processing.

#### Reflection

A good deal of processor creation logic is selection of a processor to create for a given graph element in a given situation/context and then "wiring" configuration to the processor. 
There are two processor factory classes and [ReflectiveProcessorWirer](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/ReflectiveProcessorWirer.html) class which make the selection/matching/wiring process easier.


##### ReflectiveProcessorFactoryProvider 

[ReflectiveProcessorFactoryProvider](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/ReflectiveProcessorFactoryProvider.html) invokes methods annotated with [Processor](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/Processor.html) annotation to create processors.

[SyncProcessorFactory](https://github.com/Nasdanika-Models/function-flow/blob/main/processors/targets/java/src/main/java/org/nasdanika/models/functionflow/processors/targets/java/sync/SyncProcessorFactory.java) is an example of  reflective processor factory. Below is one of factory methods:

```java
@Processor(
	type = NodeAdapter.class,
	value = "get() instanceof T(org.nasdanika.models.functionflow.FunctionFlow)")
public Object createFunctionFlowProcessor(
	NodeProcessorConfig<?,?> config, 
	boolean parallel, 
	BiConsumer<Element,BiConsumer<ProcessorInfo<Object>,ProgressMonitor>> infoProvider,
	Function<ProgressMonitor, Object> next,		
	ProgressMonitor progressMonitor) {	
	return new FunctionFlowProcessor();
}
```

###### Capability

[CapabilityProcessorFactory](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/CapabilityProcessorFactory.html) uses the [Nasdanika Capability Framework](../capability/index.html) to delegate processor creation to capability factories. 
[ReflectiveProcessorServiceFactory](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/ReflectiveProcessorServiceFactory.html) provides such a capability by collecting reflective targets from capability providers and then using ``ReflectiveProcessorFactoryProvider`` mentioned above.
This approach provides high level of decoupling between code which executes the graph and code which creates processors.

[FunctionFlowTests](https://github.com/Nasdanika-Models/function-flow/blob/main/processors/targets/java/src/test/java/org/nasdanika/models/functionflow/processors/targets/java/tests/FunctionFlowTests.java) executes a graph loaded from a [Drawio](../drawio/index.html) diagram. 
It constructs a processor factory as shown below:

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();		
CapabilityProcessorFactory<Object, BiFunction<Object, ProgressMonitor, Object>> processorFactory = new CapabilityProcessorFactory<Object, BiFunction<Object, ProgressMonitor, Object>>(
		BiFunction.class, 
		BiFunction.class, 
		BiFunction.class, 
		null, 
		capabilityLoader); 
```

``SyncProcessorFactory`` mentioned above is contributed by [SyncCapabilityFactory](https://github.com/Nasdanika-Models/function-flow/blob/main/processors/targets/java/src/main/java/org/nasdanika/models/functionflow/processors/targets/java/sync/SyncCapabilityFactory.java):

```java
@Override
public boolean canHandle(Object requirement) {
	if (requirement instanceof ReflectiveProcessorFactoryProviderTargetRequirement) {
		ReflectiveProcessorFactoryProviderTargetRequirement<?,?> targetRequirement = (ReflectiveProcessorFactoryProviderTargetRequirement<?,?>) requirement;
		if (targetRequirement.processorType() == BiFunction.class) { // To account for generic parameters create a non-generic sub-interface binding those parameters.
			ProcessorRequirement<?, ?> processorRequiremment = targetRequirement.processorRequirement();
			if (processorRequiremment.handlerType() == BiFunction.class && processorRequiremment.endpointType() == BiFunction.class) {
				return processorRequiremment.requirement() == null; // Customize if needed
			}
		}
	}
	return false;
}

@Override
public CompletionStage<Iterable<CapabilityProvider<Object>>> create(
	ReflectiveProcessorFactoryProviderTargetRequirement<Object, BiFunction<Object, ProgressMonitor, Object>> requirement,
	BiFunction<Object, ProgressMonitor, CompletionStage<Iterable<CapabilityProvider<Object>>>> resolver,
	ProgressMonitor progressMonitor) {		
	return CompletableFuture.completedStage(Collections.singleton(CapabilityProvider.of(new SyncProcessorFactory())));	
}
```

``canHandle()`` returns true if the factory can handle the requriement passed to it. ``create()`` creates a new instance of ``SyncProcessorFactory``. 
Note, that ``create()`` may request other capabilities. Say, an instsance of [OpenAIClient](https://javadoc.io/doc/com.azure/azure-ai-openai/latest/com/azure/ai/openai/OpenAIClient.html) to generate code using chat completions.

``SyncCapabilityFactory`` is registered in [module-info.java](https://github.com/Nasdanika-Models/function-flow/blob/main/processors/targets/java/src/main/java/module-info.java):

```java
exports org.nasdanika.models.functionflow.processors.targets.java.sync;
opens org.nasdanika.models.functionflow.processors.targets.java.sync to org.nasdanika.common; // For loading resources

provides CapabilityFactory with SyncCapabilityFactory;
```

Note that a package containing reflective factories and processors shall be opened to ``org.nasdanika.common`` for reflection to work.

##### Wiring
 
Processors created by the above factories are introspected for the following annotations:

* All processors:
    * [ChildProcessor](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/ChildProcessor.html) - field a method to inject processor or config of element's child matching the selector expression.
    * [ChildProcessors](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/ChildProcessors.html) - field or method to inject a map of children elements to their processor info.
    * [ParentProcessor](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/ParentProcessor.html) - field or method to inject processor or config of element's parent.
    * [ProcessorElement](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/ProcessorElement.html) - field or method to inject the graph element.
    * [Registry](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/Registry.html) - field or method to inject the registry - a map of graph elements to their info.
    * [RegistryEntry](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/RegistryEntry.html) - field or method to inject a matching registry entry.
* Node processors:
    * [IncomingEndpoint](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/IncomingEndpoint.html) - field or method to inject a matching incoming endpoint.
    * [IncomingEndpoints](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/IncomingEndpoints.html) - field or method to inject a map of incoming connections to their endpoints completion stages.
    * [IncomingHandler](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/IncomingHandler.html) - field or method to obtain a handler for an incoming connection.
    * [IncomingHandlerConsumers](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/IncomingHandlerConsumers.html) - field or method to inject a map of incoming connections to ``java.util.function.Consumer``s of handlers. 
    * [OutgoingEndpoint](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/OutgoingEndpoint.html) - field or method to inject a matching outgoing endpoint.
    * [OutgoingEndpoints](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/OutgoingEndpoints.html) - field or method to inject a map of outgoing connections to their endpoints completion stages.
    * [OutgoingHandler](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/OutgoingHandler.html) - field or method to obtain a handler for an outgoing connection.
    * [OutgoingHandlerConsumers](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/OutgoingHandlerConsumers.html) - field or method to inject a map of outgoing connections to consumers of handlers.     
* Connection processors:
    * [SourceEndpoint](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/SourceEndpoint.html) - field or method into which a connection source endpoint is injected.  Source endpoint allows the connection processor to interact with the connection source handler.
    * [SourceHandler](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/SourceHandler.html) - field or method from which the connection source handler is obtained.
    * [TargetEndpoint](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/TargetEndpoint.html) - field or method into which a connection target endpoint is injected. Target endpoint allows the connection processor to interact with the connection target handler.
    * [TargetHandler](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/TargetHandler.html) - Field or method from which the connection target handler is obtained.
    
Element/Node/Connection configuration is declaratively "wired" to processors' fields and methods. Configuration can also be wired imperatively. Declarative and imperative styles can be used together.

Below is an example of using ``@OutgoingEndpoint`` annotation by [StartProcessor](https://github.com/Nasdanika-Models/function-flow/blob/main/processors/targets/java/src/main/java/org/nasdanika/models/functionflow/processors/targets/java/sync/StartProcessor.java):

```java
public class StartProcessor implements BiFunction<Object, ProgressMonitor, Object> {

	protected Collection<BiFunction<Object, ProgressMonitor, Object>> outgoingEndpoints = Collections.synchronizedCollection(new ArrayList<>());	
	
	@Override
	public Object apply(Object arg, ProgressMonitor progressMonitor) {
		Map<BiFunction<Object, ProgressMonitor, Object>, Object> outgoingEndpointsResults = new LinkedHashMap<>();
		for (BiFunction<Object, ProgressMonitor, Object> e: outgoingEndpoints) {
			outgoingEndpointsResults.put(e, e.apply(arg, progressMonitor));
		}
		return outgoingEndpointsResults;
	}
	
	@OutgoingEndpoint
	public void addOutgoingEndpoint(BiFunction<Object, ProgressMonitor, Object> endpoint) {
		outgoingEndpoints.add(endpoint);
	}

}```       
