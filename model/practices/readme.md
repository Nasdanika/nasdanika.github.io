Practices explain *how* to use Nasdanika products to achieve specific goals and explain *why* particular design choices wer made.
The [enterprise model](https://enterprise.models.nasdanika.org/) provides deeper insight on the *WHY* in general.

The practices are organized into an [enterprise continuum](https://pubs.opengroup.org/togaf-standard/architecture-content/chap06.html#tag_06) from the most generic on the left to the most specific on the right.
However, the most specific on the right is still generic and needs to be specialized for a particular application (embodiment):

* [Analysis, Visualization & Generation](generic/index.html) - describes a general approach on using Nasdanika products.
* [Java Analysis, Visualization & Generation](generic/index.html) - application of the above to the [Java model](https://java.models.nasdanika.org/)[^java]
    * Loading and analyzing Java sources and bytecode, generation of non-Java artifacts such as HTML reports
    * Generation of Java sources.
* [JUnit test generation for low coverage methods](junit/index.html) - further specialization of the Java practice to identify methods with low test coverage using the [Coverage Model](https://coverage.models.nasdanika.org/) and then generate JUnit tests for those methods using the Java model and OpenAI.

You can think of the three practices above as progressive "binding of decision" as you move from the left to the right to reach "executability" - ability to deliver value. 

A java analogy for progressive specialization would be incremental binding of generic types as exemplified below:

* ``Map<K,V>`` - generic map.
* ``MyMap<K extends Comparable> extends Map<<K, MyValue<K>>`` - the above map bound to a single generic parameter with an upper bound. It is a specialization of the above generic map which is also generic. Some decisions were bound, but there are still decisions to be bound.
* ``MyMap<String> theMap = ...;`` -  fully bound map.

 [^java]: The page provides a general overview and the [book](https://leanpub.com/java-analysis) goes into more details.