Java bindings for [3d-force-graph](https://github.com/vasturiano/3d-force-graph).

* [Sources](https://github.com/Nasdanika/html/tree/master/3d-force-graph)
* [Javadoc](https://javadoc.io/doc/org.nasdanika.html/forcegraph3d)

## Maven dependency

```xml
<dependency>            
    <groupId>org.nasdanika.html</groupId>
    <version>2025.6.0</version>
    <artifactId>3d-force-graph</artifactId>
</dependency>               
```

## Java code

```java
ForceGraph3DFactory forceGraph3DFactory = ForceGraph3DFactory.INSTANCE;
ForceGraph3D forceGraph3D = forceGraph3DFactory.create();
forceGraph3D.name("graph");
String forceGraphContainerId = "force-graph";
forceGraph3D
    .elementId(forceGraphContainerId)
    .nodeAutoColorBy("'group'")
    .nodeVal("'size'")
    .linkDirectionalArrowLength(3.5)
    .linkDirectionalArrowRelPos(1);

// Add nodes and links here 

HTMLPage page = HTMLFactory.INSTANCE.page();
forceGraph3DFactory.cdn(page);
page.body(HTMLFactory.INSTANCE.div().id(forceGraphContainerId));                
page.body(TagName.script.create(System.lineSeparator(), forceGraph3D));
Files.writeString(new File("docs/force-graph-3d.html").toPath(), page.toString());  
```

## Examples

* [Maven dependencies graph](https://nasdanika-demos.github.io/maven-graph/graph-3d.html)
* [Maven dependencies graph with text](https://nasdanika-demos.github.io/maven-graph/force-graph-3d.html)
* Ecore documentation (generated from [ECharts Graph Model](https://echarts.models.nasdanika.org/graph/index.html#3d-force-graphs)):
    * [A2A](https://a2a.models.nasdanika.org/force-layout-graph-3d.html)
    * [GitLab](https://gitlab.models.nasdanika.org/force-layout-graph-3d-with-dependencies-and-subpackages.html)
    * [Ecore](https://ecore.models.nasdanika.org/force-layout-graph-3d.html)
    * [ECharts](https://echarts.models.nasdanika.org/graph/force-layout-graph-3d.html)


