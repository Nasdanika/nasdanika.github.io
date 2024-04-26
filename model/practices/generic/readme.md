This practice explains how to use Nasdanika products (specifically models) and related products.

```drawio-resource
practices/generic/transformation.drawio
```

The above diagram shows the core idea - load input data into a model, modify the model or create a new model from it, and save the models to native (raw) formats.
Loading to a model as opposed to working with raw formats gives the following benefits:

* Unified API
* Generated model documentation with visualizations
* Different models may extend classes from core models and be treated similarly
* Model classes may be subclassed and mixed 
* Cross-reference model elements
* [URI handlers](https://javadoc.io/static/org.eclipse.emf/org.eclipse.emf.ecore/2.33.0/org/eclipse/emf/ecore/resource/URIHandler.html) allows to load models from diverse sources 
* On-demand loading of resources and features of model elements
* Conversion of models to graphs and making them executable with graph processors

E.g. want to read/write Excel files - take a look at the [Excel metamodel](https://excel.models.nasdanika.org/diagram.html) and then use Ecore API to work with the model. 
Now want to work with PDF? 
A different [metamodel](https://pdf.models.nasdanika.org/diagram.html), the same model API.

You have [Java](https://java.models.nasdanika.org/) sources stored in [GitLab](https://gitlab.models.nasdanika.org/) and want model elements to reflect both Java and GitLab natures of your sources?
Create a ``GitLabRepositoryCompilationUnit`` class which extends both [Compilation Unit](https://java.models.nasdanika.org/references/eClassifiers/CompilationUnit/index.html) and [Repository File](https://gitlab.models.nasdanika.org/references/eClassifiers/RepositoryFile/index.html).
Customize [Loader](https://javadoc.io/doc/org.nasdanika.models.gitlab/model/latest/org.nasdanika.models.gitlab/org/nasdanika/models/gitlab/util/Loader.html) to create this class for repository files with ``java`` extension.

Want to load a PDF file directly from GitLab without having to clone the entire repository?
Use [GitLabURIHandler](https://javadoc.io/doc/org.nasdanika.models.gitlab/model/latest/org.nasdanika.models.gitlab/org/nasdanika/models/gitlab/util/GitLabURIHandler.html)!

The below diagram illustrates the above concepts:

```drawio-resource
practices/generic/concepts.drawio
```

Models can be visualized using:

* [ECharts](https://echarts.apache.org/en/index.html) using the [ECharts model](https://echarts.models.nasdanika.org/graph/), [ECharts-Java](https://github.com/ECharts-Java/ECharts-Java) or by directly generating JavaScript/JSON. [Example](https://architecture.models.nasdanika.org/default-graph-with-dependencies-and-subpackages.html).
* [PlantUML](https://plantuml.com/#google_vignette) using [DiagramGenerator](https://javadoc.io/static/org.nasdanika.core/common/2024.4.0/org.nasdanika.common/org/nasdanika/common/DiagramGenerator.html), the [diagram module](https://javadoc.io/doc/org.nasdanika.core/diagram/latest/org.nasdanika.diagram/module-summary.html) or by directly generating PlantUML text and calling Plant UML API's. [Example](https://architecture.models.nasdanika.org/diagram.html).


## Holistic model of an organization

One use case for the modeling approach outlined above is creation of a holistic model of an organization/corporation as exemplified by the below diagram[^corporate_structure]

```drawio-resource
practices/generic/corporate-structure.drawio
```

[^corporate_structure]: [Corporate structure](https://en.wikipedia.org/wiki/Corporate_structure)

In a corporation different elements of the model are typically stored in different systems and documents like Excel spreadsheets. 
The modeling approach allows to load those elements in a single resource set and cross-reference them. 
Elements which are not stored in structured formats can be captured by modeling them in diagrams and mapping those diagrams to models, see [Beyond Diagrams](https://leanpub.com/beyond-diagrams).

One important reason why a holistic model might be beneficial for an organization is the ability of using it for AI insights. 
For example, using [RAG](https://rag.nasdanika.org/)/Chat on top of the organization model. 
Such chat can be made context-aware, chatting with the Operations will return result relevant to operations.

The above diagram is very simple, a large organization may have many layers, thousands of applications, millions of lines of code.   
A model for such an organization would take some time to build, but it can be built incrementally - department by department, application by application. 
The value of building such a model will grow exponentially as more and more elements are added due to the [network effect](https://en.wikipedia.org/wiki/Network_effect).

While the resulting model might be "large", ... define large. 
Experiments show that a model element in a model like the above takes ~ 500 bytes of RAM. 
As such, 1 GB of RAM would hold about 2 million model elements.
Also, model resources are loaded on demand, so only the model elements needed by some task would be loaded to complete that task.
With [DynamicDelegate](https://javadoc.io/doc/org.nasdanika.core/emf/latest/org.nasdanika.emf/org/nasdanika/emf/DynamicDelegate.html) it is possible to have model elements loading their data from multiple sources on demand.

The organization model can be built on top of existing "generic" models such as [Java](https://java.models.nasdanika.org/), [Maven](https://maven.models.nasdanika.org/), [GitLab](https://gitlab.models.nasdanika.org/), [Azure](https://azure.models.nasdanika.org/diagram.html), ...

### Resources

* [TOGAF Enterprise Metamodel](https://pubs.opengroup.org/togaf-standard/introduction/chap03.html#tag_03_12_03)