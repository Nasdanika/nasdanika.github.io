* [Sources](https://github.com/Nasdanika/core/tree/master/echarts)
* [Javadoc](https://javadoc.io/doc/org.nasdanika.core/echarts/latest/org.nasdanika.emf/module-summary.html)

This module provides functionality on top of [ECharts-Java](https://github.com/ECharts-Java/ECharts-Java) to
simplify charts generation.

* ``SeriesChart<E,X,Y,S>`` - an interface for creating charts from collections of data with X and Y mapper functions, writing charts to a file and optionally opening them in a Web browser.
* ``AbstractSericesChart<E,X,Y,S, C extends Chart<?,?>>`` - an abstract class implementing the above interface and using ECharts  Java ``Chart``. Provides a built-in HTML page template with required scripts and stylesheets. Provides additional ``write()`` methods and ``generateChartJSON()`` for adding a chart to a custom web page.
* ``LineSeriesChart<E,X,Y>`` - a concrete subclass of the above abstract class which uses ECharts Java ``Line`` chart.

## Example

```java
LineSeriesChart<Map.Entry<String,Number>, String, Number> lineSeriesChart = new LineSeriesChart<>((a,b) -> a.compareTo(b), Function.identity());        

LineEmphasis emphasis = new LineEmphasis();
emphasis.setFocus("series");

LineSeriesBuilder<Entry<String, Number>> builder = lineSeriesChart.createSeries("Test", Map.Entry::getKey, Map.Entry::getValue);
builder.addElement(Map.entry("Monday", 1));
builder.addElement(Map.entry("Tuesday", 2));
builder.addElement(Map.entry("Wednesday", 3));
builder.addElement(Map.entry("Thursday", 8));
builder.addElement(Map.entry("Friday", 11));
builder.lineSeries().setEmphasis(emphasis);

MarkArea2DDataItemDim startPoint = new MarkArea2DDataItemDim();
startPoint.setName("Area");
startPoint.setXAxis("Monday");

MarkArea2DDataItem markAreaItem = new MarkArea2DDataItem();
markAreaItem.setStartPoint(startPoint);

MarkArea2DDataItemDim endPoint = new MarkArea2DDataItemDim();
endPoint.setXAxis("Tuesday");
markAreaItem.setEndPoint(endPoint);

MarkArea markArea = new MarkArea();
markArea.setData(new MarkAreaDataItemOption[] { markAreaItem });

builder.lineSeries().setMarkArea(markArea);

LineSeriesBuilder<Entry<String, Number>> builder2 = lineSeriesChart.createSeries("Test 2", Map.Entry::getKey, Map.Entry::getValue);
builder2.addElement(Map.entry("Monday", 2));
builder2.addElement(Map.entry("Tuesday", 3));
builder2.addElement(Map.entry("Wednesday", 5));
builder2.addElement(Map.entry("Thursday", 6));
builder2.addElement(Map.entry("Friday", 7));
builder2.lineSeries().setEmphasis(emphasis);

lineSeriesChart.write(
        new File("target/line.html"), 
        "Test", 
        true,
        chart -> {
            Option option = chart.getOption();
            DataZoom dataZoom = new DataZoom();
            dataZoom.setType("inside");
            option.setDataZoom(dataZoom);
        },
        null);
```

[Result](/demos/core/echarts/line.html)
