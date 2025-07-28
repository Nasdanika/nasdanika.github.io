This module provides implementations of core capabilities backed by [Ollama](https://ollama.com/) over [REST API](https://www.postman.com/postman-student-programs/ollama-api/collection/suc47x8/ollama-rest-api) with integrated telemetry.

However, it doesn't register capability factories - it has to be done in (CLI) assemblies.

* [Sources](https://github.com/Nasdanika/ai/tree/main/ollama)
* [Maven Central](https://central.sonatype.com/artifact/org.nasdanika.ai/ollama)
* [JavaDoc](https://javadoc.io/doc/org.nasdanika.ai/ollama/)

Below are examples of how to register capabilities[^sources].

[^sources]: [Sources](https://github.com/Nasdanika/ai/tree/main/tests/src/main/java/org/nasdanika/ai/tests)

## Embeddings

### Capability factory

```java

import java.util.concurrent.CompletionStage;

import org.nasdanika.ai.Embeddings;
import org.nasdanika.ai.ollama.OllamaEmbeddings;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Util;

import io.opentelemetry.api.OpenTelemetry;

public class SnowflakeArcticEmbedOllamatCapabilityFactory extends ServiceCapabilityFactory<Embeddings.Requirement, Embeddings> {

    private static final String MODEL = "snowflake-arctic-embed";
    private static final String PROVIDER = "Ollama";

    @Override
    public boolean isFor(Class<?> type, Object requirement) {
        if (Embeddings.class == type) {
            if (requirement == null) {
                return true;
            }
            if (requirement instanceof Embeddings.Requirement) {            
                Embeddings.Requirement eReq = (Embeddings.Requirement) requirement;
                if (!Util.isBlank(eReq.provider()) && !PROVIDER.equals(eReq.provider())) {
                    return false;
                }
                return Util.isBlank(eReq.model()) || MODEL.equals(eReq.model());
            }
        }
        return false;
    }

    @Override
    protected CompletionStage<Iterable<CapabilityProvider<Embeddings>>> createService(
            Class<Embeddings> serviceType,
            Embeddings.Requirement serviceRequirement, 
            Loader loader, 
            ProgressMonitor progressMonitor) {
                
        Requirement<Object, OpenTelemetry> openTelemetryRequirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
        CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(openTelemetryRequirement, progressMonitor);
        
        int chunkSize = serviceRequirement == null ? 0 : serviceRequirement.chunkSize();
        int overlap = serviceRequirement == null ? 0 : serviceRequirement.overlap();
        
        return wrapCompletionStage(openTelemetryCS.thenApply(openTelemetry -> createEmbeddings(openTelemetry, chunkSize, overlap)));
    }
        
    protected Embeddings createEmbeddings(
            OpenTelemetry openTelemetry,
            int chunkSize,
            int overlap) {
        return new OllamaEmbeddings(
                "http://localhost:11434/api/", 
                PROVIDER, 
                MODEL, 
                null, 
                1024,
                null, // unknown
                8192, 
                chunkSize,
                overlap,
                openTelemetry);
    }   

}
```

### module-info.java

```java
module <module name> {
    
    ...
    
    provides CapabilityFactory with SnowflakeArcticEmbedOllamatCapabilityFactory;
    
}
```    

## Chat

### Capability factory

```java
import java.util.concurrent.CompletionStage;

import org.nasdanika.ai.Chat;
import org.nasdanika.ai.ollama.OllamaChat;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Util;

import io.opentelemetry.api.OpenTelemetry;

public class Llama32OllamaChatCapabilityFactory extends ServiceCapabilityFactory<Chat.Requirement, Chat> {

    private static final String MODEL = "llama3.2";
    private static final String PROVIDER = "Ollama";

    @Override
    public boolean isFor(Class<?> type, Object requirement) {
        if (Chat.class == type) {
            if (requirement == null) {
                return true;
            }
            if (requirement instanceof Chat.Requirement) {          
                Chat.Requirement cReq = (Chat.Requirement) requirement;
                if (!Util.isBlank(cReq.provider()) && !PROVIDER.equals(cReq.provider())) {
                    return false;
                }
                return Util.isBlank(cReq.model()) || MODEL.equals(cReq.model());
            }
        }
        return false;
    }

    @Override
    protected CompletionStage<Iterable<CapabilityProvider<Chat>>> createService(
            Class<Chat> serviceType,
            Chat.Requirement serviceRequirement, 
            Loader loader, 
            ProgressMonitor progressMonitor) {
                
        Requirement<Object, OpenTelemetry> openTelemetryRequirement = ServiceCapabilityFactory.createRequirement(OpenTelemetry.class);
        CompletionStage<OpenTelemetry> openTelemetryCS = loader.loadOne(openTelemetryRequirement, progressMonitor);
        
        return wrapCompletionStage(openTelemetryCS.thenApply(this::createChat));
    }
        
    protected Chat createChat(OpenTelemetry openTelemetry) {
        return new OllamaChat(
                "http://localhost:11434/api/", 
                PROVIDER, 
                MODEL, 
                null, 
                128000, 
                2048, 
                openTelemetry);
    }   

}
```

### module-info.java

```java
module <module name> {
    
    ...
    
    provides CapabilityFactory with Llama32OllamaChatCapabilityFactory;
    
}
```    
