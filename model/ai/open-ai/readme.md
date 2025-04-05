This module provides implementations of core capabilities on top of 
[Azure OpenAI client library for Java](https://learn.microsoft.com/en-us/java/api/overview/azure/ai-openai-readme?view=azure-java-preview)
with integrated telemetry.

However, it doesn't register capability factories - it has to be done in (CLI) assemblies.

Below are examples of how to register capabilities.

## Embeddings

### Capability factory

```java
import java.util.concurrent.CompletionStage;

import org.nasdanika.ai.Embeddings;
import org.nasdanika.ai.openai.OpenAIEmbeddings;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;

import com.azure.ai.openai.OpenAIClientBuilder;
import com.knuddels.jtokkit.api.EncodingType;

import io.opentelemetry.api.OpenTelemetry;

public class OpenAIAdaEmbeddingsCapabilityFactory extends ServiceCapabilityFactory<Void, Embeddings> {

    @Override
    public boolean isFor(Class<?> type, Object requirement) {
        return Embeddings.class == type && requirement == null;
    }

    @Override
    protected CompletionStage<Iterable<CapabilityProvider<Embeddings>>> createService(
            Class<Embeddings> serviceType,
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
        
    protected Embeddings createEmbeddings(OpenAIClientBuilder openAIClientBuilder, OpenTelemetry openTelemetry) {
        return new OpenAIEmbeddings(
                openAIClientBuilder.buildClient(),
                openAIClientBuilder.buildAsyncClient(),
                "OpenAI",
                "text-embedding-ada-002",
                null,
                1536,
                EncodingType.CL100K_BASE,
                8191,
                openTelemetry);
    }
    
}
```

You can modify the above code to get configuration values from a requirement object, change the model, endpoint, ... 

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

import com.azure.ai.openai.OpenAIClientBuilder;

import io.opentelemetry.api.OpenTelemetry;

public class OpenAIGpt35TurboChatCapabilityFactory extends ServiceCapabilityFactory<Void, Chat> {

    @Override
    public boolean isFor(Class<?> type, Object requirement) {
        return Chat.class == type && requirement == null;
    }

    @Override
    protected CompletionStage<Iterable<CapabilityProvider<Chat>>> createService(
            Class<Chat> serviceType,
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
        
        return wrapCompletionStage(openAIClientBuilderCS.thenCombine(openTelemetryCS, this::createChat));
    }
        
    protected Chat createChat(OpenAIClientBuilder openAIClientBuilder, OpenTelemetry openTelemetry) {
        return new OpenAIChat(
            openAIClientBuilder.buildClient(),
            "OpenAI",
            "gpt-3.5-turbo",
            null,
            16385,
            4096,
            openTelemetry);
    }   

}
```

You can modify the above code to get configuration values from a requirement object, change the endpoint, ... 

### module-info.java

```java
module <module name> {
    
    ...
    
    provides CapabilityFactory with OpenAIGpt35TurboChatCapabilityFactory;
    
}
```    
