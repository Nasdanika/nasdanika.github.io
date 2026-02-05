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
* [ConnectionPoint](https://javadoc.io/doc/org.nasdanika.core/drawio/latest/org.nasdanika.drawio/org/nasdanika/drawio/ConnectionPoint.html) - Points on a node (ports) where connections exit and enter. For more details about connection points see [Connection Points, functionality and customization](https://drawio-app.com/blog/connection-points-functionality-and-customization-in-project-management/)
* [Connection](https://javadoc.io/doc/org.nasdanika.core/drawio/latest/org.nasdanika.drawio/org/nasdanika/drawio/Connection.html) - a connection between two connectables - nodes or connection points. 

The below diagram shows relationships between the above interfaces including their super-interfaces:

```drawio-resource
./core/drawio/classes.drawio
```

Drawio ``Element``, ``Node`` and ``Connection`` interfaces extend their Graph namesakes. Therefore, any diagram is a graph and, as such, can be made executable by creating graph processors.
Need to do some non-trivial processing? Draw it, document it, then create processors.

[Util](https://javadoc.io/doc/org.nasdanika.core/drawio/latest/org.nasdanika.drawio/org/nasdanika/drawio/Util.html) provides utility methods such as ``layout()`` and methods to navigate and query documents and their elements.

* [Sources](https://github.com/Nasdanika/core/tree/master/drawio) 
* [JavaDoc](https://javadoc.io/doc/org.nasdanika.core/drawio)
* [Maven Central](https://central.sonatype.com/artifact/org.nasdanika.core/drawio)

## Page and element links

Nasdanika Drawio API extends the concept of linking to pages to cross-document linking to pages and page elements by name, ID, or [SpEL](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions) expression.
Link targets (pages or elements) are available via ``getLinkTarget()`` method.

Drawio page links have the following format: ``data:page/id,<page id>`` with ``page/id`` being the "media type" and ``<page id>`` being the "data" of a [Data URL](https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/Data_URLs).

Nasdanika Drawio API extends it to additional media types:

* ``page/name``
* ``element/id``
* ``element/name``
* ``spel`` - Uses a SpEL expression to evaluate the link target in the context of the current element. Format: ``data:spel,<expression>``

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

## Magic properties

The API implements placeholder interpolation ``%<property name>%`` in the same way as Draw.io does if the "Placeholders" checkbox is checked. 
It also adds two "magic" properties to help with using model element values in structured properties. 
For example, you may have a property with YAML configuration with some values computed from the element properties.

### $style

Evaluates to a value of the style key provided after the colon.

Example: ``$style:fillColor``

### $spel

Evaluates a [Spring Expression Language](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#expressions) (SpEL) expression.

Example: ``$spel:style["fillColor"]``

## Selectors

The `List<ModelElement<?>> ModelElement.select(String selector)` method allows to traverse and query the diagram model structure using a path-based selector syntax. 

It takes a path-based selector string that specifies which related elements to retrieve and returns a list of model elements matching the specified selector criteria.

Selectors support a path-based syntax using forward slashes (`/`) to traverse relationships. 
Multiple segments can be chained together to navigate through the model graph.

### Basic Selectors

| Selector | Description | Example |
|----------|-------------|---------|
| `..` | Parent element | `element.select("..")` |
| `child` | Direct children | `element.select("child")` |
| `link-target` | Element or page linked via the link property | `element.select("link-target")` |
| `incoming-connection` | Incoming connections (for nodes) | `node.select("incoming-connection")` |
| `outgoing-connection` | Outgoing connections (for nodes) | `node.select("outgoing-connection")` |
| `source` | Source node (for connections) | `connection.select("source")` |
| `target` | Target node (for connections) | `connection.select("target")` |

### Qualified Selectors

Selectors can be qualified with filters using square brackets `[...]`:

#### Filter by ID
```
selector[element-id]
```
Returns only elements with the specified ID.

#### Filter by Property
```
selector[property-name=property-value]
```
Returns only elements where the specified property equals the given value.

### Path Composition

Selectors can be chained using `/` to traverse multiple relationships:

```
segment1/segment2/segment3
```

### Examples

#### Navigate to Parent

```java
// Get the parent element
ModelElement<?> parent = element.select("..").getFirst();
```

#### Get Child Elements

```java
// Get all direct children
List<ModelElement<?>> allChildren = element.select("child");

// Get a specific child by ID
ModelElement<?> specificChild = element.select("child[bob]").getFirst();

// Get a child by property value
ModelElement<?> childByAlias = element.select("child[alias=bobby]").getFirst();
```

#### Working with Connections (Node Context)

```java
// Assuming we have a Node instance
Node bob = // ... get node instance

// Get all incoming connections
List<ModelElement<?>> incomingConnections = bob.select("incoming-connection");

// Get a specific incoming connection by ID
ModelElement<?> specificConnection = bob.select("incoming-connection[alice-to-bob]").getFirst();

// Get an incoming connection filtered by property
ModelElement<?> secretConnection = bob.select("incoming-connection[classification=top-secret]").getFirst();

// Get all outgoing connections
List<ModelElement<?>> outgoingConnections = bob.select("outgoing-connection");

// Get a specific outgoing connection by ID
ModelElement<?> outConnection = bob.select("outgoing-connection[alice-to-bob]").getFirst();
```

### Navigate Through Connections

```java
Node bob = // ... get node instance

// Navigate from a node to the source of an incoming connection
ModelElement<?> sourceNode = bob.select("incoming-connection/source").getFirst();

// Navigate from a node to the target of an outgoing connection
ModelElement<?> targetNode = bob.select("outgoing-connection/target").getFirst();
```

### Complex Path Traversal

```java
Node alice = // ... get node instance

// Navigate from Alice through an outgoing connection to Bob, then to Bob's parent
ModelElement<?> bobsHouse = alice.select("outgoing-connection/target/..").getFirst();
```

### Working with Connection Source/Target (Connection Context)

```java
Connection connection = // ... get connection instance

// Get the source node of a connection
Node source = (Node) connection.select("source").getFirst();

// Get the target node of a connection
Node target = (Node) connection.select("target").getFirst();
```

### Reference Table

#### For All Model Elements

| Selector | Returns | Notes |
|----------|---------|-------|
| `..` | Parent element | Returns the containing element |
| `child` | All children | Direct child elements |
| `child[id]` | Child by ID | Single child with specified ID |
| `child[prop=value]` | Children by property | Children matching property criteria |
| `link-target` | Linked target | Element or page referenced by link property |

#### For Nodes

| Selector | Returns | Notes |
|----------|---------|-------|
| `incoming-connection` | All incoming connections | Connections where this node is the target |
| `incoming-connection[id]` | Specific incoming connection | By connection ID |
| `incoming-connection[prop=value]` | Filtered incoming connections | By connection property |
| `outgoing-connection` | All outgoing connections | Connections where this node is the source |
| `outgoing-connection[id]` | Specific outgoing connection | By connection ID |
| `outgoing-connection[prop=value]` | Filtered outgoing connections | By connection property |

#### For Connections

| Selector | Returns | Notes |
|----------|---------|-------|
| `source` | Source node | The node where the connection originates |
| `target` | Target node | The node where the connection terminates |

### Implementation Notes

- The `select()` method traverses paths recursively, evaluating each segment from left to right
- Returns an empty list if no elements match the criteria
- Filters in square brackets support exact matching only (no wildcards or regex)
- Property names and values in filters are case-sensitive
- The method is implemented differently for different element types (Node, Connection, etc.) to provide type-specific navigation

### Error Handling

The method returns an empty list when:
- No elements match the selector criteria
- An invalid selector segment is provided
- A relationship doesn't exist (e.g., selecting `incoming-connection` on an element that isn't a Node)

## Property paths

Property paths provide a way to access properties of related model elements without explicitly navigating through the model hierarchy. 
A property path combines the navigation capabilities of selectors with property access, allowing you to traverse relationships and retrieve property values in a single expression.

### Syntax

```
selector/property-name
```

A property path consists of:
- **Selector** - A navigation expression that identifies related elements 
- **Forward slash** (`/`) - Separates the selector from the property name
- **Property name** - The name of the property to retrieve from the selected element

### Reference

| Property Path | Description | Example Value |
|---------------|-------------|---------------|
| `../property-name` | Property of parent element | `"parent-id"` |
| `child/property-name` | Property of first child | `"child-value"` |
| `child[id]/property-name` | Property of child with specific ID | `"specific-value"` |
| `child[prop=value]/property-name` | Property of child matching criteria | `"filtered-value"` |
| `incoming-connection/property-name` | Property of incoming connection | `"connection-value"` |
| `outgoing-connection/property-name` | Property of outgoing connection | `"connection-value"` |
| `incoming-connection/source/property-name` | Property of connection source | `"source-value"` |
| `outgoing-connection/target/property-name` | Property of connection target | `"target-value"` |


## Style interfaces

The style interfaces provide a type-safe, fluent API for working with Draw.io element styles. 
Since Draw.io styles are stored as key-value pairs, all style interfaces extend `Map<String, String>`, allowing direct manipulation of style properties while also providing convenient typed methods for commonly used style attributes.

### Style Hierarchy

```
Style (extends Map<String,String>)
  │
  ├── LineStyle
  │     │
  │     ├── ConnectionStyle  (for edges/connections)
  │     │
  │     └── NodeStyle        (for shapes/vertices)
```

### Style
**Package:** `org.nasdanika.drawio.style`

The base style interface that all other style interfaces extend. Provides common styling properties applicable to all element types.

**Key methods:**

- `opacity()` / `opacity(String)` / `opacity(int)` - Element opacity
- `rounded()` / `rounded(boolean)` - Rounded corners
- `shadow()` / `shadow(boolean)` - Drop shadow effect
- `enumerate()` / `enumerate(boolean)` - Element enumeration flag
- `enumerateValue()` / `enumerateValue(String)` - Custom enumeration value

### LineStyle
**Package:** `org.nasdanika.drawio.style`  
**Extends:** `Style`

Provides line-related styling properties used for borders and edges. Applicable to both nodes (borders) and connections (edge lines).

**Key methods:**

- `color()` / `color(String)` - Line/stroke color
- `width()` / `width(String)` - Line width
- `dashed()` / `dashed(String)` - Dashed line pattern

### ConnectionStyle
**Package:** `org.nasdanika.drawio.style`  
**Extends:** `LineStyle`

Specialized styling for connections (edges) between nodes.

**Key methods:**

- `startArrow()` / `startArrow(String)` / `startArrow(Arrow)` - Arrow at connection start
- `endArrow()` / `endArrow(String)` / `endArrow(Arrow)` - Arrow at connection end
- `startFill()` / `startFill(boolean)` - Fill start arrow
- `endFill()` / `endFill(boolean)` - Fill end arrow
- `edgeStyle()` / `edgeStyle(String)` - Edge routing style (e.g., "orthogonalEdgeStyle")

### NodeStyle
**Package:** `org.nasdanika.drawio.style`  
**Extends:** `LineStyle`

Specialized styling for nodes (shapes/vertices).

**Key methods:**

- `backgroundColor()` / `backgroundColor(String)` - Fill color
- `shape()` / `shape(String)` - Shape type
- `verticalAlign()` / `verticalAlign(String)` - Vertical text alignment
- `align()` / `align(String)` - Horizontal text alignment
- `fontSize()` / `fontSize(String)` - Font size
- `fontColor()` / `fontColor(String)` - Font color
- `fontStyle()` / `fontStyle(String)` - Font style attributes
- `labelBackgroundColor()` / `labelBackgroundColor(String)` - Label background
- `labelBorderColor()` / `labelBorderColor(String)` - Label border
- `rotation()` / `rotation(String)` - Element rotation angle
- `container()` / `container(boolean)` - Container flag
- `collapsible()` / `collapsible(boolean)` - Collapsible container flag

### Important Notes

#### Not Exhaustive
The style interfaces are **not exhaustive** and contain only a subset of all styles supported by Draw.io. 
They focus on commonly used style properties that are frequently needed in code.

#### As-Needed Extension
These interfaces will be extended on an **as-needed basis** as additional style properties are required. 
If you need a style property not currently available through the typed methods, you can always use the `Map` interface directly:

```java
// Using typed method (if available)
nodeStyle.backgroundColor("#dae8fc");

// Using Map interface directly (for any property)
nodeStyle.put("fillColor", "#dae8fc");
nodeStyle.put("gradientColor", "#7ea6e0");
nodeStyle.put("gradientDirection", "east");
```

### Fluent API
All setter methods return the style interface itself, enabling fluent method chaining:

```java
connectionStyle
    .edgeStyle("orthogonalEdgeStyle")
    .rounded(true)
    .dashed("1")
    .width("2")
    .color("#0077ff")
    .endArrow("classic")
    .endFill(true);
```

### Examples

#### Styling a Node

```java
Node node = layer.createNode();
node.setLabel("My Node");

NodeStyle style = node.getStyle();
style
    .backgroundColor("#f5f5f5")
    .shape("rectangle")
    .rounded(true)
    .shadow(true)
    .fontSize("14")
    .fontColor("#333333")
    .color("#666666")      // Border color
    .width("2");           // Border width
```

#### Styling a Connection

```java
Connection connection = layer.createConnection(source, target);
connection.setLabel("My Connection");

ConnectionStyle style = connection.getStyle();
style
    .edgeStyle("orthogonalEdgeStyle")
    .color("#0077ff")
    .width("2")
    .dashed("1")
    .rounded(true)
    .endArrow("classic")
    .endFill(true)
    .startArrow("oval")
    .startFill(false);
```

#### Direct Map Access for Additional Properties

```java
NodeStyle style = node.getStyle();

// Use typed methods where available
style.backgroundColor("#dae8fc");
style.rounded(true);

// Use Map interface for properties without typed methods
style.put("gradientColor", "#7ea6e0");
style.put("gradientDirection", "east");
style.put("swimlane", "1");
style.put("whiteSpace", "wrap");
```

#### Reading Style Properties

```java
// Using typed getters
String color = nodeStyle.color();
boolean isRounded = nodeStyle.rounded();
String bgColor = nodeStyle.backgroundColor();

// Using Map interface
String gradientColor = nodeStyle.get("gradientColor");
String customProperty = nodeStyle.get("myCustomStyle");
```

## Auto-layout

The `Util` class provides automatic layout algorithms to arrange diagram elements so they don't overlap. 
These methods are useful when programmatically creating diagrams or importing data from external sources where node positions are not predefined.

* `layout(Root root, int gridSize)` - a simple layout method that arranges top-level nodes on all layers to prevent overlapping. This method analyzes connections between nodes to determine their relationships and positions them accordingly. Elements with more outgoing connections are typically positioned before (above) elements with more incoming connections, creating a natural flow direction in the diagram. The method is not designed to provide great auto-layouts, just starting point layouts for humans to manually layout further.
* `layout(Collection<Node> nodes, Point offset, Function<Boolean, Supplier<Point>> offsetGeneratorProvider)` - an advanced layout method that provides more control over the layout process. It computes optimal positions for a collection of nodes and returns a map of their calculated rectangles. The method automatically updates the node geometries.
* `forceLayout(Root root, double layoutWidth, double layoutHeight)` - Force-directed layout using the Fruchterman-Reingold algorithm from JGraphT. The algorithm treats the graph as a physical system where nodes repel each other while connections act as springs pulling connected nodes together. This method is particularly effective for visualizing graph structures with complex interconnections.
* `layout(Collection<Node> nodes, LayoutAlgorithm2D<Node, Connection> layout, LayoutModel2D<Node> layoutModel)` - Generic layout method that works with any [JGraphT Layout Algorithms](https://jgrapht.org/javadoc/org.jgrapht.core/org/jgrapht/alg/drawing/package-summary.html).

For more sophisticated auto-layout capabilities including hierarchical layouts, layered layouts, and advanced graph visualization algorithms, consider using the [Eclipse Layout Kernel (ELK)](https://eclipse.dev/elk/) via [Nasdanika Draw.io - ELK Integration](https://elk.models.nasdanika.org/)

ELK provides:
- Multiple layout algorithms (layered, force, radial, box, etc.)
- Hierarchical layout with containment
- Port constraints and placement
- Edge routing algorithms
- Layout configuration and fine-tuning options
- Support for complex graph structures

## Generating documentation sites

With [Nasdanika CLI](/nsd-cli/index.html) *[drawio](/nsd-cli/nsd/drawio/index.html) > [html-app](/nsd-cli/nsd/drawio/html-app/index.html) > [site](/nsd-cli/nsd/drawio/html-app/site/index.html)* 
command pipeline can be used to generate documentation web sites from Drawio diagrams:

* [Demo](https://nasdanika-demos.github.io/bob-the-builder/)
* [Video](https://www.youtube.com/watch?v=OtifPFetg9o) explaining how the above demo was created
* [Template repository](https://github.com/Nasdanika-Templates/drawio-site)
* [Internet Banking System](https://nasdanika-demos.github.io/internet-banking-system/index.html) - another demo: a sample C4 Model
* [Visual Communication Continuum](https://medium.com/nasdanika/visual-communication-continuum-4946f44ba853) - a Medium story which provides an overview of the approach and compares it with [semantic mapping](../mapping/index.html)
* [Semantic Mapping](https://medium.com/nasdanika/semantic-mapping-3ccbef5d6c70) - a medium story focusing on Semantic Mapping

### Overview video

Below is an AI-generated overview video:

<div class="embed-responsive embed-responsive-16by9">
    <iframe src="https://www.youtube.com/embed/8kkJAwrgnKM?si=q1sIWeCm141ShYDW" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>
</div>

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


## Ecore Model

[Drawio Model](https://mvnrepository.com/artifact/org.nasdanika.core/drawio-model) module provides an [EMF Ecore model](https://drawio.models.nasdanika.org/) for diagrams. 
A model instance can be obtained from the API document by calling ``Document.toModelDocument()`` method.

The model makes it more convenient to work with the diagram elements by:

* Making links from diagram elements to pages and other diagram elements bi-directional.
* Introducing [Tag](https://javadoc.io/doc/org.nasdanika.core/drawio-model/latest/org.nasdanika.drawio.model/org/nasdanika/drawio/model/Tag.html) class as opposed to a string in the API. ``Tag`` is contained by ``Page`` and has bi-directional reference with tagged elements.

* [Sources](https://github.com/Nasdanika/core/tree/master/drawio.model)
* [JavaDoc](https://javadoc.io/doc/org.nasdanika.core/drawio-model)
 