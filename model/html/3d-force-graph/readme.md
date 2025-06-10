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

## Example

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
