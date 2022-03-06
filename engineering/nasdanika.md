This page provides an overview of Nasdanika products.

The primary target audience of the Nasdanika product line are Java developers who use Maven.

The main _theme_ can be expressed as "Efficient knowledge capture and dissemination" 
using [Domain-Driven Design](https://en.wikipedia.org/wiki/Domain-driven_design), [Model-based Systems Engineering (MBSE)](https://en.wikipedia.org/wiki/Model-based_systems_engineering) and code generation being the means to efficiently
codify knowledge and then disseminate it. 

Well, it's a mouthful! Let's elaborate a bit! 

First of all, "efficiency" above means minimal requirements of human and computing resources per "unit of knowledge". 
It also means hyper-local and hyper-focused experiences because humans are notoriously bad at context switching. 
One of approaches to minimize context switching is use of code generation using Java, e.g. generation of Bootstrap Web applications using fluent Java API. 
Another one is an option to author models in YAML without having to use a specialized model editor.  

Computing resources-wise - no need in server components, you can use Nasdanika products on a flash drive in a bunker or in the wilderness with no network: ``--everything-is-hyper-local``, paraphrasing the Git motto.
Access to the Internet would be needed only to pull the required libraries from [Maven Central](https://mvnrepository.org/artifact/org.nasdanika.engineering/parent) or a mirror.

[TOC levels=6]

## Sample scenario

You've got an idea of a new "something" - not necessarily software. 

Depending on the problem at hand, how your brain is wired, and who you need to communicate your idea to, you may start with а domain model, [flow model](modules/core/modules/flow/index.html), or the [engineering model](modules/engineering/modules/model/index.html).

### Models

#### Engineering

With the engineering model you can define consumers of what you want to create - [personas](modules/engineering/modules/model/Persona.html) - and their [goals](modules/engineering/modules/model/Goal.html). 
This will allow you to better understand and communicate the **Why** of your idea.  
It will also allow you enlist help of others, e.g. potential users, to elaborate the personas and their goals - they likely know better than you what they want, unless you are building it for yourself.

Then[^1] you may define [products](modules/engineering/modules/model/Product.html) and their [features](modules/engineering/modules/model/Feature.html). 
You may break down products into [modules](modules/engineering/modules/model/Module.html).
[Align](modules/engineering/modules/model/Alignment.html) features to goals.
This would define the **What** of your idea tied to the **Why**.

Then, define [releases](modules/engineering/modules/model/Release.html), [issues](modules/engineering/modules/model/Issue.html), [increments](modules/engineering/modules/model/Increment.html), [engineers](modules/engineering/modules/model/Engineer.html).
Assign features to releases, issues to features or releases, and releases to increments.
Assign issues to engineers.
You may also assign responsible engineers to releases, features, and increments. 
This would define the **How** and the **When** both traceable to the **What** (features) and the **Why** (persona goals).

As it was mentioned above, all of that can be done on a local computer - you don't need any server component, you don't even need to keep it under version control, although it is a good idea.
So, if you have a super-secret idea you can keep everything on an encrypted thumb drive and work on it at night in your basement. 
In a team environment you can use an isolated network to collaborate. 

One more advantage of ``everything is local`` is that you may produce self-contained idea definitions which can be archived, copied/forked etc. 
It allows to establish a discovery-delivery pipeline where many ideas can be brought to high enough level of detail to allow efficient selection for delivery later. 
Having shared definitions of personas, goals, [principles](modules/engineering/modules/model/Principles.html), and [objectives](modules/engineering/modules/model/Objective.html) would simplify comparison of ideas based on their benefit and cost.

There is more to it, see the engineering model documentation and a list of [features](modules/engineering/modules/model/features.html) for more details. 

#### Flow

With [Nasdanika Flow](modules/core/modules/flow/index.html) you can create and publish models of [flows](modules/core/modules/flow/Flow.html) - graphs of [activities](modules/core/modules/flow/Activity.html) performed by [participants](modules/core/modules/flow/Participant.html) using [resources](modules/core/modules/flow/Resource.html) and consuming and producing [artifacts](modules/core/modules/flow/Participant.html).

With Nasdanika Flow you can model customer journeys and team processes. 
[Services](modules/core/modules/flow/Service.html) allow to build flows "in terms" of other flows, e.g. a customer journey flow referencing an internal process as an activity.
[Inheritance](modules/core/modules/flow/features/inheritance/index.html) allows to define common flows and then customize them. 
For example, in an IT organization there might be core development process and then technology and regions specific extensions. 

The flow concepts are very similar to the concepts of a programming language, such as Java, or of a distributed system, such as a cloud application. 
In other words with Nasdanika Flow you can "program" or "codify" operations of an organization. 

The [journey](modules/engineering/modules/model/journey/package-summary.html) package in the engineering model (work in progress) aims to provide a bridge
between the flow model and then engineering model in order to allow to define flows at persona level, reference personas from participant definitions, products/features from resource definitions, etc.
With that it would be possible to build persona (customer) journeys in terms of product features and services provided by the organization and its engineers.

#### Domain

Problem domain structure is captured in a form of [EMF](https://www.eclipse.org/modeling/emf/) Ecore (meta) model using [Ecore Tools](https://www.eclipse.org/ecoretools/), which are part of [Eclipse Modeling Tools Package](https://www.eclipse.org/downloads/packages/release/2021-12/r/eclipse-modeling-tools). You can use a diagram or a tree editor to create domain models.

Flow and Engineering models mentioned above are examples of domain models for different problem domains.

Domain models serve the following purposes:

* Form a [Ubiquitous Language](https://martinfowler.com/bliki/UbiquitousLanguage.html) - a common rigorous language between users and developers.
[Nasdanika HTML Ecore](modules/html/modules/ecore/index.html) allows to generate documentation from Ecore models, "publish a dictionary" so to speak. 
It automatically generates pacakge diagrams, class context and inheritance diagrams. Documentation can be defined in annotations or loaded from external resources.
Model documentation is written in [Markdown](modules/core/modules/exec/modules/model/content/Markdown.html) and can contain embedded diagrams. 
* Generate Java code to load, diagnose, manipulate and save models. EMF Ecore provides capabilities to load models from and save to XML (XMI). 
[Nasdanika EMF](modules/core/modules/emf/index.html) adds an ability to load models from an arbitrary storage of maps, lists, and scalars - [YAML](https://en.wikipedia.org/wiki/YAML), [JSON](https://en.wikipedia.org/wiki/JSON), properties files. 
When loading from YAML the loaded injects [markers](modules/core/modules/ncore/Marker.html) into the loaded objects to trace data elements to resource (file), line, column where they were loaded from. 
If a [Git](https://git-scm.com/) repository is detected then a [GitMarker](modules/core/modules/ncore/GitMarker.html) is injected instead with information about [remotes](https://git-scm.com/book/en/v2/Git-Basics-Working-with-Remotes) and the commit hash.
In a a model loaded from multiple resources residing in multiple Git repositories it allows to trace data elements to their source in "space" (repository, file, line and column) and time (commit). 

You've probably heard before that "[all models are wrong, but some are useful](https://en.wikipedia.org/wiki/All_models_are_wrong)". 
Models are wrong by definition because their purpose to reflect an aspect of interest of reality.

If we define usefulness as positive [ROI](https://en.wikipedia.org/wiki/Return_on_investment) and short [time to market](https://en.wikipedia.org/wiki/Time_to_market), then with Nasdanika approach to modeling it is easier to
create a useful model because production and publication of a model requires less effort than alternative approaches as explained below.

##### Meta-Model creation

Use Ecore Tools.

##### Meta-Model documentation

* **Nasdanika:** Generate HTML with visualizations from models, mount it to a larger site. This site is an example of this approach. 
* **Alternatives:** Manual or Javadoc from generated model classes.

##### Production of Meta-Model binaries 

* **Nasdanika:** Maven Java build publishing to a Maven repository. 
* **Alternatives:** Maven Tycho build generating a p2 site which needs to be hosted somewhere.

##### Model editing

* **Nasdanika:** Any text editor to author YAML or JSON - use load specification from the generation documentation to create model instances. 
* **Alternatives:**  An editor shall be created and published to a p2 site and used from Eclipse. Or a web server is needed to host an in-browser editor.

##### Consumption of Meta-Model binaries 

To programmatically read or write models.

* **Nasdanika:** Any Maven application - add a dependency.
* **Alternatives:**  Eclipse product, OSGi bundle dependency.

##### Consumption of model content

* **Nasdanika:** Generate an HTML site from the model. 
* **Alternatives:** Requires Eclipse IDE and an editor/viewer or a server to host a web viewer

One area where Nasdanika approach to modeling may be useful is disposable/situational models - use a modeling approach to solve a problem which is small enough to justify investment into graphical editors, p2 hosting etc., but large enough to benefit from automation.

Metaphorically speaking, Nasdanika uses [r-selection](https://en.wikipedia.org/wiki/R/K_selection_theory#r-selection) - create cheap models quickly, see which one is useful and then invest more if needed.
The alternatives tend to be closer to the [K](https://en.wikipedia.org/wiki/R/K_selection_theory#K-selection) end of the spectrum requiring larger up-front investment. 

### Use of the models

#### Populate the model

Once there is a domain Ecore (meta) model you can start creating model resources containing definitions of model elements. 
E.g. if your meta-model contains classes [Organization](modules/engineering/modules/model/Organization.html) and [Engineer](modules/engineering/modules/model/Engineer.html),  
you may define organization, "Acme Corp." with an engineer "Joe Doe".

There are multiple ways to populate the model. Some of them are listed below. 
You don't to use one particular way - they can be used in combination. 
Some resources in your model may be defined YAML and edited in a text editor, some other may be defined in XMI and edited with, say, a diagram editor.
There resources can cross-reference each other.

##### YAML or JSON

You can create models in YAML. With this approach, unlike the others listed below, you don't need to create a specialized editor - just use a YAML or text editor and load specification pages of the generated model documentation as a reference.

Because models are defined in text they are easy to collaborate on without the use of specialized merge tools - use traditional diff and merge as for other textual artifacts such as Java files.

This approach also allows to relatively easily load data from external systems which have no knowledge of your models' XMI format. 
Data can be loaded from REST endpoints or from data exports.   

##### Tree editor

You can generate a tree editor for your models using Ecore Tools. 
It is a relatively small effort, but you'll need to build it, publish to a p2 repository (update site) and model authors will need to have Eclipse IDE and to be able to install the editor.

##### Diagram editor

With [Eclipse Sirius](https://www.eclipse.org/sirius/overview.html) you can generate visual diagram editors and with [Sirius Web](https://www.eclipse.org/sirius/sirius-web.html) you can provide a web interface for editing models.

##### Specialized text editor

With [Xtext](https://www.eclipse.org/Xtext/) you can create a specialized text editor with syntax highlighting, code completions, live validation and other features.

##### Custom resources and factories

You can also load data from external sources by implementing a custom [Resource](https://download.eclipse.org/modeling/emf/emf/javadoc/2.11/?org/eclipse/emf/ecore/resource/Resource.html) and [Resource.Factory](https://download.eclipse.org/modeling/emf/emf/javadoc/2.11/?org/eclipse/emf/ecore/resource/Resource.Factory.html).
[YamlResource](https://docs.nasdanika.org/modules/core/modules/emf/apidocs/?org/nasdanika/emf/persistence/YamlResource.html) and [YamlResourceFactory](https://docs.nasdanika.org/modules/core/modules/emf/apidocs/?org/nasdanika/emf/persistence/YamlResourceFactory.html) are examples of such implementation.

For example, you may create a resource to load issue information from [Jira](https://www.atlassian.com/software/jira) using [Jira Java API](https://mvnrepository.com/artifact/com.atlassian.jira/jira-api/9.0.0-QR-20211224040439).

In Domain-Driven Design terminology such resource and a resource factory would act as an [Anti-Corruption Layer](https://docs.microsoft.com/en-us/azure/architecture/patterns/anti-corruption-layer).

#### Web application

So you've captured quite a bit of knowledge in your models, now it is time to share it!
One way to share knowledge with humans is to generate a web site. 
For example, this site was generated from multiple engineering models stored in multiple Git repositories with documentation generated from Ecore models "mounted" to it.

You can add dynamic read-only behavior to the generated site using [Single-page applications](https://en.wikipedia.org/wiki/Single-page_application) which use model data. 
[All issues](all-issues.html) page is an example of such an application built with [Vue.js](https://vuejs.org/) and [BootstrapVue](https://bootstrap-vue.org/).
It uses browser local storage for user preferences. 
This approach allows to have highly focused mini-apps injected into a static web site.
  
Shall you need more dynamic behavior you can use helper web services. 
In this case model data can be used as input to the services in addition to user-provided data putting user input into a context.
E.g. a single site may have multiple mini-apps calling the same service but with different inputs.

#### Programmatic

Model data can also be used by computers. In Java you use the generated model classes. 
You may load your model from multiple sources and then save it to XMI which can be published to a binary repository or on a web site.

In non-java scenarios you may generate code in the target language. E.g. cross-referencing JSON files for a static REST API or
JavaScript files to use model information in the browser.
It can be done in combination with generating a static file, i.e. your site would contain both HTML and JSON/JavaScript. 
In this case JSON/JavaScript can be used by the mini-apps.

## Products overview

This section provides a brief overview of Nasdanika products and their modules.

* [Core](modules/core/index.html) - foundational functionality
    * [CLI](modules/core/modules/cli/index.html) - classes which allow to call [execution model](modules/core/modules/common/features/execution-model/index.html) [Command](modules/core/modules/common/apidocs/index.html?org/nasdanika/common/Command.html) from a command line.
    * [Common](modules/core/modules/common/index.html) - common functionality such as the execution model, resource and persistence frameworks.
    * [Diagram](modules/core/modules/diagram/index.html) - Diagram EMF model is a level of abstraction to create diagram visualizations using Java API and then generate [PlantUML](https://plantuml.com/) and [diagrams.net](https://www.diagrams.net/) diagrams from it. 
    * [EMF](modules/core/modules/emf/index.html) - classes for working with Ecore models, e.g. classes for loading models from YAML files.
    * [Exec](modules/core/modules/exec/index.html) - Ecore models and adapters to execution model participants to build code generators and to load model content from different sources.
    * [Flow](modules/core/modules/flow/index.html) - Ecore flow model to capture customer journeys/business processes. 
    * [mxgraph](modules/core/modules/mxgraph/index.html) - Maven jar packaging of mxgraph sources. It is used to generate diagrams.net diagrams.
    * [Ncore](modules/core/modules/ncore/index.html) - Ecore model containing common classes used/extended in other models.
* [HTML](modules/html/index.html) - Java API's and models for generating HTML
    * [Bootstrap](modules/html/modules/bootstrap/index.html) - Fluent Java API to generate [Bootstrap 4](https://getbootstrap.com/docs/4.6/getting-started/introduction/) markup.
    * [Ecore](modules/html/modules/ecore/index.html) - Classes to generate documentation of Ecore models as action models (see Models/Application below). 
    * [EMF](modules/html/modules/emf/index.html) - Base classes for building generators of action models from Ecore model elements.
    * [Flow](modules/html/modules/flow/index.html) - Generators of action models from the flow model elements.
    * [HTML](modules/html/modules/html/index.html) - Fluent Java API to generate [HTML 5](https://en.wikipedia.org/wiki/HTML5) markup.
    * [jsTree](modules/html/modules/jstree/index.html) - Java API for generating [jsTree](https://www.jstree.com/) JSON.
    * [Models](modules/html/modules/models/index.html) - Ecore models to build HTML pages and applications.
        * [HTML](modules/html/modules/models/modules/html/index.html) - Ecore model representing HTML elements and generation adapters.
        * [Bootstrap](modules/html/modules/models/modules/bootstrap/index.html) - Ecore model representing Bootstrap 4 elements and generation adapters. 
        * [Application](modules/html/modules/models/modules/app/index.html) - Ecore model representing a web site/application as a hierarchy of actions. Adapters to generate HTML sites (resource models) from action models.
* [Engineering](modules/engineering/index.html) - Ecore model representing (software) engineering concepts such as an organization, engineer, product, release, feature, issue, ... Adapters for generating HTML sites from engineering models.
* [TOGAF](modules/togaf/index.html) - Models representing [TOGAF](https://en.wikipedia.org/wiki/The_Open_Group_Architecture_Framework) concepts.
    * [ADM](modules/togaf/modules/adm/index.html) - flow model of TOGAF [Architecture Development Method](http://www.opengroup.org/public/arch/p2/p2_intro.htm).                  

Nasdanika products are Java-based, hosted on [GitHub Nasdanika organization](https://github.com/Nasdanika) and published to [Maven Central](https://mvnrepository.maven.org/):

* [Core](https://mvnrepository.maven.org/search?q=g:org.nasdanika.core)
* [HTML](https://mvnrepository.maven.org/search?q=g:org.nasdanika.html)
* [Engineering](https://mvnrepository.maven.org/search?q=g:org.nasdanika.engineering)


[^1]: Or before, or in parallel - totally up to you. Start somewhere, expand from there. 