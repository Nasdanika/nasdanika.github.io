This module provides AI interfaces implemented by provider modules such as [OpenAI](../open-ai/index.html) and [Ollama](../ollama/index.html).

[Javadoc](https://javadoc.io/doc/org.nasdanika.ai/core/latest/org.nasdanika.ai/module-summary.html)

Sample client code:

## Embeddings

### Synchronous

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
try {
    Requirement<Void, org.nasdanika.ai.Embeddings> requirement = ServiceCapabilityFactory.createRequirement(org.nasdanika.ai.Embeddings.class);         
    org.nasdanika.ai.Embeddings embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
    
    List<Float> vector = embeddings.generate("Hello world!");
    System.out.println(vector.size());
} finally {
    capabilityLoader.close(progressMonitor);
}
```

#### With telemetry

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
try {
    Requirement<Void, org.nasdanika.ai.Embeddings> requirement = ServiceCapabilityFactory.createRequirement(org.nasdanika.ai.Embeddings.class);         
    org.nasdanika.ai.Embeddings embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
    
    OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
    Tracer tracer = openTelemetry.getTracer("test.openai");        
    Span span = tracer
        .spanBuilder("Embeddings")
        .startSpan();
    
    try (Scope scope = span.makeCurrent()) {
        List<Float> vector = embeddings.generate("Hello world!");
        System.out.println(vector.size());
    } finally {
        span.end();
    }
} finally {
    capabilityLoader.close(progressMonitor);
}
```

### Asynchronous

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();

Requirement<Void, org.nasdanika.ai.Embeddings> requirement = ServiceCapabilityFactory.createRequirement(org.nasdanika.ai.Embeddings.class);         
org.nasdanika.ai.Embeddings embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
    
embeddings
    .generateAsync("Hello world!")
    .subscribe(vector -> System.out.println(vector.size()));
```

#### With telemetry

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();

Requirement<Void, org.nasdanika.ai.Embeddings> requirement = ServiceCapabilityFactory.createRequirement(org.nasdanika.ai.Embeddings.class);         
org.nasdanika.ai.Embeddings embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
    
OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);

Tracer tracer = openTelemetry.getTracer("test.openai");        
Span span = tracer
    .spanBuilder("Embeddings")
    .startSpan();
    
embeddings
    .generateAsync("Hello world!")
    .contextWrite(reactor.util.context.Context.of(Context.class, Context.current().with(span)))
    .doFinally(signal -> span.end())
    .subscribe(vector -> System.out.println(vector.size()));
```

## Chat

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
try {
    Requirement<Void, Chat> requirement = ServiceCapabilityFactory.createRequirement(Chat.class);           
    org.nasdanika.ai.Chat chat = capabilityLoader.loadOne(requirement, progressMonitor);
    
    List<ResponseMessage> responses = chat.chat(
        Chat.Role.system.createMessage("You are a helpful assistant. You will talk like a pirate."),
        Chat.Role.user.createMessage("Can you help me?"),
        Chat.Role.system.createMessage("Of course, me hearty! What can I do for ye?"),
        Chat.Role.user.createMessage("What's the best way to train a parrot?")
    );
        
    for (ResponseMessage response: responses) {
        System.out.println(response.getContent());
    }
} finally {
    capabilityLoader.close(progressMonitor);
}
```

### With telemtery

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
try {
    Requirement<Void, Chat> requirement = ServiceCapabilityFactory.createRequirement(Chat.class);           
    org.nasdanika.ai.Chat chat = capabilityLoader.loadOne(requirement, progressMonitor);
    
    OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);

    Tracer tracer = openTelemetry.getTracer("test.openai");        
    Span span = tracer
        .spanBuilder("Chat")
        .startSpan();
    
    try (Scope scope = span.makeCurrent()) {
        List<ResponseMessage> responses = chat.chat(
            Chat.Role.system.createMessage("You are a helpful assistant. You will talk like a pirate."),
            Chat.Role.user.createMessage("Can you help me?"),
            Chat.Role.system.createMessage("Of course, me hearty! What can I do for ye?"),
            Chat.Role.user.createMessage("What's the best way to train a parrot?")
        );
        
        for (ResponseMessage response: responses) {
            System.out.println(response.getContent());
        }
    } finally {
        span.end();
    }
} finally {
    capabilityLoader.close(progressMonitor);
}
```
