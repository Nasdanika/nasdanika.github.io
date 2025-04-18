MCP modules provides building blocks to create [MCP](https://modelcontextprotocol.io/introduction) servers and clients on top of Nasdanika capabilities with built-in [telemetry](../../core/telemetry/index.html).

There are three modules:

* ``mcp`` - core MCP-related functionality
* ``mcp-sse`` - SSE/HTTP related functionality
* ``mcp-help`` - Help contributor generating capabilities tables

## Server

Nasdanika MCP servers will have three dimensions:

* Capabilities (prompts, resources, tools) 
* Transports (STDIO, SSE)
* Telemetry instrumentation scope name

[McpServerCommandBase](https://github.com/Nasdanika/ai/blob/main/mcp/src/main/java/org/nasdanika/ai/mcp/McpServerCommandBase.java) class is a base class for MCP server [CLI](../../core/cli/index.html) commands.
Subclasses shall override one or more ``getXXXSpecificaion()`` methods to provide capabilities. 
The command implements ``McpAsyncServerProvider`` and there are two sub-commands binding to implementations of this interface - SSE and STDIO transport commands.

[McpServerCommand](https://github.com/Nasdanika/ai/blob/main/mcp/src/main/java/org/nasdanika/ai/mcp/McpServerCommand.java) is a concrete command which binds as a sub-command of the root command.
It collects specification capabilities and binds to the root command if there is at least one capability.
Help generator for the command generates documentation for provided capabilities.

[MCP Server CLI assembly](https://nasdanika-demos.github.io/mcp-server/) demonstrates how to contribute MCP server capabilities.

### Contributing a capability

#### Capability factory

```java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class SyncCalculatorCapabilityFactory extends ServiceCapabilityFactory<Void, SyncToolSpecification> {

    @Override
    public boolean isFor(Class<?> type, Object requirement) {
        return SyncToolSpecification.class == type && requirement == null;
    }

    @Override
    protected CompletionStage<Iterable<CapabilityProvider<SyncToolSpecification>>> createService(
            Class<SyncToolSpecification> serviceType, 
            Void serviceRequirement, 
            Loader loader,
            ProgressMonitor progressMonitor) {

        
        String schema = """
                {
                  "type" : "object",
                  "id" : "urn:jsonschema:Operation",
                  "properties" : {
                    "operation" : {
                      "type" : "string"
                    },
                    "a" : {
                      "type" : "number"
                    },
                    "b" : {
                      "type" : "number"
                    }
                  }
                }
                """;
            
            SyncToolSpecification syncToolSpecification = new McpServerFeatures.SyncToolSpecification(
                new Tool("calculator", "Nasdanika calculator of all great things", schema), 
                (exchange, arguments) -> {
                    List<Content> result = new ArrayList<>();
                    result.add(new TextContent("Result: " + arguments));
                    
                    return new CallToolResult(result, false);
                }
            );

        return wrap(syncToolSpecification);         
    }
    
}
```

#### module-info.java

```java
import org.nasdanika.capability.CapabilityFactory;
import org.nasdanika.demos.mcp.server.capabilities.SyncCalculatorCapabilityFactory;

module org.nasdanika.demos.mcp.server {
    
    exports org.nasdanika.demos.mcp.server;
    
    requires transitive org.nasdanika.ai.mcp.sse;
    
    provides CapabilityFactory with 
        SyncCalculatorCapabilityFactory;
                
}
```

### Running a server

#### SSE

```
nsd mcp-server sse --http-port=8080
```

#### STDIO

```
nsd mcp-server stdio
```

### Generating documentation

```
nsd help site --page-template="page-template.yml#/" --root-action-icon=https://docs.nasdanika.org/images/nasdanika-logo.png --root-action-location=https://github.com/Nasdanika-Demos --root-action-text="Nasdanika Demos" docs
```

## Client

[TelemetryMcpClientTransportFilter](https://github.com/Nasdanika/ai/blob/main/mcp/src/main/java/org/nasdanika/ai/mcp/TelemetryMcpClientTransportFilter.java) is a filter for [McpClientTransport](https://javadoc.io/doc/io.modelcontextprotocol.sdk/mcp/latest/io.modelcontextprotocol.sdk.mcp/io/modelcontextprotocol/spec/McpClientTransport.html) implementations adding telemetry.
[HttpClientTelemetrySseClientTransport](https://github.com/Nasdanika/ai/blob/main/mcp/src/main/java/org/nasdanika/ai/mcp/HttpClientTelemetrySseClientTransport.java) propagates the trace to the server side. 
It can be used if the server supports telemetry, e.g. if it is built using Nasdanika MCP classes.

```java
OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
McpClientTransport transport = new HttpClientTelemetrySseClientTransport(
        "http://localhost:8080", 
        openTelemetry.getTracer(TestMcp.class.getName() + ".transport"),
        openTelemetry.getPropagators().getTextMapPropagator(),
        null);

Tracer tracer = openTelemetry.getTracer(TestMcp.class.getName());       
Span span = TelemetryUtil.buildSpan(tracer.spanBuilder("testSseTelemetryClient")).startSpan();
        
try (Scope scope = span.makeCurrent()) {
    TelemetryMcpClientTransportFilter transportFilter = new TelemetryMcpClientTransportFilter(
            transport, 
            openTelemetry.getTracer(TestMcp.class.getName() + ".transportFilter"), 
            Context.current());         

    McpSyncClient client = McpClient.sync(transportFilter)
            .requestTimeout(Duration.ofSeconds(10))
            .capabilities(ClientCapabilities.builder()
                .roots(true)      // Enable roots capability
                .sampling()       // Enable sampling capability
                .build())
            .sampling(request -> {                  
                CreateMessageResult result = null;
                return result;
            })
            .build();       
    
    client.initialize();
    ListResourcesResult resources = client.listResources();
    System.out.println(resources);
    
    ReadResourceResult resource = client.readResource(new ReadResourceRequest("nasdanika://drawio"));           
    System.out.println(resource.contents());
    
    // List available tools
    ListToolsResult tools = client.listTools();
    System.out.println(tools);

    // Call a tool
    CallToolResult result = client.callTool(
        new CallToolRequest("calculator", 
            Map.of("operation", "add", "a", 2, "b", 3))
    );
    System.out.println(result);         
    
    client.closeGracefully();
} finally {
    span.end();
}
```

Please note that in the above example the current telemetry context is passed to the ``TelemetryMcpClientTransportFilter`` constructor. 
It is needed because the context does not propagate all the way along the reactive chain. 

You can find more examples of using the above classes to build telemetry-enabled MCP servers and clients in [TestMcp](https://github.com/Nasdanika/ai/blob/main/mcp-sse/src/test/java/org/nasdanika/ai/mcp/sse/tests/TestMcp.java) class.

## Roadmap

### Annotations and a reflective capability factory

Create annotations for prompts, resources and tools, infer sync/async from the return type.
Description and tool schema is inline with expansion tokens to include content loaded from a URI, including classpath resources.
Category and documentation for command documentation generation.

Reflective factory using ``Reflector``. 
If tools need capabilities, they shall be obtained during target construction/initialization - the reflector would deal just with creation of specifications.