Nasdankia provides two Maven modules for working with Drawio diagrams - API and Model. 
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

### Executable diagrams

With Nasdanika Drawio API and other products you can make your diagrams executable.
There are two primary methods:

* Creating graph element processors for diagram elements
* Mapping diagrams to a semantic model and then making the model executable, possibly using graph processors

The first option requires less coding, the second is more flexible.

[Executable (computational) graphs & diagrams](https://medium.com/nasdanika/executable-computational-graphs-diagrams-1eeffc80976d) provides a high level overview of executable graphs and diagrams.
[Graph documentation](../graph/index.html) features more technical details and code samples.
[Beyond Diagrams](https://leanpub.com/beyond-diagrams) book explains the mapping approach.
And [Compute Graph Demo](https://github.com/Nasdanika-Demos/compute-graph) provides examples of the both approaches using the compute graph from the "Executable (computational) graphs & diagrams" story.

## Model

[Drawio Model](https://mvnrepository.com/artifact/org.nasdanika.core/drawio-model) module provides an [EMF Ecore model](https://drawio.models.nasdanika.org/) for diagrams. 
A model instance can be obtained from the API document by calling ``Document.toModelDocument()`` method.

The model makes it more convenient to work with the diagram elements by:

* Making a reference between pages and model elements bi-directional.
* Introducing [Tag](https://javadoc.io/doc/org.nasdanika.core/drawio-model/latest/org.nasdanika.drawio.model/org/nasdanika/drawio/model/Tag.html) class as opposed to a string in the API. ``Tag`` is contained by ``Page`` and has bi-directional reference with tagged elements.

* [Sources](https://github.com/Nasdanika/core/tree/master/drawio.model)
* [JavaDoc](https://javadoc.io/doc/org.nasdanika.core/drawio-model)
 