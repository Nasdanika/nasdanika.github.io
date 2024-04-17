
This practice is a specialization of the [Analysis, Visualization & Generation Practice](../generic/index.html) for using the [Java model](https://java.models.nasdanika.org/) as a source model, target model, or both. 
This page provides a high level reference and [the book](https://leanpub.com/java-analysis) goes into details. 

So what is possible to do with the Java model/language in addition to generic analysis, visualization and generation?


## Analysis

* Java model can be loaded from sources and bytecode.
* Tests [coverage](https://coverage.models.nasdanika.org/) can be loaded from ``jacoco.exec`` and class files and associated with model elements.
* Bytecode information can be used to establish bi-directional references between model elements - field access, method calls.
* Bytecode can be instrumented to collect runtime cross-referencing such as reflective method calls and field access.

## Visualization

* Module, package, class, method dependency graphs. The graphs may reflect coverage data so they can be used for prioritization of addressing technical debt. For example, many well-covered microservices may use a shared library with low coverage. 
* Sequence diagrams

## Generation

### Documentation

Documentation similar to documentation generated from Ecore models such as Java model above, [Family model](https://family.models.nasdanika.org/), or [Enterprise model](https://enterprise.models.nasdanika.org/) with:

    * Visualizations mentioned above
    * Documentation produced by GenAI - explainations and recommendations.

Such documentation may be useful in modernization efforts where there is a need to understand a legacy codebase. 
It may also be useful in onboarding of new team members and it might help provide deeper insights into the codebase for all team members.
    
### Source code
    
Source code with [``@Generated``](https://javadoc.io/doc/org.nasdanika.core/common/latest/org.nasdanika.common/org/nasdanika/common/Generated.html) annotations or ``@generated`` Javadoc tags to allow detection of changes in the generated code and re-generation only if there changes in the generator inputs, and the output was not modified since the last generation. It allows concurrent evolution of the generator, generator inputs, and manual modifications. 
For more details see [Solution instantiation](https://cv.pavel.vlasov.us/cover-letter.html#solution-instantiation).  

### RAG/Chat

[RAG](https://rag.nasdanika.org/)/Chat on top of the Java model may use bytecode and runtime introspection information in addition to just source code. For example "This method is overridden by ... and is called by ...". 
RAG may be contextual - chat with a class, a method, a package, a module or an application (group of modules) if the model elements are "mounted" under higher level constructs such as products and segments.
