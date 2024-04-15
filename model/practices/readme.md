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
 
 Decisions are bound at ``variation point``. 
 For example, "storage" is a variation point, "blob storage" is one of alternatives, decision to use "blob storage" binds the variation point to a specific alternative.
Decision binding forms a graph. Once you bind, say, "storage" variation point, some downstream alternatives may become unavailable because they are incompatible with that binding.
Some might be available, but make no sense. 
For example, a decision to send data unencrypted over a public network is compatible with a decision to purchase some additional strong encryption hardware to use on-prem, but does it make business sense? 
 
Different alternatives feature different "quality attributes" - performance, reliability, cost. 
As the number of variation points and alternatives grows purely human-based decision making becomes inefficient.
In this case variation points can be modeled as requirements and alternatives as capability providers or capabilities with quality attributes (see[capability](https://docs.nasdanika.org/core/capability/index.html)). 
After this a list of "designs" (a.k.a. "provisioning plans") can be created.
A design/provisioning plan is a collection of compatible capabilities.
If a list of designs is short enough it can be analyzed by humans directly.
In the case of long lists or a large number of very similar designs [decision analysis](https://mcda.models.nasdanika.org/) can be employed for making a selection of a design which is best fit for purpose. 