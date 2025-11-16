Nasdanika Core HTTP module provides classes and interfaces for serving HTTP requests with [Reactor Netty HTTP Server routes](https://projectreactor.io/docs/netty/1.2.1/reference/http-server.html#routing-http).

* [Sources](https://github.com/Nasdanika/core/tree/master/http)
* [Javadoc](https://javadoc.io/doc/org.nasdanika.core/http/latest/org.nasdanika.http/module-summary.html)

[TOC levels=6]

## Reflective routing

[ReflectiveHttpServerRouteBuilder](https://javadoc.io/doc/org.nasdanika.core/http/latest/org.nasdanika.http/org/nasdanika/http/ReflectiveHttpServerRouteBuilder.html) uses 
[Route](https://javadoc.io/doc/org.nasdanika.core/http/latest/org.nasdanika.http/org/nasdanika/http/ReflectiveHttpServerRouteBuilder.Route.html) 
and [RouteBuilder](https://javadoc.io/doc/org.nasdanika.core/http/latest/org.nasdanika.http/org/nasdanika/http/ReflectiveHttpServerRouteBuilder.RouteBuilder.html)
annotations on methods, fields and types to build server routes. 

[Example](https://github.com/Nasdanika-Demos/cli/blob/main/src/main/java/org/nasdanika/launcher/demo/http/DemoReflectiveHttpRoutes.java): 

```java
package org.nasdanika.launcher.demo.http;

import org.nasdanika.http.ReflectiveHttpServerRouteBuilder.Route;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

@Route("/test/")
public class DemoReflectiveHttpRoutes {

    /**
     * In this handler method GET is derived from the get prefix
     * and path is derived from Hello 
     */
    @Route
    public Publisher<Void> getHello(
            HttpServerRequest request, 
            HttpServerResponse response) {
        return response.sendString(Mono.just("getHello()"));
    }
    
    /**
     * In this handler the HTTP method is GET because the 
     * first segment "hola" doesn't  match any HTTP method.
     * The path is "hola/soy/dora"
     */
    @Route
    public Publisher<Void> holaSoyDora(
            HttpServerRequest request, 
            HttpServerResponse response) {
        return response.sendString(Mono.just("holaSoyDora()"));
    }
    
    /**
     * In this handler the path is explicitly specified
     * by the Route annotation 
     */
    @Route("do-something")
    public Publisher<Void> doSomething(
            HttpServerRequest request, 
            HttpServerResponse response) {
        return response.sendString(Mono.just("do someting"));
    }
    
    @RouteBuilder("field-route-builder")
    public HttpServerRouteBuilder routeBuilder = routes -> {
        routes.get("/hello", (request, response) -> response.sendString(Mono.just("Hello from field route builder!")));             
    };

    @RouteBuilder("getter-route-builder")
    public HttpServerRouteBuilder getRouteBuilder() {
        return  routes -> {
            routes.get("/hello", (request, response) -> response.sendString(Mono.just("Hello from getter route builder!")));
        };
    };  

    @RouteBuilder("route-builder-method")
    public void buildRoutes(HttpServerRoutes routes) {
        routes.get("/hello", (request, response) -> response.sendString(Mono.just("Hello from route builder method!")));
    };  
    
    @Route
    public JSONObject getApiSearch(
            HttpServerRequest request, 
            HttpServerResponse response) {
        JSONObject result = new JSONObject();
        result.put("result", "Hello World!");
        return result;
    }
        
    @Route("do-something-else")
    public Mono<String> doSomethingElse(
            HttpServerRequest request, 
            HttpServerResponse response) {
        return Mono.just("do someting else");
    }   
    
}
```

The code snippet above shows three GET handler methods, a route builder field and two flavors of builder methods. 
All of their paths are prefixed with ``/test/`` from the class level annotation. 

The ``getApiSearch()`` method returns [JSONObject](https://javadoc.io/static/org.json/json/20250107/org/json/JSONObject.html). 
The returned value is converted to ``String`` and is sent as a response with ``application/json`` content type header.

Conversion works as explained below. 
For ``Mono`` and ``Flux`` ``Mono|Flux<String>`` is assumed by default and ``request.sendString()`` is used.
The mono result is mapped to ``String`` using ``ReflectiveHttpServerRouteBuilder.toString(Object)``. 

Set ``binary`` attribute of ``@Route`` annotation to ``true`` for binary content so ``request.sendByteArray()`` is used.
In this case mapping to ``byte[]`` is performed by ``ReflectiveHttpServerRouteBuilder.toByteArray(Object)``.

Both ``ReflectiveHttpServerRouteBuilder.toString(Object)`` and ``ReflectiveHttpServerRouteBuilder.toByteArray(Object)`` use [DefaultConverter](https://javadoc.io/doc/org.nasdanika.core/common/latest/org.nasdanika.common/org/nasdanika/common/DefaultConverter.html).INSTANCE obtained from ``getConverter()`` method. 
These methods can be overridden to add support for additional conversions. E.g. [URI](https://javadoc.io/doc/org.eclipse.emf/org.eclipse.emf.common/latest/org/eclipse/emf/common/util/URI.html) to ``String`` or ``byte[]`` using [ResourceSet](https://javadoc.io/doc/org.eclipse.emf/org.eclipse.emf.ecore/latest/org/eclipse/emf/ecore/resource/ResourceSet.html) [URI Converter](https://javadoc.io/doc/org.eclipse.emf/org.eclipse.emf.ecore/latest/org/eclipse/emf/ecore/resource/URIConverter.html) obtained as [capability](../capability/index.html) to, say, serve resources from Maven jars or GitLab using respective [URIHandler](https://javadoc.io/doc/org.eclipse.emf/org.eclipse.emf.ecore/latest/org/eclipse/emf/ecore/resource/URIHandler.html)s - see Maven/Gitlab models documentation for more details.

* ``Publisher`` which is not ``Mono`` or ``Flux`` is cast to ``Publisher<Void>`` and returned.
* ``String`` is not converted
* ``JSONObject`` is converted to string and also ``application/json`` content type header is added
* ``JSONArray`` is converted to string and also ``application/json`` content type header is added
* ``byte[]`` is not converted
* ``InputStream`` is converted to ``byte[]``
* Other types are converted to a byte array, using ``ReflectiveHttpServerRouteBuilder.toByteArray(Object)``, if ``binary`` is true and to a string using ``ReflectiveHttpServerRouteBuilder.toString(Object)`` otherwise.

The code snippet below shows how to use the above handlers. Note that the handler target is registered with ``/reflective prefix``. 
As such, the full path for, say, getHello() method, is ``/reflective/test/hello``.

```java
ReflectiveHttpServerRouteBuilder builder = new ReflectiveHttpServerRouteBuilder();
builder.addTargets("/reflective", new DemoReflectiveHttpRoutes());

DisposableServer server = HttpServer
  .create()
  .port(8080)
  .route(builder::buildRoutes)
  .bindNow();

try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
 LineReader lineReader = LineReaderBuilder
   .builder()
            .terminal(terminal)
            .build();
 
 String prompt = "http-server>";
    while (true) {
        String line = null;
        line = lineReader.readLine(prompt);
        System.out.println("Got: " + line);
        if ("exit".equals(line)) {
         break;
        }
    }
}

server.dispose();
server.onDispose().block();
```

### Factories

Route building can be organized into hierarchy using methods and fields annotated with [Reflector.Factory](https://javadoc.io/doc/org.nasdanika.core/common/latest/org.nasdanika.common/org/nasdanika/common/Reflector.Factory.html)
or [Reflector.Factories](https://javadoc.io/doc/org.nasdanika.core/common/latest/org.nasdanika.common/org/nasdanika/common/Reflector.Factories.html) annotations. 
Such methods can also be annotated with ``Route`` - in this case route value is used as a prefix. Below snippets show two levels of factory hierarchy:

```java
import org.nasdanika.common.Reflector;
import org.nasdanika.http.ReflectiveHttpServerRouteBuilder.Route;

/**
 * Demo of a hierarchical routing with factory
 */
@Route("/super-factory")
public class ReflectorSuperFactory {
    
    @Route("/super-demo")
    @Reflector.Factory
    public ReflectorFactory getRoutes() {
        return new ReflectorFactory();
    }   

}
```

```java
import org.nasdanika.common.Reflector;
import org.nasdanika.http.ReflectiveHttpServerRouteBuilder.Route;

/**
 * Demo of a hierarchical routing with factory
 */
@Route("/factory")
public class ReflectorFactory {
    
    @Route("/demo")
    @Reflector.Factory
    public DemoReflectiveHttpRoutes getRoutes() {
        return new DemoReflectiveHttpRoutes();
    }   

}
```

With factories you can assemble web applications from parameterizable building blocks.

### Serving

#### Java

```java
ReflectiveHttpServerRouteBuilder builder = new ReflectiveHttpServerRouteBuilder();
builder.addTargets("/reflective", new DemoReflectiveHttpRoutes());

DisposableServer server = HttpServer
  .create()
  .port(8080)
  .route(builder::buildRoutes)
  .bindNow();

try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
 LineReader lineReader = LineReaderBuilder
   .builder()
            .terminal(terminal)
            .build();
 
 String prompt = "http-server>";
    while (true) {
        String line = null;
        line = lineReader.readLine(prompt);
        System.out.println("Got: " + line);
        if ("exit".equals(line)) {
         break;
        }
    }
}

server.dispose();
server.onDispose().block();
```

#### http-server CLI command

You can build a custom [CLI](../cli/index.html) and serve routes with [http-server](../../nsd-cli/nsd/http-server/index.html) command by
creating a capability factory and registering it in ``module-info.java``.

##### Capability factory

```
public class DemoReflectiveHttpRoutesFactory extends ServiceCapabilityFactory<Void, HttpServerRouteBuilder> {
  
 @Override
 public boolean isFor(Class<?> type, Object requirement) {
  return HttpServerRouteBuilder.class == type && requirement == null;
 }

 @Override
 protected CompletionStage<Iterable<CapabilityProvider<HttpServerRouteBuilder>>> createService(
   Class<HttpServerRouteBuilder> serviceType, 
   Void serviceRequirement, 
   Loader loader,
   ProgressMonitor progressMonitor) {
  
  ReflectiveHttpServerRouteBuilder builder = new ReflectiveHttpServerRouteBuilder();
  builder.addTargets("/reflective", new DemoReflectiveHttpRoutes());    
  return wrap(builder);
 }
 
}
```

##### module-info.java

```java
...
provides CapabilityFactory with DemoDiagramRoutesBuilderFactory;    
...

```

### Telemetry

You may pass [TelemetryFilter](https://github.com/Nasdanika/core/blob/master/http/src/main/java/org/nasdanika/http/TelemetryFilter.java) to ``ReflectiveHttpServerRouteBuilder``. 
In this case route methods which return ``String``, ``JSONArray``, ``JSONObject``, ``byte[]`` or ``InputStream`` will be filtered to collect telemetry
and propagate telemetry context along the reactive chain. 

## Graph/Diagram processors 

[HttpServerRouteBuilder](https://javadoc.io/doc/org.nasdanika.core/http/latest/org.nasdanika.http/org/nasdanika/http/HttpServerRouteBuilder.html)
has a static ``buildRoutes(Collection<ProcessorInfo<P>> processorInfos, String routeProperty, HttpServerRoutes routes)`` method for building routes from graph/diagram processors implementing ``BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>>`` or implementing/[adaptable to](https://javadoc.io/doc/org.nasdanika.core/common/latest/org.nasdanika.common/org/nasdanika/common/Adaptable.html) HttpServerRouteBuilder.
You can find examples in [Nasdanika Demo CLI](https://github.com/Nasdanika-Demos/cli).

### Processors

#### Groovy

Below is a [Groovy processor](https://github.com/Nasdanika-Demos/cli/blob/main/test-data/drawio-http/person.groovy) for single route:

```groovy
 1  import reactor.core.publisher.Mono 
 2  import org.nasdanika.drawio.Node
 3  import org.nasdanika.graph.processor.ProcessorElement
 4  
 5  new java.util.function.BiFunction() {
 6  
 7      @ProcessorElement
 8      public Node element;
 9      
10      def apply(request, response) {
11          response.sendString(Mono.just(element.getLabel()))
12      }
13  
14  }
```

The diagram element is injected into the ``element`` field at line ``8`` because the fields is annotated with [@ProcessorElement](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/processor/ProcessorElement.html) annotation (line ``7``). 
``apply()`` method at line ``10`` processes HTTP requests by sending the element label.

#### Java

##### Route (BiFunction)

Below is a [Java processor](https://github.com/Nasdanika-Demos/cli/blob/main/src/main/java/org/nasdanika/launcher/demo/drawio/SystemHttpHandler.java) serving a single route:

```java
 1  package org.nasdanika.launcher.demo.drawio;
 2  
 3  import java.util.concurrent.CompletionStage;
 4  import java.util.function.BiConsumer;
 5  import java.util.function.BiFunction;
 6  import java.util.function.Consumer;
 7  
 8  import org.nasdanika.capability.CapabilityFactory.Loader;
 9  import org.nasdanika.common.Invocable;
10  import org.nasdanika.common.ProgressMonitor;
11  import org.nasdanika.drawio.Node;
12  import org.nasdanika.graph.Element;
13  import org.nasdanika.graph.processor.ConnectionProcessorConfig;
14  import org.nasdanika.graph.processor.NodeProcessorConfig;
15  import org.nasdanika.graph.processor.ProcessorConfig;
16  import org.nasdanika.graph.processor.ProcessorElement;
17  import org.nasdanika.graph.processor.ProcessorInfo;
18  import org.reactivestreams.Publisher;
19  
20  import reactor.core.publisher.Mono;
21  import reactor.netty.http.server.HttpServerRequest;
22  import reactor.netty.http.server.HttpServerResponse;
23  
24  /**
25   * Diagram element processor which processes HTTP requests 
26   */
27  public class SystemHttpHandler implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {
28      
29      private String amount;
30      
31      @ProcessorElement
32      public void setElement(Node element) {
33          this.amount = element.getProperty("amount");
34      }
35  
36      /**
37       * This is the constructor signature for graph processor classes which are to be instantiated by URIInvocableCapabilityFactory (org.nasdanika.capability.factories.URIInvocableCapabilityFactory).
38       * Config may be of specific types {@link ProcessorConfig} - {@link NodeProcessorConfig} or {@link ConnectionProcessorConfig}.  
39       * @param loader
40       * @param loaderProgressMonitor
41       * @param data
42       * @param fragment
43       * @param config
44       * @param infoProvider
45       * @param endpointWiringStageConsumer
46       * @param wiringProgressMonitor
47       */
48      public SystemHttpHandler(
49              Loader loader,
50              ProgressMonitor loaderProgressMonitor,
51              Object data,
52              String fragment,
53              ProcessorConfig config,
54              BiConsumer<Element, BiConsumer<ProcessorInfo<Invocable>, ProgressMonitor>> infoProvider,
55              Consumer<CompletionStage<?>> endpointWiringStageConsumer,
56              ProgressMonitor wiringProgressMonitor) {
57          
58          System.out.println("I got constructed " + this);
59      }
60  
61      @Override
62      public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
63          return response.sendString(Mono.just("Account: " + request.param("account") + ", Amount: " + amount));
64      }
65  
66  }
```

``apply()`` method at line ``62`` processes HTTP ``GET`` requests.

##### Route Builder (HttpServerRouteBuilder)

The below [processor](https://github.com/Nasdanika-Demos/cli/blob/main/src/main/java/org/nasdanika/launcher/demo/drawio/RouteBuilderProcessor.java) implements ``HttpServerRouteBuilder``:

```java
package org.nasdanika.launcher.demo.drawio;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.nasdanika.capability.CapabilityFactory.Loader;
import org.nasdanika.common.Invocable;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.drawio.Node;
import org.nasdanika.graph.Element;
import org.nasdanika.graph.processor.ConnectionProcessorConfig;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.ProcessorConfig;
import org.nasdanika.graph.processor.ProcessorElement;
import org.nasdanika.graph.processor.ProcessorInfo;
import org.nasdanika.http.HttpServerRouteBuilder;

import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRoutes;

/**
 * Diagram element processor which builds HTTP routes 
 */
public class RouteBuilderProcessor implements HttpServerRouteBuilder {
    
    private String amount;
    
    @ProcessorElement
    public void setElement(Node element) {
        this.amount = element.getProperty("amount");
    }

    /**
     * This is the constructor signature for graph processor classes which are to be instantiated by URIInvocableCapabilityFactory (org.nasdanika.capability.factories.URIInvocableCapabilityFactory).
     * Config may be of specific types {@link ProcessorConfig} - {@link NodeProcessorConfig} or {@link ConnectionProcessorConfig}.  
     * @param loader
     * @param loaderProgressMonitor
     * @param data
     * @param fragment
     * @param config
     * @param infoProvider
     * @param endpointWiringStageConsumer
     * @param wiringProgressMonitor
     */
    public RouteBuilderProcessor(
            Loader loader,
            ProgressMonitor loaderProgressMonitor,
            Object data,
            String fragment,
            ProcessorConfig config,
            BiConsumer<Element, BiConsumer<ProcessorInfo<Invocable>, ProgressMonitor>> infoProvider,
            Consumer<CompletionStage<?>> endpointWiringStageConsumer,
            ProgressMonitor wiringProgressMonitor) {
        
        System.out.println("I got constructed " + this);
    }

    @Override
    public void buildRoutes(HttpServerRoutes routes) {
        routes.get("/balance", (request, response) -> response.sendString(Mono.just("Account: " + request.param("account") + ", Amount: " + amount)));      
    }

}
``` 

### Serving

#### drawio http-server

One way to have diagrams to process HTTP requests is to use [drawio](../../nsd-cli/nsd/drawio/index.html) [http-server](../../nsd-cli/nsd/drawio/http-server/index.html) command pipeline. 
Below is a sample command line:

```
drawio test-data/drawio-http/diagram.drawio http-server --http-port=8080 processor route
```

#### http-server

Another option is to use the http-server command in a custom CLI assembly. 
In this case diagrams shall be provided as ``HttpServerRouteBuilder`` capabilities.
This is done by creating a subclass of [DiagramRoutesBuilderFactory](https://javadoc.io/doc/org.nasdanika.core/http/latest/org.nasdanika.http/org/nasdanika/http/DiagramRoutesBuilderFactory.html):

```java
package org.nasdanika.launcher.demo.drawio;

import org.eclipse.emf.common.util.URI;
import org.nasdanika.common.Util;
import org.nasdanika.http.DiagramRoutesBuilderFactory;

public class DemoDiagramRoutesBuilderFactory extends DiagramRoutesBuilderFactory {

 public DemoDiagramRoutesBuilderFactory() {
  super(
    URI
     .createURI("system.drawio")
     .resolve(Util.createClassURI(DemoDiagramRoutesBuilderFactory.class)), 
    "processor", 
    "route");
 }
 
}
```

and adding it to [module-info.java](https://github.com/Nasdanika-Demos/cli/blob/main/src/main/java/module-info.java#L41) ``provides``.

#### Java

The below [snippet](https://github.com/Nasdanika-Demos/cli/blob/main/src/test/java/org/nasdanika/launcher/demo/tests/TestHttpServerRoutes.java#L145) shows how to serve diagram processors routes with Java:

```java
Document document = Document.load(
        URI.createFileURI(new File("test-data/drawio-http/diagram.drawio").getCanonicalPath()), 
        null, 
        null);

ElementProcessorFactory<Object> elementProcessorFactory = new ElementProcessorFactory<Object>(
        document, 
        new CapabilityLoader(), 
        "processor");
    
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();

Map<Element, ProcessorInfo<Object>> processors = elementProcessorFactory.createProcessors(
        null, 
        null, 
        progressMonitor);

DisposableServer server = HttpServer
        .create()
        .port(8080)
        .route(routes -> HttpServerRouteBuilder.buildRoutes(processors.values(), "route", routes))
        .bindNow();

try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
    LineReader lineReader = LineReaderBuilder
            .builder()
            .terminal(terminal)
            .build();
    
    String prompt = "http-server>";
    while (true) {
        String line = null;
        line = lineReader.readLine(prompt);
        System.out.println("Got: " + line);
        if ("exit".equals(line)) {
            break;
        }
    }
}

server.dispose();
server.onDispose().block();
```

## SerpapiConnector

``SerpapiConnector`` class uses [SerpApi](https://serpapi.com/) Google search to find pages.
Then it retrieves them, extracts main content (content of the ``main`` element, configurable) and the converts the main content to Markdown.
Search result includes SerpApi data, page content, main content and markdown main content. 
The primary purpose of this class it to provide grounding for LLMs and AI agents with information that matters (main content) and in a format which is semantically structured.

Example:

```java
String apiKey = System.getenv("SERPER_KEY");
String query = "What is a kernel function in microsoft semantic kernel";

SerpapiConnector serpApiConnector = new SerpapiConnector(apiKey, "learn.microsoft.com/en-us/semantic-kernel");
Flux<SearchResult> results = serpApiConnector.search(query, 10, 0);
List<SearchResult> resultList = results.collectList().block();
for (SearchResult result: resultList) {
    System.out.println("===");
    System.out.println(result.title());
    System.out.println();
    System.out.println(result.markdownMainContent());
}
```
