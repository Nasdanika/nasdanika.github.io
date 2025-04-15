MCP modules provides building blocks to create [MCP](https://modelcontextprotocol.io/introduction) servers on top of Nasdanika capabilities with built-in [telemetry](../../core/telemetry/index.html).

There are two modules:

* mcp - provides:
    * [HttpClientTelemetrySseClientTransport](https://github.com/Nasdanika/ai/blob/main/mcp/src/main/java/org/nasdanika/ai/mcp/HttpClientTelemetrySseClientTransport.java) - SSE client with integrated telemetry. 
    * [TelemetryMcpClientTransportFilter](https://github.com/Nasdanika/ai/blob/main/mcp/src/main/java/org/nasdanika/ai/mcp/TelemetryMcpClientTransportFilter.java) - a filter for [McpClientTransport](https://javadoc.io/doc/io.modelcontextprotocol.sdk/mcp/latest/io.modelcontextprotocol.sdk.mcp/io/modelcontextprotocol/spec/McpClientTransport.html) implementations adding telemetry.
* mcp-http - provides [HttpServerRoutesTransportProvider](https://github.com/Nasdanika/ai/blob/main/mcp-http/src/main/java/org/nasdanika/ai/mcp/http/HttpServerRoutesTransportProvider.java) which provides an HTTP/SSE transport on top of [Reactor Netty HTTP Routing and SSE](https://projectreactor.io/docs/netty/1.2.5/reference/http-server.html#routing-http) with integrated telemetry.

You can find examples of using the above classes to build telemetry-enabled MCP servers and clients in [TestMcp](https://github.com/Nasdanika/ai/blob/main/mcp/src/test/java/org/nasdanika/ai/mcp/tests/TestMcp.java) class.

## Roadmap

Nasdanika MCP servers will have three dimensions:

* Capabilities (tools, resources, ...) 
* Transports (STDIO, SSE)
* Telemetry instrumentation scope name

The plan is to create the following classes:

### AbstractMcpServerCommand

This class will extends ``CommandGroup`` and will use ``McpAsyncServer``. 
It will obtain capabilities and instrumentation scope name from abstract methods and wrap them into telemetry filters.
Sync capabilities will be wrapped into async capabilities in the same way as it is done in the ``McpSyncServer``:

```java
this.asyncServer.addTool(McpServerFeatures.AsyncToolSpecification.fromSync(toolHandler)).block();
```
 
The command will have two sub-commands for transport providers.

The transport provider sub-commands will be responsible for creating a transport provider and then passing it to the parent command for execution. 
Something like this:

```java
McpServerTransportProvider transportProvider = ... // Create, use options and arguments
parent.execute(transportProvider)
```

### McpServerCommand

This class will extends ``AbstractMcpServerCommand`` and bind to the root command. 
It will take capabilities and instrumentation scope name as constructor arguments and implement abstract methods from the superclass to get capabilities and instrumentation scope name.

### Capability factory

The capability factory will collect specification capabilities, both sync and async, e.g. [SyncToolSpecification](https://javadoc.io/doc/io.modelcontextprotocol.sdk/mcp/latest/io.modelcontextprotocol.sdk.mcp/io/modelcontextprotocol/server/McpServerFeatures.SyncToolSpecification.html).
If there is at least one capability, an instance of ``McpServerCommand`` would be created. 
This approach would allow to assemble servers in a declarative way, by adding dependencies to ``pom.xml`` and ``module-info.java``.

### Documentation generator

A custom command documentation generator which would generate documentation for MCP server capabilities as child pages of the server command page.
So the command documentation will also serve as a tools/resources catalog.

### Custom server commands

Custom server commands can be created by extending ``AbstractMcpServerCommand``. 
Capabilities for such commands can be loaded based on command configuration - command line arguments and options. 

For example:

* Resources loaded from ``search-documents.json`` files of sites generated from [HTML Application models](https://html-app.models.nasdanika.org/index.html). 
E.g. [search-documents.json](../../search-documents.json) of this site.
* Resources created from "narrated" models/diagrams. For example, in the [sample family](https://nasdanika-demos.github.io/family-semantic-mapping/)
the model with derived relationships and capability-based reasoning can be used to explain that:
    * [Isa](https://nasdanika-demos.github.io/family-semantic-mapping/references/members/isa/index.html) is a [Woman](https://family.models.nasdanika.org/references/eClassifiers/Woman/index.html) and a [mother](https://family.models.nasdanika.org/references/eClassifiers/Person/references/eStructuralFeatures/mother/index.html) of [Elias](https://nasdanika-demos.github.io/family-semantic-mapping/references/members/elias/index.html)
    * Paul is a [parent](https://family.models.nasdanika.org/references/eClassifiers/Person/references/eStructuralFeatures/parents/index.html) of Lea
    * Paul is a father of Lea
    * Lea is a child of Paul
    * Lea is a daughter of Paul
    * Elias is a sibling of Lea
    * Elias is a brother of Lea
    * ...
* RAG and chat on top of models/sites using [sampling](https://modelcontextprotocol.io/docs/concepts/sampling) or own LLMs. In the case of ``search-documents.json`` embeddings can be pre-calculated and stored in the file.
* Tools/resources loaded from [Invocable URIs](../../core/capability/index.html#loading-invocables-from-uris)
