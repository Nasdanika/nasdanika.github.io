Nasdankia provides two Maven modules for working with [Drawio](https://www.drawio.com/) diagrams - API and Model. 
The modules require Java 17 or above.

[TOC levels=6]

## API

[Drawio](https://mvnrepository.com/artifact/org.nasdanika.core/drawio) module provides [Java API](https://javadoc.io/doc/org.nasdanika.core/drawio) for reading and manipulating [Drawio](https://www.drawio.com/) diagrams.
It is built on top of [Graph](../graph/index.html).

The module provides the following interfaces representing elements of a diagram file:

* [Document](https://javadoc.io/doc/org.nasdanika.core/drawio/latest/org.nasdanika.drawio/org/nasdanika/drawio/Document.html) - the root object of the API representing a file/resource which contains one or more pages. 
* [Page](https://javadoc.io/doc/org.nasdanika.core/drawio/latest/org.nasdanika.drawio/org/nasdanika/drawio/Page.html) - a page containing a diagram (Model).
* [Model](https://javadoc.io/doc/org.nasdanika.core/drawio/latest/org.nasdanika.drawio/org/nasdanika/drawio/Model.html) - a diagram model containing the diagram root.
* [Root](https://javadoc.io/doc/org.nasdanika.core/drawio/latest/org.nasdanika.drawio/org/nasdanika/drawio/Root.html) - the root of the model containing layers.
* [Layer](https://javadoc.io/doc/org.nasdanika.core/drawio/latest/org.nasdanika.drawio/org/nasdanika/drawio/Layer.html) - a diagram may have one or more layers. Layers contain Nodes and Connections.
* [Node](https://javadoc.io/doc/org.nasdanika.core/drawio/latest/org.nasdanika.drawio/org/nasdanika/drawio/Node.html) - a node can be connected to other nodes with connections. A node may contain other nodes and connections.
* [Connection](https://javadoc.io/doc/org.nasdanika.core/drawio/latest/org.nasdanika.drawio/org/nasdanika/drawio/Connection.html) - a connection between two nodes. 

The below diagram shows relationships between the above interfaces including their super-interfaces:

```drawio-resource
./core/drawio/classes.drawio
```

[Util](https://javadoc.io/doc/org.nasdanika.core/drawio/latest/org.nasdanika.drawio/org/nasdanika/drawio/Util.html) provides utility methods such as ``layout()`` and methods to navigate and query documents and their elements.

* [Sources](https://github.com/Nasdanika/core/tree/master/drawio) 
* [JavaDoc](https://javadoc.io/doc/org.nasdanika.core/drawio)

## Page and element links

Nasdanika Drawio API extends the concept of linking to pages to cross-document linking to pages and page elements by name or ID.
Link targets (pages or elements) are available via ``getLinkTarget()`` method.

Drawio page links have the following format: ``data:page/id,<page id>`` with ``page/id`` being the "media type" and ``<page id>`` being the "data" of a [Data URL](https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/Data_URLs).

Nasdanika Drawio API extends it to additional media types:

* ``page/name``
* ``element/id``
* ``element/name``

The data (selector) format has the following format: 

* Page: ``[<diagram resource>#]<page selector>``
    * Diagram resource is a URI resolved relative to the current document URI. If not present then the link target page is in the same document.
    * Page selector is either page ID or URL encoded page name depending on the media type - id or name.
* Element: ``[<diagram resource>#][<page selector>/]<element selector>]``
    * Diagram resource is a URI resolved relative to the current document URI. If not present then the link target element is in the same document.
    * Page selector is either of:
        * ``id,<page id>``
        * ``name,<URL encoded page name>``
    * Element selector is either page ID or URL encoded element label text (stripped of HTML formatting) depending on the media type - id or name.
    
For elements URL's page selector is required if diagram resource URI is present.    
Examples:

* Page links:
    * ``data:page/name,compressed.drawio#Page-1`` - Link to compressed first page
    * ``data:page/name,compressed.drawio#Page+2`` - Link to compressed second page
* Element links:
    * ``data:element/id,7KSC1_O8d7ACaxm1iSCq-1`` - Link by ID to an element on the same page
    * ``data:element/name,name,Page+2/Linked`` - Link by name to Linked on Page 2 referenced by name
    * ``data:element/name,compressed.drawio#name,Page+2/Linked`` - Link to Linked on compressed second page
        
This approach allows to create a multi-resource graph of diagrams. 
Nasdanika Drawio API also supports loading of documents from arbitrary URI's using a URI resolver. 
For example, ``maven://<gav>/<resource path>`` to load from Maven resources or ``gitlab://<project>/<path>`` to load resources from GitLab without cloning a repository, provided there is a handler (``Function<URI,InputStream>``) supporting the aforementioned URI's. 

## Generating documentation sites

With [Nasdanika CLI](/nsd-cli/index.html) *[drawio](/nsd-cli/nsd/drawio/index.html) > [html-app](/nsd-cli/nsd/drawio/html-app/index.html) > [site](/nsd-cli/nsd/drawio/html-app/site/index.html)* 
command pipeline can be used to generate documentation web sites from Drawio diagrams:

* [Demo](https://nasdanika-demos.github.io/bob-the-builder/)
* [Video](https://www.youtube.com/watch?v=OtifPFetg9o) explaining how the above demo was created
* [Template repository](https://github.com/Nasdanika-Templates/drawio-site)
* [Internet Banking System](https://nasdanika-demos.github.io/internet-banking-system/index.html) - another demo: a sample C4 Model
* [Visual Communication Continuum](https://medium.com/nasdanika/visual-communication-continuum-4946f44ba853) - a Medium story which provides an overview of the approach and compares it with semantic mapping

## Executable diagrams

With Nasdanika Drawio API and other products you can make your diagrams executable as explained in the following sections.

### Invocable URIs

You may set diagram element properties to URIs of processors.
This approach is explained in [General Purpose Executable Graphs and Diagrams](https://medium.com/nasdanika/general-purpose-executable-graphs-and-diagrams-8663deae5248) Medium story. 
A demo repository is here - https://github.com/Nasdanika-Demos/executable-diagram-dynamic-proxy.
You can use this site/ repository as a starting point for your diagramming ecosystem:

* [Site](https://nasdanika-demos.github.io/general-purpose-executable-diagrams-story/)
* [Code](https://github.com/Nasdanika-Demos/general-purpose-executable-diagrams-story)

### Java

You can create graph element processors for diagram elements in Java. 

[Executable (computational) graphs & diagrams](https://medium.com/nasdanika/executable-computational-graphs-diagrams-1eeffc80976d) story provides a high level overview of executable graphs and diagrams.
[Graph documentation](../graph/index.html) features more technical details and code samples.
[Compute Graph Demo](https://github.com/Nasdanika-Demos/compute-graph) provides examples of this and semantic mapping (below) approaches using the compute graph from the "Executable (computational) graphs & diagrams" story.

### Semantic mapping

Semantic mapping diagrams is mapping of diagram elements to a semantic model and then making the model executable, possibly using graph processors.
[Visual Communication Continuum](https://medium.com/nasdanika/visual-communication-continuum-4946f44ba853) story provides an overview of semantic mapping and when to use it comparing to "direct generation". 
[Beyond Diagrams](https://leanpub.com/beyond-diagrams) book explains the mapping approach in more detail.

## Model

[Drawio Model](https://mvnrepository.com/artifact/org.nasdanika.core/drawio-model) module provides an [EMF Ecore model](https://drawio.models.nasdanika.org/) for diagrams. 
A model instance can be obtained from the API document by calling ``Document.toModelDocument()`` method.

The model makes it more convenient to work with the diagram elements by:

* Making links from diagram elements to pages and other diagram elements bi-directional.
* Introducing [Tag](https://javadoc.io/doc/org.nasdanika.core/drawio-model/latest/org.nasdanika.drawio.model/org/nasdanika/drawio/model/Tag.html) class as opposed to a string in the API. ``Tag`` is contained by ``Page`` and has bi-directional reference with tagged elements.

* [Sources](https://github.com/Nasdanika/core/tree/master/drawio.model)
* [JavaDoc](https://javadoc.io/doc/org.nasdanika.core/drawio-model)
 