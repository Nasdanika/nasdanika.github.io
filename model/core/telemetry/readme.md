This module provides an instance of [OpenTelemetry](https://javadoc.io/doc/io.opentelemetry/opentelemetry-api/latest/io/opentelemetry/api/OpenTelemetry.html) as a capability. 
The instance is obtained from [GlobalOpenTelemetry](https://javadoc.io/doc/io.opentelemetry/opentelemetry-api/latest/io/opentelemetry/api/GlobalOpenTelemetry.html). 
The capability factory takes care of installing a logback appender to bridge OpenTelemetry with logging frameworks.

If you are new to OpenTelemetry, check out [Open Telemetry Quick Reference (Java)](https://nasdanika-knowledge.github.io/open-telemetry/index.html) for general information.

This page focuses on Nasdanika-specific functionality.

## Configuration

By default auto-configuration is disabled. 
Set ``otel.java.global-autoconfigure.enabled`` to true to enable auto-configuration. 
Then use [Environment variables and system properties](https://opentelemetry.io/docs/languages/java/configuration/#environment-variables-and-system-properties) to configure the global instance.

This is an example of Java command line properties to configure telemetry repoting to a collector over OTLP protocol: 
``-Dotel.java.global-autoconfigure.enabled=true -Dotel.metrics.exporter=otlp -Dotel.logs.exporter=otlp -Dotel.traces.exporter=otlp -Dotel.exporter.otlp.endpoint=http://<VM external IP>:4317 -Dotel.service.name=<service name>``.

### Logging

Below is a sample ``logback.xml`` file/resource:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="file" class="ch.qos.logback.core.FileAppender">
        <file>nsd.log</file>
        <append>true</append>
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg %kvp{DOUBLE}%n</pattern>
        </encoder>
    </appender>    
    <appender name="OpenTelemetry"
              class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
        <captureExperimentalAttributes>true</captureExperimentalAttributes>
        <captureKeyValuePairAttributes>true</captureKeyValuePairAttributes>
    </appender>
    <root level="INFO">
        <appender-ref ref="file"/>
        <appender-ref ref="OpenTelemetry"/>
    </root>
</configuration>
```
 
## Obtain a capability

### From a non-capability code

```java
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
CapabilityLoader capabilityLoader = new CapabilityLoader();
try {
    Requirement<Object, OpenTelemetry> requirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
    OpenTelemetry openTelemetry = capabilityLoader.loadOne(requirement, progressMonitor);
    
    ...
    
    } finally {
        parentSpan.end();
    }
} finally {             
    capabilityLoader.close(progressMonitor);
}
```

See [Capability](../capability/index.html) for more details.

### From another capability provider

If an instance of OpenTelemetry is required by another capability provider, use [CapabilityFactory.Loader](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/CapabilityFactory.Loader.html)
instead of [CapabilityLoader](https://javadoc.io/doc/org.nasdanika.core/capability/latest/org.nasdanika.capability/org/nasdanika/capability/CapabilityLoader.html) and chain capability completion stages with ``thenApply`` and ``thenCombine()``.

### thenApply()

If your capability depends just on the OpenTelemetry capability then use ``thenApply()`` as shown below:

```java
public class MyCapabilityFactory extends ServiceCapabilityFactory<MyRequirement, MyCapability> {

    @Override
    public boolean isFor(Class<?> type, Object requirement) {
        return MyCapability.class == type && (requirement == null || requirement instanceof MyRequirement);
    }

    @Override
    protected CompletionStage<Iterable<CapabilityProvider<MyCapability>>> createService(
            Class<MyCapability> serviceType, 
            MyRequirement requirement, 
            Loader loader,
            ProgressMonitor progressMonitor) {
        
        Requirement<Object, OpenTelemetry> openTelemetryRequirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
        CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(openTelemetryRequirement, progressMonitor);      
        return wrapCompletionStage(openTelemetryCS.thenApply(openTelemetry -> createMyCapability(openTelemetry, requirement)));
    }
    
    protected MyCapability createMyCapability(OpenTelemetry openTelemetry, MyRequirement requirement) {
        return new MyCapabilityImpl(openTelemetry, requirement);
    }

}
``` 

### thenCombine()

If your capability depends on the OpenTelemetry capability and other capabilities, then use ``thenCombine()`` as shown below:


```java
public class MyCapabilityFactory extends ServiceCapabilityFactory<Void, MyCapability> {

    @Override
    public boolean isFor(Class<?> type, Object requirement) {
        return MyCapability.class == type && requirement == null;
    }

    @Override
    protected CompletionStage<Iterable<CapabilityProvider<MyCapability>>> createService(
            Class<MyCapability> serviceType,
            Void serviceRequirement, 
            Loader loader, 
            ProgressMonitor progressMonitor) {
        
        
        Requirement<Object, OpenTelemetry> openTelemetryRequirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
        CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(openTelemetryRequirement, progressMonitor);
        
        Requirement<String, OpenAIClientBuilder> openAIClientBuilderRequirement = ServiceCapabilityFactory.createRequirement(
                OpenAIClientBuilder.class,
                null,
                "https://api.openai.com/v1/");
        
        CompletionStage<OpenAIClientBuilder> openAIClientBuilderCS = loader.loadOne(openAIClientBuilderRequirement, progressMonitor);
        
        return wrapCompletionStage(openAIClientBuilderCS.thenCombine(openTelemetryCS, this::createEmbeddings));
    }
        
    protected MyCapability createMyCapability(OpenAIClientBuilder openAIClientBuilder, OpenTelemetry openTelemetry) {
        return new MyCapabilityImpl(openAIClientBuilder, openTelemetry);
    }
    
}
```
