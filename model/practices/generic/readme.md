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
