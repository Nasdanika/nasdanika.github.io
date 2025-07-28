This module provides implementations of core capabilities on top of 
[Azure OpenAI client library for Java](https://learn.microsoft.com/en-us/java/api/overview/azure/ai-openai-readme?view=azure-java-preview)
with integrated telemetry.

However, it doesn't register capability factories - it has to be done in (CLI) assemblies.

* [Sources](https://github.com/Nasdanika/ai/tree/main/openai)
* [Maven Central](https://central.sonatype.com/artifact/org.nasdanika.ai/openai)
* [JavaDoc](https://javadoc.io/doc/org.nasdanika.ai/openai/)

Below are examples of how to register capabilities[^sources].

[^sources]: [Sources](https://github.com/Nasdanika/ai/tree/main/tests/src/main/java/org/nasdanika/ai/tests)

## Embeddings

### Capability factory

```java
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

import org.nasdanika.ai.Embeddings;
import org.nasdanika.ai.openai.OpenAIEmbeddings;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Util;

import com.azure.ai.openai.OpenAIClientBuilder;
import com.knuddels.jtokkit.api.EncodingType;

import io.opentelemetry.api.OpenTelemetry;

public class OpenAIAdaEmbeddingsCapabilityFactory extends ServiceCapabilityFactory<Embeddings.Requirement, Embeddings> {

    private static final String MODEL = "text-embedding-ada-002";
    private static final String PROVIDER = "OpenAI";

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
        
        Requirement<String, OpenAIClientBuilder> openAIClientBuilderRequirement = ServiceCapabilityFactory.createRequirement(
                OpenAIClientBuilder.class,
                null,
                "https://api.openai.com/v1/");
        
        CompletionStage<OpenAIClientBuilder> openAIClientBuilderCS = loader.loadOne(openAIClientBuilderRequirement, progressMonitor);
        
        int chunkSize = serviceRequirement == null ? 0 : serviceRequirement.chunkSize();
        int overlap = serviceRequirement == null ? 0 : serviceRequirement.overlap();
        
        BiFunction<OpenAIClientBuilder, OpenTelemetry, Embeddings> combiner = (openAIClientBuilder, openTelemetry) -> createEmbeddings(openAIClientBuilder, openTelemetry, chunkSize, overlap);
        return wrapCompletionStage(openAIClientBuilderCS.thenCombine(openTelemetryCS, combiner));
    }
        
    protected Embeddings createEmbeddings(
            OpenAIClientBuilder openAIClientBuilder, 
            OpenTelemetry openTelemetry,
            int chunkSize,
            int overlap) {
        return new OpenAIEmbeddings(
                openAIClientBuilder.buildClient(),
                openAIClientBuilder.buildAsyncClient(),
                PROVIDER,
                MODEL,
                null,
                1536,
                EncodingType.CL100K_BASE,
                8191,
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
    
    provides CapabilityFactory with OpenAIAdaEmbeddingsCapabilityFactory;
    
}
```    

## Chat

### Capability factory

```java

import java.util.concurrent.CompletionStage;

import org.nasdanika.ai.Chat;
import org.nasdanika.ai.openai.OpenAIChat;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Util;

import com.azure.ai.openai.OpenAIClientBuilder;

import io.opentelemetry.api.OpenTelemetry;

public class OpenAIGpt35TurboChatCapabilityFactory extends ServiceCapabilityFactory<Chat.Requirement, Chat> {

    private static final String MODEL = "gpt-3.5-turbo";
    private static final String PROVIDER = "OpenAI";

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
        
        Requirement<String, OpenAIClientBuilder> openAIClientBuilderRequirement = ServiceCapabilityFactory.createRequirement(
                OpenAIClientBuilder.class,
                null,
                "https://api.openai.com/v1/");
        
        CompletionStage<OpenAIClientBuilder> openAIClientBuilderCS = loader.loadOne(openAIClientBuilderRequirement, progressMonitor);
        
        return wrapCompletionStage(openAIClientBuilderCS.thenCombine(openTelemetryCS, this::createChat));
    }
        
    protected Chat createChat(OpenAIClientBuilder openAIClientBuilder, OpenTelemetry openTelemetry) {
        return new OpenAIChat(
            openAIClientBuilder.buildClient(),
            openAIClientBuilder.buildAsyncClient(),
            PROVIDER,
            MODEL,
            null,
            16385,
            4096,
            openTelemetry);
    }   

}
```

### module-info.java

```java
module <module name> {
    
    ...
    
    provides CapabilityFactory with OpenAIGpt35TurboChatCapabilityFactory;
    
}
```    
