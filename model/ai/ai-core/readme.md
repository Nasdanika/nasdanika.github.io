This module provides AI interfaces implemented by provider modules such as [OpenAI](../open-ai/index.html) and [Ollama](../ollama/index.html).
It also provides classes and interfaces for building similarity search and RAG solutions.

* [Sources](https://github.com/Nasdanika/ai/tree/main/core)
* [Maven Central](https://central.sonatype.com/artifact/org.nasdanika.ai/core)
* [Javadoc](https://javadoc.io/doc/org.nasdanika.ai/core)

See [test sources](https://github.com/Nasdanika/ai/blob/main/tests/src/test/java/org/nasdanika/ai/openai/tests/tests/TestAI.java) for examples of using Nasdanika AI classes.

[TOC levels=6]

## Embeddings

In [machine learning](https://en.wikipedia.org/wiki/Machine_learning) <a href="https://en.wikipedia.org/wiki/Embedding_(machine_learning)">embedding</a> is

> a [representation learning](https://en.wikipedia.org/wiki/Feature_learning) technique that maps complex, high-dimensional data into a lower-dimensional [vector space](https://en.wikipedia.org/wiki/Vector_space) of numerical vectors

Here the word embedding is used in more [mathematical sense](https://en.wikipedia.org/wiki/Embedding) - [dimensionality](https://en.wikipedia.org/wiki/Dimensionality_reduction) with structure preservation.

Below are a few examples of embeddings:

* Vector embeddings of text or images
* Textual description of an image, [diagram](../ai-drawio/index.html), or another complext structure such as a [graph](../../core/graph/index.html) or [Ecore model](../ai-emf/index.html).
* Text summary

With extended definition embedding generation can be chained. 
Examples:

* image -> text -> vector
* (PDF) document, diagram or other complex structure -> text and images -> combine text with image descriptions -> vector

In this module ``EmbeddingGenerator<S,E>`` is the base interface for generating embeddings. 
Is has multiple sub-interfaces and implementations.
It has the following methods:

* ``Mono<E> generateAsync(S)`` - generates an embedding of type ``E`` from source ``S`` asynchronously/reactively. This is the only method of the interface which has to be implemented - the other methods have default implementations.
* ``E generate(S)`` - generates an embedding synchronously.
* ``Mono<Map<S,E>> generateAsync(List<S>)`` - asynchronous batch generation. For example, generation of vectors for text chunks.
* ``Map<S,E> generate(List<S>)`` - synchronous batch generation. The default implementation calls generateAsync().block(), so actual processing is asynchronous.
* ``<F> EmbeddingGenerator<S,F> then(EmbeddingGenerator<E,F> next)`` - combines two generators. For example, image narrator with text vector generator.
* ``<V> EmbeddingGenerator<V,E> adapt(Function<V,Mono<S>> mapper)`` - adapts this generator to another source type using a mapper function. For example, image embedding generator can be adapted to input stream embedding generator.

``EmbeddingGenerator`` interface also has a nested ``Requirement`` record for obtaining embedding generators using the [capability framework](../../core/capability/index.html).

``EmbeddingModel`` interface extends ``EmbeddingGenerator`` and ``Coordinates`` interfaces, 
so it has ``provider``, ``name``, and ``version`` attributes which can be used for requesting a specific model via the capability framework.

``Narrator<S>`` interface extends ``EmbeddingGenerator<S, String>``.
Specializations of this interface can be used for generating text from images, diagrams, graphs, models, ...
There might be implementations generating text from text. For example a summary or translation to another language. 

### Text

Implementations of ``TextEmbeddingGenerator`` can be used to generate text embeddings.
``TextFloatVectorEmbeddingModel`` is a specialization of ``TextEmbeddingGenerator`` for generating float vector embeddins.
There are implementations of this interfaces for Ollama and OpenAI.

``TextFloatVectorEmbeddingModel`` generates a list of vectors from a piece of text, which is provided by concrete subclasses of ``TextFloatVectorChunkingEmbeddingModel`` - ``TextFloatVectorCharChunkingEmbeddings`` and ``TextFloatVectorEncodingChunkingEmbeddingModel``.

#### Generation

##### Synchronous

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);                
try {
    Requirement<EmbeddingGenerator.Requirement, TextFloatVectorEmbeddingModel> requirement = ServiceCapabilityFactory.createRequirement(TextFloatVectorEmbeddingModel.class);           
    Iterable<CapabilityProvider<TextFloatVectorEmbeddingModel>> embeddingsProviders = capabilityLoader.load(requirement, progressMonitor);
    List<TextFloatVectorEmbeddingModel  > allEmbeddings = new ArrayList<>();
    embeddingsProviders.forEach(ep -> allEmbeddings.addAll(ep.getPublisher().collect(Collectors.toList()).block()));
    for (TextFloatVectorEmbeddingModel embeddings: allEmbeddings) {             
        for (List<Float> vector: embeddings.generate("Hello world!")) {     
            System.out.println("\t\t" + vector.size());
        }
    }
} finally {
    capabilityLoader.close(progressMonitor);
}
```

###### With telemetry

_All providers and models_

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);new LoggerProgressMonitor(LOGGER);             
try {
    OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
    
    Requirement<EmbeddingGenerator.Requirement, TextFloatVectorEmbeddingModel> requirement = ServiceCapabilityFactory.createRequirement(TextFloatVectorEmbeddingModel.class);           
    Iterable<CapabilityProvider<TextFloatVectorEmbeddingModel>> embeddingsProviders = capabilityLoader.load(requirement, progressMonitor);
    List<TextFloatVectorEmbeddingModel  > allEmbeddings = new ArrayList<>();
    embeddingsProviders.forEach(ep -> allEmbeddings.addAll(ep.getPublisher().collect(Collectors.toList()).block()));
    Tracer tracer = openTelemetry.getTracer("test.ai");        
    Span span = tracer
        .spanBuilder("Embeddings")
        .startSpan();
    
    try (Scope scope = span.makeCurrent()) {
        for (TextFloatVectorEmbeddingModel embeddings: allEmbeddings) {             
            for (List<Float> vector: embeddings.generate("Hello world!")) {     
                System.out.println("\t\t" + vector.size());
            }
        }
    } finally {
        span.end();
    }
} finally {
    capabilityLoader.close(progressMonitor);
}
```

_A specific provider_ 

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);             
try {
    OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
    
    EmbeddingGenerator.Requirement eReq = TextFloatVectorEmbeddingModel.createRequirement("Ollama", null, null);
    Requirement<EmbeddingGenerator.Requirement, TextFloatVectorEmbeddingModel> requirement = ServiceCapabilityFactory.createRequirement(TextFloatVectorEmbeddingModel.class, null, eReq);           
    Iterable<CapabilityProvider<TextFloatVectorEmbeddingModel>> embeddingsProviders = capabilityLoader.load(requirement, progressMonitor);
    List<TextFloatVectorEmbeddingModel> allEmbeddings = new ArrayList<>();
    embeddingsProviders.forEach(ep -> ep.getPublisher().subscribe(allEmbeddings::add));
    for (TextFloatVectorEmbeddingModel embeddings: allEmbeddings) {             
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

##### Asynchronous

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
try {
    Requirement<EmbeddingGenerator.Requirement, TextFloatVectorEmbeddingModel> requirement = ServiceCapabilityFactory.createRequirement(TextFloatVectorEmbeddingModel.class);           
    TextFloatVectorEmbeddingModel embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
    List<List<Float>> vectors = embeddings.generateAsync("Hello world!").block();
    for (List<Float> vector: vectors) {
        System.out.println(vector.size());
    }
} finally {
    capabilityLoader.close(progressMonitor);
}
```

###### With telemetry

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
try {
    Requirement<EmbeddingGenerator.Requirement, TextFloatVectorEmbeddingModel> requirement = ServiceCapabilityFactory.createRequirement(TextFloatVectorEmbeddingModel.class);           
    TextFloatVectorEmbeddingModel embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
    
    OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
    assertNotNull(openTelemetry);

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

#### Chunking

Chunking embeddings for [``text-embedding-ada-002``](https://platform.openai.com/docs/guides/embeddings#embedding-models) using ``CL100K_BASE`` encoding.
``1000`` tokens chunks with ``20`` tokens overlap.

```java
TextFloatVectorEncodingChunkingEmbeddingModel chunkingEmbeddings = new TextFloatVectorEncodingChunkingEmbeddingModel(
        embeddings, 
        1000, 
        20, 
        EncodingType.CL100K_BASE);
```

#### Embeddings resource

``TextFloatVectorEmbeddingResource`` provides content with pre-computed embeddings.
It is modeled after [McpSchema.Resource](https://javadoc.io/doc/io.modelcontextprotocol.sdk/mcp/latest/io.modelcontextprotocol.sdk.mcp/io/modelcontextprotocol/spec/McpSchema.Resource.html) (see also [MCP Resources](https://modelcontextprotocol.io/docs/concepts/resources)) to make it easy to wrap an embedding resources into an MCP resource and vice versa.

The idea is to publish text with embeddings and expose the data as an MCP resource and embeddings resource so it can be used by MCP clients, MCP tools, and there is no need to compute embeddings.

Example: [search-documents-embeddings.json](https://docs.nasdanika.org/search-documents-embeddings.json) contains plain text for the pages of this site with
pre-computed Ada embeddings of page chunks.

### Images

This module provides the following classes and interfaces for working with images:

* Interfaces
    * ``ImageEmbeddingGenerator<E> extends EmbeddingGenerator<BufferedImage, E>``
    * ``ImageNarrator extends ImageEmbeddingGenerator<String>, Narrator<BufferedImage>``
    * ``ImageFloatVectorEmbeddingModel extends EmbeddingModel<BufferedImage, List<List<Float>>>, ImageEmbeddingGenerator<List<List<Float>>>, FloatVectorEmbeddingGenerator<BufferedImage>``
* Classes
    * ``CachingImageEmbeddingGenerator<E> extends CachingEmbeddingGenerator<BufferedImage, E, String> implements ImageEmbeddingGenerator<E>``
        * ``CachingImageNarrator extends CachingImageEmbeddingGenerator<String> implements ImageNarrator``
    * ``ChatImageNarrator implements ImageNarrator`` - uses multi-modal chat (see below) to generate image descriptions, extract text, ...        
    
#### Narrate (describe) image

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);

List<Chat> chats = new ArrayList<>();       
try {
    Chat.Requirement cReq = new Chat.Requirement("OpenAI", "gpt-4o", null);
    Requirement<Chat.Requirement, Chat> requirement = ServiceCapabilityFactory.createRequirement(Chat.class, null, cReq);           
    for (CapabilityProvider<Chat> chatProvider: capabilityLoader.<Chat>load(requirement, progressMonitor)) {
        chatProvider.getPublisher().subscribe(chats::add);
    }
    
    Tracer tracer = openTelemetry.getTracer("test.ai");        
    Span span = tracer
        .spanBuilder("Chat")
        .startSpan();
    try (Scope scope = span.makeCurrent()) {            
        for (Chat chat: chats) {
            ChatImageNarrator chatImageNarrator = new ChatImageNarrator(chat);
            String narration = chatImageNarrator.asFileEmbeddingGenerator().generate(new File("llama.png"));
            System.out.println(narration);
        }
    } finally {
        span.end();
    }
} finally {
    capabilityLoader.close(progressMonitor);
}
```

### Caching

Generation of embeddigns can be an expensive operation time and money wise (token cost). 
``CachingEmbeddingGenerator`` and its subclasses can be used for caching and sharing embeddings.

One application of caching of image -> textual description caching is manual modification of descriptions for images such as icons and logos where there is much more than meets the eye.  
For example, an architectural diagram may have an element with an image from an image library representing, say, a mainframe as on [this diagram](https://nasdanika-demos.github.io/internet-banking-system/index.html). 
Or it may represent a specific enterprise system such as a payment processing mainframe. 
Having a shared "visual glossary" would allow to generate higher quality diagram descriptions.

#### CachingImageNarrator

```java
// Load cache here

ChatImageNarrator chatImageNarrator = new ChatImageNarrator(chat);
ImageNarrator cachingImageNarrator = new CachingImageNarrator(chatImageNarrator, cache);
String narration = cachingImageNarrator.asFileEmbeddingGenerator().generate(new File("llama.png"));
System.out.println(narration);

// Save cache here
```

## Similarity

One application of generating embeddings is to compute similarity between things such as text and images.

### Computing

``SimilarityComputer`` interface and its extensions and implementation provide functionality for computing similarity for text and images.

``SimilarityComputer`` has the following methods:

* ``S compute(T, T)``
* ``Mono<S> computeAsync(T, T)``
* ``Mono<S> computeAsync(Mono<T>, Mono<T>)``
* ``<V> SimilarityComputer<V,S> adapt(Function<V, Mono<T>>)`` - adapts to another input type using a mapper function


Sub-interfaces:

* ``BufferedImageSimilarityComputer`` - binds the input generic parameter to ``BufferedImage`` and provides adapter methods loading images from ``InputStream``, ``URL`` or ``File`` and then computing similarity
* ``TextSimilarityComputer`` - binds the input generic parameter to ``String``
* ``VectorSimilarityComputer<E,S> extends SimilarityComputer<List<E>, S>`` - binds the input generic parameter to a list of vector elements
    * ``FloatVectorSimilarityComputer extends VectorSimilarityComputer<Float, Float>`` - binds input element and similarity to ``Float``. Provides ``COSINE_SIMILARITY_COMPUTER`` constant
    
Implementations:

* ``CompositeFloatSimilarityComputer<T> implements SimilarityComputer<T,Float>`` - computes combined float similarity from several similarity computers. Computers are added using ``addComputer(SimilarityComputer<? super T, Float> computer, float weight)`` method. Possible application - compute image similarity using visual similarity methods, e.g. leveraging [OpenCV](https://opencv.org/) or [Deep Java Library](https://djl.ai/). Then generate text descriptions and compute similarity between them. Finally, combine visual and textual similarity with weights.    

### Search

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

In the case of a diagram element, it can be converted to text by explaining its label, tooltip, layer it belongs to, and other elements it connects to
 - this is different from computer vision because tooltips and layers are not visible.
The narration may also include styling such as color, description of a shape image and geometry. E.g. "above", "to the right of".

In the case of a model element the narration would include element documentation, its references, and its type. 

E.g.:

* The "API Application" is a [Container](https://architecture.models.nasdanika.org/references/eSubpackages/c4/references/eClassifiers/Container/index.html).
* [Paul](https://nasdanika-demos.github.io/family-semantic-mapping/references/members/paul/index.html) is a [Man](https://family.models.nasdanika.org/references/eClassifiers/Man/index.html).

Documentation may be in plain text or, for example, [Markdown](../../core/exec/index.html#markdown). 
In the latter case fenced blocks with diagrams can be narrated as explained above and images can be explained using vision models like ChatGPT.
Image alternative text can be used as well.

Static ``textFloatVectorEmbeddingSearch()`` method adapts a float multi-vector search to string (text) search.
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
TextFloatVectorChunkingEmbeddingModel chunkingEmbeddings = new TextFloatVectorChunkingEmbeddingModel(
        embeddings, 
        1000, 
        20, 
        EncodingType.CL100K_BASE);

SimilaritySearch<String, Float> textSearch = SimilaritySearch.textFloatVectorEmbeddingSearch(multiVectorSearch, chunkingEmbeddings);
```

## Chat

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
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
ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
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


