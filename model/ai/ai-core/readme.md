This module provides AI interfaces implemented by provider modules such as [OpenAI](../open-ai/index.html) and [Ollama](../ollama/index.html).

* [Sources](https://github.com/Nasdanika/ai/tree/main/core)
* [Javadoc](https://javadoc.io/doc/org.nasdanika.ai/core)

Sample client code, see [test sources](https://github.com/Nasdanika/ai/blob/main/tests/src/test/java/org/nasdanika/ai/openai/tests/tests/TestAI.java) for
more examples:

## Embeddings

### Synchronous

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
try {
    Requirement<Void, org.nasdanika.ai.Embeddings> requirement = ServiceCapabilityFactory.createRequirement(org.nasdanika.ai.Embeddings.class);         
    org.nasdanika.ai.Embeddings embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
    
    for(List<Float> vector: embeddings.generate("Hello world!")) {
        System.out.println(vector.size());
    }
} finally {
    capabilityLoader.close(progressMonitor);
}
```

#### With telemetry

##### All providers and models

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();             
try {
    OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
    assertNotNull(openTelemetry);           
    
    Requirement<Embeddings.Requirement, Embeddings> requirement = ServiceCapabilityFactory.createRequirement(Embeddings.class);         
    Iterable<CapabilityProvider<Embeddings>> embeddingsProviders = capabilityLoader.load(requirement, progressMonitor);
    List<Embeddings> allEmbeddings = new ArrayList<>();
    embeddingsProviders.forEach(ep -> ep.getPublisher().subscribe(allEmbeddings::add));
    for (Embeddings embeddings: allEmbeddings) {                
        assertNotNull(embeddings);
        System.out.println("=== Embeddings ===");
        System.out.println("Name:\t" + embeddings.getName());
        System.out.println("Provider:\t" + embeddings.getProvider());
        System.out.println("Max input:\t" + embeddings.getMaxInputTokens());
        System.out.println("Dimensions:\t" + embeddings.getDimensions());
                
        Tracer tracer = openTelemetry.getTracer("test.ai");        
        Span span = tracer
            .spanBuilder("Embeddings")
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            Thread.sleep(200);
            for (Entry<String, List<List<Float>>> vectors: embeddings.generate(List.of("Hello world!", "Hello universe!")).entrySet()) {        
                System.out.println("\t" + vectors.getKey());
                for (List<Float> vector: vectors.getValue()) {
                    System.out.println("\t\t" + vector.size());
                }
            }
        } finally {
            span.end();
        }
    }
} finally {
    capabilityLoader.close(progressMonitor);
}
```

##### A specific provider 

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();             
try {
    OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
    assertNotNull(openTelemetry);           
    
    Embeddings.Requirement eReq = new Embeddings.Requirement("Ollama", null, null, 0, 0);
    Requirement<Embeddings.Requirement, Embeddings> requirement = ServiceCapabilityFactory.createRequirement(Embeddings.class, null, eReq);         
    Iterable<CapabilityProvider<Embeddings>> embeddingsProviders = capabilityLoader.load(requirement, progressMonitor);
    List<Embeddings> allEmbeddings = new ArrayList<>();
    embeddingsProviders.forEach(ep -> ep.getPublisher().subscribe(allEmbeddings::add));
    for (Embeddings embeddings: allEmbeddings) {                
        assertNotNull(embeddings);
        System.out.println("=== Embeddings ===");
        System.out.println("Name:\t" + embeddings.getName());
        System.out.println("Provider:\t" + embeddings.getProvider());
        System.out.println("Max input:\t" + embeddings.getMaxInputTokens());
        System.out.println("Dimensions:\t" + embeddings.getDimensions());
                
        Tracer tracer = openTelemetry.getTracer("test.ai");        
        Span span = tracer
            .spanBuilder("Embeddings")
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            Thread.sleep(200);
            for (Entry<String, List<List<Float>>> vectors: embeddings.generate(List.of("Hello world!", "Hello universe!")).entrySet()) {        
                System.out.println("\t" + vectors.getKey());
                for (List<Float> vector: vectors.getValue()) {
                    System.out.println("\t\t" + vector.size());
                }
            }
            span.setStatus(StatusCode.OK);
        } finally {
            span.end();
        }
    }
} finally {
    capabilityLoader.close(progressMonitor);
}
```


### Asynchronous

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
try {
    Requirement<Embeddings.Requirement, Embeddings> requirement = ServiceCapabilityFactory.createRequirement(Embeddings.class);         
    Embeddings embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
    
    List<List<Float>> vectors = embeddings
        .generateAsync("Hello world!")
        .contextWrite(reactor.util.context.Context.of(Context.class, Context.current().with(span)))
        .block();

    for (List<Float> vector: vectors) {
        System.out.println(vector.size());
    }
} finally {
    capabilityLoader.close(progressMonitor);
}
```

#### With telemetry

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
try {
    Requirement<Embeddings.Requirement, Embeddings> requirement = ServiceCapabilityFactory.createRequirement(Embeddings.class);         
    Embeddings embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
    
    OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);

    Tracer tracer = openTelemetry.getTracer("test.ai");        
    Span span = tracer
        .spanBuilder("Embeddings")
        .startSpan();
    
    List<List<Float>> vectors = embeddings
        .generateAsync("Hello world!")
        .contextWrite(reactor.util.context.Context.of(Context.class, Context.current().with(span)))
        .doFinally(signal -> span.end())
        .block();

    for (List<Float> vector: vectors) {
        System.out.println(vector.size());
    }
} finally {
    capabilityLoader.close(progressMonitor);
}
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

### With telemetery

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
