# Overview

```drawio
${representations/drawio/diagram}
```

* [Core](modules/core/index.html) - foundational functionality
(modules/core/modules/common/apidocs/index.html?org/nasdanika/common/Command.html) from a command line.
    * [Common](modules/core/modules/common/index.html) - common functionality such as the execution model, resource and persistence frameworks.

        * [Model](modules/core/modules/diagram/modules/model/index.html) - Contains Diagram, DiagramElement, Node, Connection, ... classes
        * [Generators](modules/core/modules/diagram/modules/gen/index.html) - Generators from the model to different formats including PlantUML and Drawio    
    * [Drawio](modules/core/modules/drawio/index.html) - Java API for reading and writing [Drawio](https://www.diagrams.net/) files including base classes for loading of EMF models from Drawio diagrams
    * [EMF](modules/core/modules/emf/index.html) - classes for working with Ecore models, e.g. classes for loading models from YAML files.
    * [Exec](modules/core/modules/exec/index.html) - Ecore models and adapters to execution model participants to build code generators and to load model content from different sources.
        * [Model](modules/core/modules/exec/modules/model/index.html) - Ecore model containing flow control, content, and resource classes
        * [Generators](modules/core/modules/exec/modules/gen/index.html) - Adapters to execution participants    
    * [Graph](modules/core/modules/graph/index.html) - Java API for working with directed graphs, such as diagrams. Includes mapping of graph elements to Java methods and fields to make graphs excutable. Also includes a base class for loading EMF models from graphs.
    * [Ncore](modules/core/modules/ncore/index.html) - Ecore model containing common classes used/extended in other models.
    * [Resources](modules/core/modules/resources/index.html) - An abstraction layer for working with resources - units of content organized into directories, e.g. files or zip file entries. 
    * [Persistence](modules/core/modules/persistence/index.html) - A persistence framework focusing on loading data from key/value formats such as [YAML](https://en.wikipedia.org/wiki/YAML) and [JSON](https://en.wikipedia.org/wiki/JSON).
* [HTML](modules/html/index.html) - Java API's and models for generating HTML
    * [Bootstrap](modules/html/modules/bootstrap/index.html) - Fluent Java API to generate [Bootstrap 4](https://getbootstrap.com/docs/4.6/getting-started/introduction/) markup.
    * [Ecore](modules/html/modules/ecore/index.html) - Classes to generate documentation of Ecore models as action models (see Models/Application below). 
    * [EMF](modules/html/modules/emf/index.html) - Base classes for building generators of action models from Ecore model elements.
    * [HTML](modules/html/modules/html/index.html) - Fluent Java API to generate [HTML 5](https://en.wikipedia.org/wiki/HTML5) markup.
    * [JsTree](modules/html/modules/jstree/index.html) - Java API for generating [jsTree](https://www.jstree.com/) JSON.
    * [Models](modules/html/modules/models/index.html) - Ecore models to build HTML pages and applications.
        * [HTML](modules/html/modules/models/modules/html/index.html) - Ecore model representing HTML elements and generation adapters.
            * [Model](modules/html/modules/models/modules/html/modules/model/index.html) - Ecore model of HTML elements
            * [Generators](modules/html/modules/models/modules/html/modules/gen/index.html) - Generator adapters for the HTML model
        * [Bootstrap](modules/html/modules/models/modules/bootstrap/index.html) - Ecore model representing [Bootstrap 4](https://getbootstrap.com/docs/4.5/getting-started/introduction/) elements and generation adapters. 
            * [Model](modules/html/modules/models/modules/bootstrap/modules/model/index.html) - Ecore model of Bootstrap elements
            * [Generators](modules/html/modules/models/modules/bootstrap/modules/gen/index.html) - Generator adapters for the Bootstrap model
        * [Application](modules/html/modules/models/modules/app/index.html) - Ecore model representing a web site/application as a hierarchy of actions. Adapters to generate HTML sites (resource models) from action models.
            * [Model](modules/html/modules/models/modules/app/modules/model/index.html) - Ecore model of application elements
            * [Drawio](modules/html/modules/models/modules/app/modules/drawio/index.html) - Semantic mapping of Drawio diagrams to the application model. Allows to generate HTML sites from diagrams.
            * [Generators](modules/html/modules/models/modules/app/modules/gen/index.html) - Generator adapters for the application model
* Demos
    * [App Drawio](modules/demos/modules/app-drawio/index.html) - Demonstrations of generation of HTML sites from Drawio diagrams
        * [Actions](modules/demos/modules/app-drawio/modules/actions/index.html) - Demonstrates generation of a documentation site from a Drawio diagram using Amazon AWS deployment diagram as an example 
        * [Flow](modules/demos/modules/app-drawio/modules/flow-actions/index.html) - Demonstrates generation of a documentation site from a flow/process Drawio diagram
        * [Map](modules/demos/modules/app-drawio/modules/map/index.html) - Demonstrates generation of a documentation site from a (mind) map Drawio diagram
    * [TOGAF](modules/togaf/index.html) - Models representing [TOGAF](https://en.wikipedia.org/wiki/The_Open_Group_Architecture_Framework) concepts.
        * [ADM](modules/togaf/modules/adm/index.html) - flow model of TOGAF [Architecture Development Method](http://www.opengroup.org/public/arch/p2/p2_intro.htm)           

Nasdanika products are Java-based, hosted on [GitHub Nasdanika organization](https://github.com/Nasdanika) and published to [Maven Central](https://mvnrepository.maven.org/):

* [Core](https://mvnrepository.com/artifact/org.nasdanika.core)
* [HTML](https://mvnrepository.com/artifact/org.nasdanika.html)

