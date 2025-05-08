This module provides building blocks for command line AI solutions - [argument groups](https://picocli.info/#_argument_groups) and base commands.

* [Sources](https://github.com/Nasdanika/ai/tree/main/cli)
* [Javadoc](https://javadoc.io/doc/org.nasdanika.ai/cli)


## Argument groups

* [ChunkingEmbeddingsArgGroup](https://github.com/Nasdanika/ai/blob/main/cli/src/main/java/org/nasdanika/ai/cli/ChunkingEmbeddingsArgGroup.java) - configures chunking for embeddings generation. Options:
    * Chunk size
    * Chunk overlap
* [EmbeddingsArgGroup](https://github.com/Nasdanika/ai/blob/main/cli/src/main/java/org/nasdanika/ai/cli/EmbeddingsArgGroup.java) - configures embeddings requirement. Options:
    * Provider
    * Model
    * Version    
* [EncodingChunkingEmbeddingsArgGroup](https://github.com/Nasdanika/ai/blob/main/cli/src/main/java/org/nasdanika/ai/cli/EncodingChunkingEmbeddingsArgGroup.java) - extends ``ChunkingEmbeddingsArgGroup``. Option:
    * Chunk encoding type
* [HnswIndexBuilderArgGroup](https://github.com/Nasdanika/ai/blob/main/cli/src/main/java/org/nasdanika/ai/cli/HnswIndexBuilderArgGroup.java) - abstract base class for argument groups configuring [HnswIndex.Builder](https://javadoc.io/doc/com.github.jelmerk/hnswlib-core/latest/com/github/jelmerk/hnswlib/core/hnsw/HnswIndex.Builder.html). Options:
    * Ef
    * Ef-construction
    * M
    * Remove enabled
    * Threads
    * Progress update interval
* [HnswIndexBuilderFloatArgGroup](https://github.com/Nasdanika/ai/blob/main/cli/src/main/java/org/nasdanika/ai/cli/HnswIndexBuilderFloatArgGroup.java) - concrete extension of ``HnswIndexBuilderArgGroup``. Options:
    * Distance function - a choice of 19 distance functions with ``COSINE`` being the default
    * Normalize
           
## Commands

* [HnswIndexCommandBase](https://github.com/Nasdanika/ai/blob/main/cli/src/main/java/org/nasdanika/ai/cli/HnswIndexCommandBase.java) - base class for commands generating [HNSW](https://en.wikipedia.org/wiki/Hierarchical_navigable_small_world) indices. It uses the above argument groups. [PdfIndexerCommand](https://github.com/Nasdanika-Demos/cli/blob/main/src/main/java/org/nasdanika/launcher/demo/ai/PdfIndexerCommand.java) is a subclass of ``HnswIndexCommandBase`` which generated indices for PDF files. See also [PDF Indexer Documentation](https://nasdanika-demos.github.io/cli/pdf-indexer/index.html).          

