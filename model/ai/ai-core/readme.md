This module provides AI interfaces implemented by provider modules such as [OpenAI](../open-ai/index.html) and [Ollama](../ollama/index.html).
It also provides classes and interfaces for building similarity search and RAG solutions.

* [Sources](https://github.com/Nasdanika/ai/tree/main/core)
* [Javadoc](https://javadoc.io/doc/org.nasdanika.ai/core)

See [test sources](https://github.com/Nasdanika/ai/blob/main/tests/src/test/java/org/nasdanika/ai/openai/tests/tests/TestAI.java) for examples of using Nasdanika AI classes.

[TOC levels=6]

## Embeddings

[Embeddings](https://github.com/Nasdanika/ai/blob/main/core/src/main/java/org/nasdanika/ai/Embeddings.java) interface allows to generate one or more vectors
per string. 
This follows the structure of Ollama REST API.
This approach allows transparent chunking, which is provided by [ChunkingEmbeddings](https://github.com/Nasdanika/ai/blob/main/core/src/main/java/org/nasdanika/ai/ChunkingEmbeddings.java) class and its subclasses,
[EncodingChunkingEmbeddings](https://github.com/Nasdanika/ai/blob/main/core/src/main/java/org/nasdanika/ai/EncodingChunkingEmbeddings.java) in particular.

### Generating

#### Synchronous

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

##### With telemetry

###### All providers and models

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

###### A specific provider 

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

#### Asynchronous

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

##### With telemetry

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

### Chunking

Chunking embeddings for [``text-embedding-ada-002``](https://platform.openai.com/docs/guides/embeddings#embedding-models) using ``CL100K_BASE`` encoding.
``1000`` tokens chunks with ``20`` tokens overlap.

```java
ChunkingEmbeddings<?> chunkingEmbeddings = new EncodingChunkingEmbeddings(
        embeddings, 
        1000, 
        20, 
        EncodingType.CL100K_BASE);
```

### Embeddings resource

[EmbeddingsResource](https://github.com/Nasdanika/ai/blob/main/core/src/main/java/org/nasdanika/ai/EmbeddingsResource.java) provides content with
pre-computed embeddings.
It is modeled after [McpSchema.Resource](https://javadoc.io/doc/io.modelcontextprotocol.sdk/mcp/latest/io.modelcontextprotocol.sdk.mcp/io/modelcontextprotocol/spec/McpSchema.Resource.html) (see also [MCP Resources](https://modelcontextprotocol.io/docs/concepts/resources)) to make it easy to wrap an embedding resources into an MCP resource and vice versa.

The idea is to publish text with embeddings and expose the data as an MCP resource and embeddings resource so it can be used by MCP clients, MCP tools, and there is no need to compute embeddings.

Example: [search-documents-embeddings.json](https://docs.nasdanika.org/search-documents-embeddings.json) contains plain text for the pages of this site with
pre-computed Ada embeddings of page chunks.

## Similarity search

[SimilaritySearch](https://github.com/Nasdanika/ai/blob/main/core/src/main/java/org/nasdanika/ai/SimilaritySearch.java) interface is intended to be used for finding items similar to a query using one of ``find`` methods:

* ``List<SearchResult<D>> find(U query, int numberOfItems)``
* ``Mono<List<SearchResult<D>>> findAsync(U query, int numberOfItems)``

Similarity search can work on any data type with a distance defined. 
The distance type must be ``Comparable``.
For example, for text distance can be computed using embeddings, a bag of words, a semantic graph, or a combination of thereof. 

Semantic graphs may be useful with internal terminology. 
Let's say there is a ``GBS`` system which uses IBM MQ for communication with payload structure defined as ``TVT`` and encoded to bytes using ``XLF``.
The semantic/knowledge graph would "know" that ``GBS`` stands for "Global Booking System" and it is related to ``TVT``, ``MQ``, and ``XLF``[^arch-as-code].

[^arch-as-code]: See [Connecting the dots](https://medium.com/nasdanika/connecting-the-dots-94a733c61059) and [Architecture as code](https://medium.com/nasdanika/architecture-as-code-7c0eadfc0b2b) stories for more details.

An instance of similarity search can be adapted to another type using ``adapt(Function<U,T> mapper, Function<U, Mono<T>> asyncMapper)`` method.
One usage scenario is to adapt a structured type to text by "narrating" it. 
For example, there is a [Drawio](../../core/drawio/index.html) diagram element or a [C4 Model](https://architecture.models.nasdanika.org/references/eSubpackages/c4/index.html) element, say [API Application](https://nasdanika-demos.github.io/internet-banking-system-c4/cerulean/references/elements/internet-banking-system/references/elements/api-application/index.html).

In the case of a diagram element, it can be converted to text by explaining its label, tooltip, layer it belongs to, and other elements it connects to.
The narration may also include styling such as color and geometry. E.g. "above", "to the right of".

In the case of a model element the narration would include element documentation, its references, and its type. 
E.g.:

* The "API Application" is a [Container](https://architecture.models.nasdanika.org/references/eSubpackages/c4/references/eClassifiers/Container/index.html).
* [Paul](https://nasdanika-demos.github.io/family-semantic-mapping/references/members/paul/index.html) is a [Man](https://family.models.nasdanika.org/references/eClassifiers/Man/index.html).

Static ``embeddingSearch()`` method adapts a float multi-vector search to string (text) search.
There is a static method to adapt a single vector search to a multi-vector search.

The below code snippet shows how to create a vector search instance on top of [Hnswlib](https://github.com/jelmerk/hnswlib):

```java
HnswIndex<IndexId, float[], EmbeddingsItem, Float> hnswIndex = HnswIndex
    .newBuilder(1536, DistanceFunctions.FLOAT_COSINE_DISTANCE, resources.size())
    .withM(16)
    .withEf(200)
    .withEfConstruction(200)
    .build();

Map<String, String> contentMap = new HashMap<>();

resourceSet.getResources().subscribe(er -> {
    List<List<Float>> vectors = er.getEmbeddings();
    for (int i = 0; i < vectors.size(); ++i) {
        List<Float> vector = vectors.get(i);
        float[] fVector = new float[vector.size()];
        for (int j = 0; j < fVector.length; ++j) {
            fVector[j] = vector.get(j);
        }
        hnswIndex.add(new EmbeddingsItem(
                new IndexId(er.getUri(), i), 
                fVector, 
                er.getDimensions()));               
    }
    contentMap.put(er.getUri(), er.getContent());
});

hnswIndex.save(new File("test-data/hnsw-index.bin"));

SimilaritySearch<List<Float>, Float> vectorSearch = new SimilaritySearch<List<Float>, Float>() {
    
    @Override
    public Mono<List<SearchResult<Float>>> findAsync(List<Float> query, int numberOfItems) {
        return Mono.just(find(query, numberOfItems));
    }
    
    @Override
    public List<SearchResult<Float>> find(List<Float> query, int numberOfItems) {
        float[] fVector = new float[query.size()];
        for (int j = 0; j < fVector.length; ++j) {
            fVector[j] = query.get(j);
        }
        List<SearchResult<Float>> ret = new ArrayList<>();
        for (com.github.jelmerk.hnswlib.core.SearchResult<EmbeddingsItem, Float> nearest: hnswIndex.findNearest(fVector, numberOfItems)) {
            ret.add(new SearchResult<Float>() {
                
                @Override
                public String getUri() {
                    return nearest.item().id().uri();
                }
                
                @Override
                public int getIndex() {
                    return nearest.item().id().index();
                }
                
                @Override
                public Float getDistance() {
                    return nearest.distance();
                }
                
            });
        }
        return ret;
    }
    
};      

```

An instance of vector search can be adapted to a multi-vector search:

```java
SimilaritySearch<List<List<Float>>, Float> multiVectorSearch = SimilaritySearch.adapt(vectorSearch);
```

and then to a text search: 

```java
ChunkingEmbeddings<?> chunkingEmbeddings = new EncodingChunkingEmbeddings(
        embeddings, 
        1000, 
        20, 
        EncodingType.CL100K_BASE);

SimilaritySearch<String, Float> textSearch = SimilaritySearch.embeddingsSearch(multiVectorSearch, chunkingEmbeddings);
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

### Retrieval-augmented generation (RAG)

The below code snippet shows how chat can be used with the text search for [retrieval-augmented generation](https://en.wikipedia.org/wiki/Retrieval-augmented_generation):

```java
String query = ...
List<SearchResult<Float>> searchResults = textSearch.find(query, 10);
  
// Chat
Chat.Requirement cReq = new Chat.Requirement("OpenAI", "gpt-4o", null);
Requirement<Chat.Requirement, Chat> chatRequirement = ServiceCapabilityFactory.createRequirement(Chat.class, null, cReq);
Chat chat = capabilityLoader.loadOne(chatRequirement, progressMonitor);

List<Chat.Message> messages = new ArrayList<>();
messages.add(Chat.Role.system.createMessage("You are a helpful assistant. You will answer user question leveraging provided documents and provide references to the used documents. Output your answer in markdown"));
messages.add(Chat.Role.user.createMessage(query));

Map<String, List<SearchResult<Float>>> groupedResults = org.nasdanika.common.Util.groupBy(searchResults, SearchResult::getUri);
for (Entry<String, List<SearchResult<Float>>> sre: groupedResults.entrySet()) {
    StringBuilder messageBuilder = new StringBuilder("Use this document with URL " + sre.getKey() + ":" + System.lineSeparator());
    List<String> chunks = chunkingEmbeddings.chunk(contentMap.get(sre.getKey()));
    for (SearchResult<Float> chunkResult: sre.getValue()) {
        String chunk = chunks.get(chunkResult.getIndex());
        messageBuilder.append(System.lineSeparator() + System.lineSeparator() + chunk);
    }
    
    messages.add(Chat.Role.system.createMessage(messageBuilder.toString()));
}       

List<ResponseMessage> responses = chat.chat(messages);                              

for (ResponseMessage response: responses) {
    System.out.println(response.getContent());
}               
```

Please note that this is a very basic implementation:

* It doesn't take the size of the context window into account and doesn't count input tokens.
* It uses all search results regardless of the distance. A more robust implementation would discard results with the distance greater than some threshold and perhaps would reply "Not enough information" if there are no good matches.

Also the example above is a single-shot - ask a question, get an answer, it is not a dialog.


