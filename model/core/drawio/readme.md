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

### Page and element links

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
Example: ``data:element/id,my-system.drawio#name,My+Component/id,my-class`` links to a diagram element with id ``my-class`` on the ``My Component`` page in ``my-system.drawio`` resource. 

        
This approach allows to create a multi-resource graph of diagrams. 
Nasdanika Drawio API also supports loading of documents from arbitrary URI's using a URI resolver. 
For example, ``maven://<gav>/<resource path>`` to load from Maven resources or ``gitlab://<project>/<path>`` to load resources from GitLab without cloning a repository, provided there is a handler (``Function<URI,InputStream>``) supporting the aforementioned URI's. 

### Executable diagrams

With Nasdanika Drawio API and other products you can make your diagrams executable.
There are two primary methods:

* Creating graph element processors for diagram elements
* Mapping diagrams to a semantic model and then making the model executable, possibly using graph processors

The first option requires less coding, the second is more flexible.

[Executable (computational) graphs & diagrams](https://medium.com/nasdanika/executable-computational-graphs-diagrams-1eeffc80976d) story provides a high level overview of executable graphs and diagrams.
[Graph documentation](../graph/index.html) features more technical details and code samples.
[Beyond Diagrams](https://leanpub.com/beyond-diagrams) book explains the mapping approach.
And [Compute Graph Demo](https://github.com/Nasdanika-Demos/compute-graph) provides examples of the both approaches using the compute graph from the "Executable (computational) graphs & diagrams" story.

## Model

[Drawio Model](https://mvnrepository.com/artifact/org.nasdanika.core/drawio-model) module provides an [EMF Ecore model](https://drawio.models.nasdanika.org/) for diagrams. 
A model instance can be obtained from the API document by calling ``Document.toModelDocument()`` method.

The model makes it more convenient to work with the diagram elements by:

* Making links from diagram elements to pages and other diagram elements bi-directional.
* Introducing [Tag](https://javadoc.io/doc/org.nasdanika.core/drawio-model/latest/org.nasdanika.drawio.model/org/nasdanika/drawio/model/Tag.html) class as opposed to a string in the API. ``Tag`` is contained by ``Page`` and has bi-directional reference with tagged elements.

* [Sources](https://github.com/Nasdanika/core/tree/master/drawio.model)
* [JavaDoc](https://javadoc.io/doc/org.nasdanika.core/drawio-model)
 