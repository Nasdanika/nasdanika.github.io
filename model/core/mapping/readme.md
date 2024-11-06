[Visual Communication Continuum](https://medium.com/nasdanika/visual-communication-continuum-4946f44ba853) story provides an overview of semantic mapping and when to use it comparing to "direct generation". 
[Beyond Diagrams](https://leanpub.com/beyond-diagrams) book explains the mapping approach in more detail.

[TOC levels=6]


TODO - Ecore model diagram, concepts

```drawio-resource
./core/drawio/classes.drawio
```

## Resources

* [Sources](https://github.com/Nasdanika/core/tree/master/mapping) 
* [JavaDoc](https://javadoc.io/doc/org.nasdanika.core/mapping)

# Mapping

Mapping of one model to another is a non-trivial process. 
In the case of diagrams the order in which diagram elements are mapped is not under control of the user - it depend on the order of creation of the diagram elements.
Therefore, the mapping process consists of several phases and some of them may involve multiple passes.
This chapter describes the mapping process in the order of phases. 
A phase section explains element configuration properties used for that phase.
In the mapping reference chapters properties are ordered alphabetically.

## Initialization

In the first phase diagram elements are associated with (mapped to) elements of the target problem domain. 
For example, a person image can be associated with a person object from the family model. 
The phase is called "Initialization" because the mapping process is similar to assigning a value to a variable 
in languages like Java and virtually identical to what happens in JavaScript where objects are essentially maps.
The difference is that in Java and JavaScript variable/field names are strings, but in the case of diagrams they are the diagram elements themselves.

The following sections explain the initialization sequence

### Initializer

``initializer`` property value shall be Spring Expression Language (SpEL) expression evaluating to a semantic element.
It may return ``null`` - in this case ``type`` would be used to create a semantic element, if specified.

This property can be used, for example, to look-up semantic elements defined elsewhere. 
Say, a list of family members can be defined in an MS Excel workbook, but family relationships in a diagram.
For this to work the workbook shall be available via expression variables during the loading process.
The below code snippet is a Java analogy of the initializer expression:

```java
EObject isaDiagramElement = familyWorkBook.findById("isa");
```

Another example is "progressive enrichment". 
For example, high-level architecture is defined in some model and a diagram uses 
initializers for already defined architecture elements and ``type`` for their sub-elements. 
This approach can be applied multiple times similar to how Docker images are assembled from layers and base images - you can layer fine-grained models/diagrams over coarse-grained ones.
If you are old enough to remember JPEG images being loaded over a slow dial-up connection - something like that.

In order to implement lookup initializers, override ``getVariables()`` or ``configureInitializerEvaluationContext()`` in sub-classes of ``AbstractDrawioFactory``. 
In Drawio resource factories override ``getVariables()``.
Set variables from which the initializer expression would obtain semantic elements.

### type

``type`` property is used if there is no ``initializer`` or if it evaluated to ``null``. 
The value of property is the type of the semantic element. 
Types are looked up in the factory packages in the following way:

* If the value contains a hash (``# ``) then it is treated as a type URI. For example ``ecore://nasdanika.org/models/family#//Man``.
* If the value contains a dot (``.``) then it is treated as a qualified EClass name with EPackage prefix before the dot. For example ``family.Man``. There may be more than one dot if EClass is defined in a sub-package. For example, ``exec.content.Markdown``. 
* Otherwise, the value is treated as an unqualified EClass name - registered EPackages (and sub-packages recursively) are sorted by Namespace URI, iterated, and the first found EClass with matching name is used to create a semantic element.

A combination of ``initializer`` and ``type`` can be used for mapping in different contexts.
For example, when loading a stand-alone model ``initializer`` would evaluate to ``null`` and then ``type`` would be used. 
When the same diagram loaded in the context of a larger model, ``initializer`` may evaluate to a semantic element looked up in that larger model.

A Java analogy for ``type``:

```java
Object isaDiagramElement = getClassLoader().loadClass(type).newInstance();
```

### ref-id

``ref-id`` is some identifier to resolve to a semantic element. 

If there is already a semantic element created/resolved with ``initializer``, ``type``, or ``initializer-script`` and ``isRefIProxydURI()`` returns true, 
then ``ref-id`` is treated as EObject proxy URI resolved relative to the base URI (see below). 
You may need to use this approach in case of circular references between resources or if the semantic element type and URI are known, but the element itself is not available during the load time.

If there is no semantic element yet, then ``ref-id`` is used to look it up with ``getByRefId()`` method. 
In this case you would need to override ``getByRefId()`` method and define what the reference id means in your case.
For example, you may use semantic URI's to lookup elements. Say ``ssn:123-45-6789`` to lookup a person by their SSN.

Resource factories treat ``ref-id``'s as URI's resolved relative to the diagram element base URI. 
Resolved URI's are then passed to a URI resolver function provided in a factory constructor.
One option to implement this function is to call  ``ResourceSet.getEObject(URI uri, true)``.
``ref-id`` may be used in a top-down federation of diagrams - diagram elements may provide URI's of other diagrams as their ``ref-id``'s

### base-uri

This property is used to compute base URI for resolving references such as ``ref-id``, ``doc-ref``, and ``spec-ref``.
The base URI is resolved relative to the logical parent element up to the document (diagram) URI. 
For pages with links from diagram elements the logical parent is the first linking element.
For connections with sources, it is the connection source.
Otherwise, the element's container (``eContainer()``) is the logical parent.

One way to consistently set base URI's is to check "Placeholders" checkbox and then use ``%semantic-id%/`` or ``%id%/`` value for base URI.

### initializer-script

You can use initializer script if ``initializer`` expression and ``type`` do not provide enough expressive power to compute a semantic element.

Initializer script can be provided in ``initializer-script`` property or loaded from a resource URI specified in ``initializer-script-ref`` property.
The URI is resolved relative to the base URI of the diagram element.

The script has access to the following variables:

* ``baseURI``
* ``diagramElement``
* ``logicalParent``
* ``registry``
* ``semanticElement``

Additional variables can be introduced by overriding ``getVariables()`` or ``configureInitializerScriptEngine()`` methods.
Script class loader can be customized by overriding ``getClassLoader()`` method.

If there is more than one script engine, then ``initializer-script-engine`` boolean SpEL expression can be used to select an engine.
The expression is evaluated in the context of a candidate ``javax.script.ScriptEngineFactory`` with the following variables:

* ``diagramElement``
* ``registry``
* ``semanticElement``

If there is no ``initializer-script-engine`` expression and a script is loaded from a resource specified in ``initializer-script-ref``,
then the URI file extension (characters after the last dot) is used to match a script engine by extension..

As you can see, both script and engine selector expression have access to the ``semanticElement`` variable. 
This variable contains semantic element value produced by ``initializer`` or ``type``.
In other words, the initializer script is "chained" with ``initializer``/``type``. 

### page-element

If there is no semantic element for a diagram element yet and the diagram element's  ``page-element`` property is set to ``true`` then its semantic element is 
set to the first found semantic element of diagram elements linking to the page. 

On the [Internet Banking System Container Diagram](https://architecture.models.nasdanika.org/demo/internet-banking-system/references/elements/internet-banking-system/index.html) 
the "Internet Banking System" container is the page element. 
It "inherits" "Internet Banking System" semantic element from the [System Context Diagram](https://architecture.models.nasdanika.org/demo/internet-banking-system/index.html)
because the "Internet Banking System" diagram element on the System Context Diagram links to to the container diagram page.

There should be one page element per page. Having more than one may lead to an unpredictable behavior.
For the first top level page the page element becomes a document element. 
By default, a top level page is a page which is not linked from diagram elements. 

The default behavior can be modified by setting ``top-level-page`` property. 
``true`` value indicates that the page is a top level page even if it has incoming links.
Any other value is treated as ``false``. You can set this property to a falsey value to prevent a page from becoming a top level page.
For example, pages with prototypes or template elements.

Using ``page element`` you can define a high-level structure on a top level diagram page, link other pages to the diagram elements and 
refine their definitions.
This process can be repeated to build a hierarchy of pages as demonstrated in the "Internet Banking System Architecture" demo mentioned above.

If the semantic element of a page element extends [``NamedElement``](https://ncore.models.nasdanika.org/references/eClassifiers/NamedElement/index.html) then the page name is used as element's name if hasn't been already set by other means.

### prototype

Spring Expression Language (SpEL) expression evaluating to a diagram element.
The semantic element of that diagram element is copied and the copy is used as the semantic element of this diagram element. 
Also, prototype configuration (properties) is applied to this semantic element. 

Example: ``getDocument().getModelElementById('web-server-prototype')``

Prototypes allow to define common configuration in one element and then reuse it in other elements. 
For example, a web server prototype may define an icon and then all web server elements would inherit that configuration.
Prototypes can be chained - you may create an inheritance hierarchy of diagram elements. 

Drawio model classes provide convenience methods for finding diagram elements:

* [Document](https://drawio.models.nasdanika.org/references/eClassifiers/Document/operations.html):
    * ``getModelElementById(String id)``
    * ``getModelElementByProperty(String name, String value)``
    * ``getModelElementsByProperty(String name, String value)``
    * ``getPageById(String id)``
    * ``getPageByName(String name)``
* [Page](https://drawio.models.nasdanika.org/references/eClassifiers/Page/operations.html):
    * ``getModelElementById(String id)``
    * ``getModelElementByProperty(String name, String value)``
    * ``getModelElementsByProperty(String name, String value)``
    * ``getTag(String name)``
* [ModelElement](https://drawio.models.nasdanika.org/references/eClassifiers/ModelElement/operations.html):
    * ``getDocument()``
    * ``getPage()``
* [Root](https://drawio.models.nasdanika.org/references/eClassifiers/Root/operations.html), [Layer](https://drawio.models.nasdanika.org/references/eClassifiers/Layer/operations.html):    
    * ``getModelElementById(String id)``
    * ``getModelElementByProperty(String name, String value)``
    * ``getModelElementsByProperty(String name, String value)``

A prototype must have a semantic element defined - the loading process will keep evaluating the prototype expression until it returns non-null or until the maximum number of passes is exceeded. 
In the latter case the loading process would fail.
If you want to inherit just configuration, but not the semantic element, then use ``config-prototype`` property instead of ``prototype``.

### selector

``selector`` is Spring Expression Language (SpEL) expression evaluating to a diagram element.
The semantic element of that diagram element is used as the semantic element of this diagram element. 
Selectors allow to use the same semantic element on multiple diagrams. 

For example, in the "Internet Banking System" demo ``E-Mail System`` is defined on the System Context Diagram and selected (referenced) on the Container Diagram with ``getDocument().getModelElementByProperty('semantic-id', 'microsoft-exchange')`` expression.

The expression is evaluated in the context of the diagram element with access to a ``registry`` variable.
Override ``getVariables()`` to provide more variables to the evaluator.

Please note that you may also use the extended link syntax to associate more than one diagram element with a single semantic element.
If you are selecting by diagram element ``id`` or label, then the extending link syntax is preferable to using ``selector`` expression.

In the "Internet Banking System" C4 Model demo ``Single-Page Application`` is defined on the Container Diagram and linked from on the API Application Component Diagram with ``data:element/id,name,Container+Diagram/single-page-application)`` link.

### semantic-selector

Spring Expression Language (SpEL) expression evaluating to a semantic element.
Semantic selectors are similar to initializers with the following differences:

* Semantic selectors are evaluated after initializers
* An initializer may evaluate to ``null``, but a semantic selector must eventually evaluate to a non-null value
* An initializer is evaluated once, but a semantic selector might be evaluated multiple times until it returns a non-null value
* A semantic selector is only evaluated if there isn't a semantic element already

Semantic selectors can be used to evaluate semantic elements using semantic elements of other elements. 
For example, a semantic selector of a child node may need a semantic element of its parent to resolve its own semantic element.

The expression is evaluated in the context of the diagram element with access to a ``registry`` variable.
Override ``getVariables()`` to provide variables to the evaluator.

## References

Diagram elements can be associated with semantic elements' references. 
A Java analogy would be:

```java
List<Person> isaChildrenDiagramElement = familyWorkBook.findById("isa").getChildren();
```

``reference`` property associates a diagram element with a reference of the semantic element of the first matched logical ancestor for mapping purposes.
If the element has mapped descendants, their matching semantic elements are added to the reference.

Reference value can be a string or a map. The string form is equivalent to the map form with just the ``name`` entry.

The map form supports the following keys:

* ``comparator`` - used to sort reference elements, see ``comparator`` in "Feature Mapping".
* ``condition`` - a SpEL ``boolean`` expression evaluated in the context of the logical ancestor semantic element. If not provided, matches the first mapped ancestor. Has access to the following variables:
    * ``sourcePath`` - a list of logical ancestors starting with the logical parent
    * ``registry``
* ``expression`` - a SpEL ``EObject`` expression evaluated in the context of the logical ancestor semantic element. If not provided, the logical ancestor itself is used. Has access to the following variables:
    * ``sourcePath`` - a list of logical ancestors starting with the logical parent
    * ``registry``
* ``name`` - reference name
* ``element-condition`` - a SpEL ``boolean`` expression evaluated in the context of the descendant (contents) semantic element. If not provided, elements are matched by type compatibility with the reference type. Has access to the following variables:
    * ``sourcePath`` - source containment path
    * ``registry``
* ``element-expression`` - a SpEL ``EObject`` expression evaluated in the context of the descendant (contents) semantic element. If not provided, the descendant itself is used. Has access to the following variables:
    * ``sourcePath`` - source containment path
    * ``registry``
    
![school model](hogwarts.png "Hogwarts reference mapping")

In the above diagram "Faculty" and "Students" containers inside the "Hogwarts" container can be associated with School's references with the same name.
The "Hogwarts" container would be of type "School" and people images would be of type "Person".

## Wiring

During the wiring phase semantic element features are set according to feature mapping configuration as explained below and in the "Feature Mapping" chapter.
Feature map is a YAML map which can be specified in the ``feature-map`` property or in a resource specified in ``feature-map-ref`` property. 
Resource URI is resolved relative to the base URI of the element. 
Relatively small feature maps may be maintained in ``feature-map`` properties, but if they grow large or are maintained by people other than the mapper,
then it makes sense to externalize them and use ``feature-map-ref`` property and a feature mapping resource.

![wiring](wiring.png "Wiring (feature mapping) example")

The following sections explain feature map configuration keys and values using the above diagram to provide examples.
In the diagram we assume that:

* USA is of type ``Country`` and has a ``states`` reference
* Florida and Texas are of type ``State`` and have a ``people`` reference
* Jane and Joe are of type ``Person`` and have:
    * ``father`` and ``children`` opposite references. The father reference is single (multiplicity 0..1) and children reference is many (multiplicity 0..-1).
    * ``country`` reference
* Jane -> Joe connection doesn't have a semantic element in all examples except ``end`` and ``start``. For those examples it has a semantic element of type ``Father`` with ``child`` and ``father`` references.    

### container

Mapping specification for the container element in container/contents permutation.
Contains one or more of the following sub-keys with each containing a map of feature names to a mapping specification or a list of feature names.
The feature mapping specification is explained in the next chapter ("Feature Mapping")

* ``self`` - this element is a container 
* ``other`` - the other element is a container

#### Example

```yaml
container:
  other: states
  self: 
    people:
      argument-type: Person
      path: 1
```

The above example shows Florida's feature map:

* ``other`` means USA because USA contains Florida. This mapping specifies that this (Florida's) semantic element shall be added to the ``states`` feature of its container (USA) regardless of the containment path length. In the example the containment path length is ``1``.
* ``self`` means Florida itself. The mapping specifies that immediate children of this element (``path: 1``) shall be added to this (Florida) semantic element ``people`` collection if they are instances of ``Person``. Jane diagram element is an immediate child of Florida diagram element.

Please note that when a diagram element is linked to a page, then the page's page element is logically merged with that element. 
In the Internet Banking System Architecture ``GetBalanceRequest`` is contained by ``Internet Banking System`` with ``path=3`` - they appear on different diagrams (pages), but these diagrams are connected with page links.

### contents

Mapping specification for the contents element in container/contents permutation. 
Contains one or more of the following sub-keys with each containing a map of feature names to a mapping specification or a list of feature names.

* ``self`` - this element is contained by the other 
* ``other`` - the other element is contained by this element

#### Examples

##### USA

```yaml
contents:
  other: 
    country:
      path: 2
```

The above feature map for USA means that ``other`` is either Florida, Texas, Jane or Joe - they are all contained in USA directly or indirectly.
Path 2 means that only Jane and Joe match this mapping. And ``country`` means that their ``country`` reference shall be set to USA. 

##### Joe

```yaml
contents:
  self: 
    country:
      path: 2
```

The above feature map for Joe means that ``other`` is either Texas or USA - they both contain Joe, Texas directly and USA transitively.
Path 2 means that only USA matches this mapping. And ``country`` means that Joe's ``country`` reference shall be set to USA. 

### end

For connections - mapping specification for the connection end feature to map the connection target semantic element to a feature of the connection semantic element.

```yaml
end: father
```

The above specification means "set ``father`` reference of the connection semantic element to the semantic element of its end (Joe)".

### self

A map of feature names to Spring Expression Language (SpEL) expressions or a list of expressions evaluating to the feature value or feature element value.

The expression is evaluated in the context of the source diagram element and has access to the following variables:

* ``value`` - semantic element
* ``registry`` - a map of diagram element to semantic elements

### source

For connections - mapping specification for the connection source feature to map the connection semantic element to a feature of the connection source semantic element.
If the connection semantic element is ``null``, then the connection target semantic element is used instead.

```yaml
source: father
```

The above specification at the Jane -> Joe connection means to set connection source (Jane) ``father`` feature to connection target semantic element (Joe) because the connection doesn't have a semantic element. 
In pseudo-code:

```java
connection.getSource().setParent(connection.getTarget());
```

### start

For connections - mapping specification for the connection start feature to map the connection source semantic element to a feature of the connection semantic element.

```yaml
start: child
```

The above specification means "set ``child`` reference of the connection semantic element to the semantic element of its start (Jane)".

### target

For connections - mapping specification for the connection target feature to map the connection semantic element to a feature of the connection target semantic element.
If the connection semantic element is ``null``, then the connection source semantic element is used instead.

```yaml
target: children
```

The above specification at the Jane -> Joe connection means to set connection target (Joe) ``children`` feature to connection source semantic element (Jane) because the connection doesn't have a semantic element. 
In pseudo-code:

```java
connection.getTarget().getChildren().add(connection.getSource());
```

## Mapping selector

Mapping selector is used to select one or more semantic elements for feature mapping.
If it is not provided, then the diagram element's semantic element is used for feature mapping.

Mapping selector shall be a YAML document containing either a single string or a list of strings.
The strings are treated as Spring Expression Language (SpEL)] expression
evaluating to a semantic element or a collection of semantic elements to use for feature mapping.
Expressions are evaluated in the context of the diagram element and have access to a ``registry`` variable.

Mapping selectors may be used to associate multiple semantic elements with a diagram element for feature mapping purposes.

Mapping selector can be defined in ``mapping-selector`` property or in an external resource with URI specified in ``mapping-selector-ref`` property.
The resource URI is resolved relative to the base URI of the diagram element.
 
## Representations

For semantic elements which extend [ModelElement](https://ncore.models.nasdanika.org/references/eClassifiers/ModelElement/index.html)
the loading process injects representations which can be used in Markdown documentation as will be explained in the "Markdown" chapter and as icons in generated HTML documentation. 

The loading process injects two representations:

* ``drawio`` - a Drawio diagram containing pages where the page element maps to this semantic element.
* ``image`` - loaded from diagram element style ``image``. 

In the Sample Family demo people icons are scaled-down ``image`` representations and the diagram on the page is the ``drawio`` representation. 
The same approach is used in the "Internet Banking System" demo - element icons in the navigation panel are scaled images from diagrams

Representation reduce documentation effort and drive consistency.

## Configuration

After diagram elements are mapped to semantic elements (initialized) and wired (their features are mapped), they are configured using their diagram element properties 
as explained below.

### Configuration prototype

With ``config-prototype`` property you can inherit configuration from another diagram element.
Property value shall be a Spring Expression Language (SpEL) expression evaluating to a diagram element.
Diagram element configuration (properties) is applied to this semantic element. 

Example: ``getDocument().getModelElementById('web-server-prototype')``

Config prototypes allow to define common configuration in one element and then reuse it in other elements. 
For example, a web server prototype may define an icon and then all web server element would inherit that configuration.
Config prototypes can be chained - you may create an inheritance hierarchy of diagram elements. 

### Documentation

Documentation properties can be used to add documentation to semantic elements which implement [Documented](https://ncore.models.nasdanika.org/references/eClassifiers/Documented/index.html) interface.

Documentation can be provided in ``documentation`` property in Markdown, plain text, or HTML. 
Markdown is the default documentation format. 
You can modify it by setting ``doc-format`` property. Supported values are ``markdown``, ``text``, and ``html``.

It might be more convenient to maintain documentation in an external resource. 
In this case specify the documentation resource URI in ``doc-ref`` property. 
The resource URI is resolved relative to the base URI of the diagram element. 
If ``doc-format`` is not set, it is inferred from the resource extension - HTML for ``.htm`` and ``.html``, text for ``.txt``, Markdown otherwise. 

Documentation may also be configured via ``spec`` or ``spec-ref``.

### Label

If the semantic element extends [NamedElement](https://ncore.models.nasdanika.org/references/eClassifiers/NamedElement/index.html)
and its name is not set, then diagram element label converted to plain text is used as semantic element name.

### Markers

If the semantic element implements [Marked](https://ncore.models.nasdanika.org/references/eClassifiers/Marked/index.html) then the loading process
adds diagram element markers to the semantic element. 
It allows to track provenance of data elements.

### semantic-id

If the semantic element extends [``StringIdentity``](https://ncore.models.nasdanika.org/references/eClassifiers/StringIdentity/index.html), ``semantic-id`` property can be used to specify the ``id`` attribute.
If this property is not provided, then Drawio model element ID is used as a semantic ID.  
Drawio element id's are editable, but duplicate id's are not allowed on the same page. 
You may have duplicate semantic id's in different containers on the same page. 
In this case you may use ``semantic-id``. 
Semantic id can also be set using the ``spec`` and ``spec-ref`` YAML, this property is a shortcut way.

### Specification

Semantic elements may be configured by providing a YAML configuration map in the ``spec`` property
or a URI of configuration resource in the ``spec-ref`` property. The URI is resolved relative to the base URI of the diagram element.

Configuration YAML maps semantic element features (attributes and references) to their values as shown in the below example:

```yaml
location: ${base-uri}index.html
icon: https://docs.nasdanika.org/images/nasdanika-logo.png
children:
  - Action:
      location: ${base-uri}search.html
      icon: fas fa-search
      text: Search
      content:
        Interpolator:
          source:
            exec.content.Resource: classpath://org/nasdanika/html/model/app/gen/search.html
  - Action:
      location: ${base-uri}glossary.html
      text: Glossary
      content:
        Interpolator:
          source:
            exec.content.Resource: classpath://org/nasdanika/html/model/app/gen/semantic-info.html
navigation:
  - Action:
      text: Source
      icon: fab fa-github
      location: https://github.com/Nasdanika/nasdanika.github.io/  
```

Note singleton maps specifying child elements. 
The key of of such maps is a type as explained in the "Initialization" > "type" section:

* ``Action`` - a short type name, there is only one Action class available during loading
* ``Interpolator`` - also a short type name
* ``exec.content.Resource`` - a fully qualified type name because there are two ``Resource`` classes - in the ``content`` package and in the ``resources`` package

"Load specification" model documentation pages provide information about configuration keys supported by a specific type. 
Examples:

* Action class [Load Specification](https://html-app.models.nasdanika.org/references/eClassifiers/Action/load-specification.html)
* Woman class [Load Specification](https://family.models.nasdanika.org/references/eClassifiers/Woman/load-specification.html)

### Tooltip

If the semantic element extends [NamedElement](https://ncore.models.nasdanika.org/references/eClassifiers/ModelElement/index.html)
and its description is not set, then diagram element tooltip is used as semantic element description.

## Operations

Using ``operation-mapping`` and ``operation-mapping-ref`` properties you may specify 
semantic element operations to be invoked. 
``operations-mapping`` value shall be a YAML map of operation names to invocation specifications explained below,
``operation-mapping-ref`` shall be a URI of a resource containing a YAML map.
The URI is resolved relative to the diagram element base URI. 

![operations](operations.png "Operations example")

We will use the above example - the [Fox](https://nature.models.nasdanika.org/references/eClassifiers/Fox/index.html) [eats](https://nature.models.nasdanika.org/references/eClassifiers/Animal/references/eOperations/eats-1/index.html) the [Hare](https://nature.models.nasdanika.org/references/eClassifiers/Hare/index.html) after the Hare eats the [Grass](https://nature.models.nasdanika.org/references/eClassifiers/Grass/index.html).

The invocation specification is either a map or a list of maps. 
The sections below describe the keys supported by the maps.

### arguments

A map of parameter names to SpEL expression evaluating their values in the context of the iterator element (see below).
Argument names are used for operation selection/matching - a candidate operation must have parameters with matching names.  
The map does not have to contain entries for all operation parameter. 
Nulls are used as arguments for parameters which are not present in the map. 

For example, the Fox and Hare operation mapping may look like:

```
eats:
    food: registry.get(outgoing[0].target)
```

### iterator

An optional SpEL expression which returns a value to iterate over and invoke the operation for every element. 

* If the result is ``java.util.Iterator`` then it is used AS-IS.
* If the result is Iterable, Stream, or array, an iterator is created to iterate over the elements.
* If the result is ``null``, then an empty iterator is created. 
* Otherwise, a singleton iterator is created wrapping the result.

It allows to invoke the operation zero or more times.  
If not defined, the iterator contains the source diagram element. 

### pass

An optional integer specifying the pass in which this operation shall be invoked.
Use for ordering operation invocations. 

Because we want the Fox to eat the Hare after it eats the Grass, we need so set ``pass`` to ``1`` for the Fox:

```
eats:
    food: registry.get(outgoing[0].target)
    pass: 1
```

### selector

An optional SpEL boolean expression evaluated in the context of the operation to disambiguate overloaded operations.

## Script

Declarative mapping using properties, YAML, and expressions may not work in all cases - you may have conditional logic which cannot be expressed using declarative constructs.
Or, you may just prefer the imperative style. Or you may want to externalize all mapping to a single resource.
If so - use scripting!

You can provide script text in ``script`` property or URI of a script resource in ``script-ref`` property. 
The URI is resolved relative to the base URI of the diagram element. 

If there is more than one script engine available, ``script-entine`` boolean SpEL expression can be used for engine selection. 
The expression is evaluated in the context of a candidate ``javax.script.ScriptEngineFactory`` with the following variables:

* ``diagramElement``
* ``pass``
* ``registry``
* ``semanticElement``

If there is no ``script-engine`` expression and a script is loaded from a resource specified in ``script-ref``,
then the URI file extension (characters after the last dot) is used to match a script engine by extension..

Script engine has access to the following variables:

* ``baseURI``
* ``diagramElement``
* ``logicalParent``
* ``pass``
* ``registry``
* ``semanticElement``

Additional variables can be introduced by overriding ``getVariables()`` method.
Script class loader can be customized by overriding ``getClassLoader()`` method.

If the script evaluates to ``false`` it gets executed again in subsequent passes until it evaluates to any value which is not ``false``, including ``null``.
An exception is thrown if the maximum number of passes is exceeded.
Returning ``false`` can be used to indicate that some preconditions are not met in this pass, but will be met in subsequent passes.
For example, a script of one diagram element relies on a value set by a script of another diagram element.

## Processor

You may define reusable or complex configuration logic in Java. One way to do so it to use processors. 
Another way is to make the logic available to expressions and scripts via variables. 
 
``processor`` is a SpEL expression evaluating to zero or more [``org.nasdanika.drawio.model.util.AbstractDrawioFactory.Processor``](https://javadoc.io/doc/org.nasdanika.core/drawio-model/latest/org.nasdanika.drawio.model/org/nasdanika/drawio/model/util/AbstractDrawioFactory.Processor.html)s. 
The expression can evaluate to ``null``, a processor instance, iterator, iterable, array, or stream. 
It is evaluated in the context of the diagram element with ``semanticElement``, ``pass``, and ``registry`` variables.
Additional variables can be introduced by overriding ``getVariables()`` method.

Similar to scripts, processors may return false to indicate that they shall be invoked again in subsequent passes.

### Tag configuration

Tags can be configured by providing a YAML map of tag names to their configurations in the page root ``tag-spec`` property
or a resource URI in ``tag-spec-ref`` property. 
The URI is resolved relative to the base URI of the root.

To edit the root properties click on the page diagram canvas and then click "Edit Data" in the right panel or "Edit" menu. 
Or use ``Ctrl+M`` shortcut.

Map values are maps with keys corresponding to configuration property names described in this chapter.

#### Example

```yaml
rel-2024-1:
  type: Release
  spec:
    release-date: 2024/1/20
  contents: 
    other: releases  
```    

The above snippet specifies that for the tag ``rel-2024-1`` a semantic element of type ``Release`` shall be created
and its ``releaseDate`` feature shall be set to ``2024/1/20``.
The tag's semantic element shall be added to the ``releases`` reference of the elements tagged with this tag - for mapping purposes tagged elements are considered to be "contained" by tags.

## Namespaces

You can maintain multiple mappings at a single element using namespace prefixes.
In subclasses of ``AbstractDrawioFactory`` override ``getPropertyNamespace()`` method. 
By default this method returns an empty string which is used as a prefix for the configuration properties.



# Feature mapping

Feature mapping value can be either a string or a map. If it is a string it is treated as a singleton map to ``true`` (unconditional mapping).

The below two snippets are equivalent:

```yaml
container:
  other: elements
```

```yaml
container:
  other: 
    elements: true
```

The map value supports the following keys:

## argument-type

Specifies the type of feature elements to be set/added. 
String or a list of strings. Each string is a type name as defined in Mapping / Initialization / type section. 
Optionally prefixed with ``!`` for negation.
In the case of a list of strings the result is a logical OR - if any of elements matches.
Only instances of matching types will be set/added.

If absent, the feature type is used.
Argument type can be used to restrict elements to a specific subtype of the feature type.

Examples:

In the below snippet elements of type ``Transition`` and its sub-types are excluded from the elements.

```yaml
container:
  self: 
    elements:
      argument-type: "!Transition"
      path: 2
```

In this example only elements of type ``Person`` (and its sub-types) are added to the members feature.

```yaml
container:
  self: 
    members:
      argument-type: Person
```

This example is equivalent to the previous one, but lists ``Person`` sub-types ``Man`` and ``Woman`` explicitly.

```yaml
container:
  self: 
    members:
      argument-type:
        - Man
        - Woman
```

## comparator

Comparator is used for "many" features to order elements. 
A comparator instance is created by ``createComparator()`` method which can be overridden in subclasses to provide support for additional comparators.

The following comparators are provided "out of the box":

### Geometric

Diagram element position convey semantics. 
However, most diagramming tools ignore it. I.e. they lose information or force diagrammers to keep geometry and 
semantics in sync. Geometric comparators allow to order semantic elements according to the positions of their diagram elements - to semantize geometry. 

There are two classes of geometric comparators - Angular and Cartesian.

#### Angular

Angular comparators use angles to order elements. There are two flavors - ``clockwise`` and ``counterclockwise``.

##### clockwise

Compares elements by their angle relative to the node of the semantic element which holds the many reference. 
For example, in the [Living Beings](https://graph.models.nasdanika.org/demo/living-beings/index.html) demo "Bird", "Fish", and "Bacteria" are compared by their angle to the "Living Beings" with the angle counted from "12 o'clock" - 90 degrees (default).

Feature mapping with comparators of "Bird", "Fish", and "Bacteria" are defined at the connections from "Living Beings" as:

```yaml
source: 
  elements:
    comparator: clockwise
```

To specify the base angle other than 90 degree use the map version of comparator definition where ``clockwise`` is the key mapping to a number or string value. 
The number value is used as the angle value in degrees.
The string value is treated as a Spring Expression Language (SpEL) expression evaluated in the context of the "parent" node.
The expression may evaluate to a number or to a node.
In the latter case the result is used to compute the angle between the context node and the result node.

In the Living Beings example "Streptococcus", ..., "Staphyllococcus" are compared relative to the "Bacteria" node with the base angle being the angle between the "Bacteria" node and "Living Beings" node. 
As such, "Streptococcus" is the smallest node and "Staphyllococcus" is the largest.
With the default angle of 90 degrees "Lactobacyllus" would be the smallest and "Streptococcus" would be the largest.

Feature mapping with comparators of "Streptococcus", ..., "Staphyllococcus" is defined at connections from "Bacteria" to the respective genus nodes as:

```yaml
source: 
  elements:
    comparator: 
      clockwise: incoming[0].source
```

``incoming[0]`` evaluates to the connection from "Living Beings" to "Bacteria" and ``source`` evaluates to "Living Beings".

##### counterclockwise

Reverse of ``clockwise``.

#### Cartesian

Cartesian comparators use horizontal and vertical positions to compare/order elements with 2 coordinates and 2 directions in each it gives us 8 permutations.

##### down-left

Compares nodes by their vertical order first with higher nodes being smaller and then by horizontal order with nodes on the right being smaller.
Nodes are considered vertically equal if they vertically overlap.  

##### down-right

Compares nodes by their vertical order first with higher nodes being smaller and then by horizontal order with nodes on the left being smaller.
Nodes are considered vertically equal if they vertically overlap. 
This comparator can be used for org. charts.

##### left-down

Compares nodes by their horizontal order first with nodes on the right being smaller and then by vertical order with higher nodes being smaller.
Nodes are considered horizontally equal if they horizontally overlap.  

##### left-up

Compares nodes by their horizontal order first with nodes on the right being smaller and then by vertical order with lower nodes being smaller.
Nodes are considered horizontally equal if they horizontally overlap.  

##### right-down

Compares nodes by their horizontal order first with nodes on the left being smaller and then by vertical order with higher nodes being smaller.
Nodes are considered horizontally equal if they horizontally overlap.  

##### right-up

Compares nodes by their horizontal order first with nodes on the left being smaller and then by vertical order with lower nodes being smaller.
Nodes are considered horizontally equal if they horizontally overlap.  

##### up-left

Compares nodes by their vertical order first with lower nodes being smaller and then by horizontal order with nodes on the right being smaller.
Nodes are considered vertically equal if they vertically overlap.  

##### up-right

Compares nodes by their vertical order first with lower nodes being smaller and then by horizontal order with nodes on the left being smaller.
Nodes are considered vertically equal if they vertically overlap.  

### Dependency

``flow`` and ``reverse-flow`` order elements based on how they are connected to each other.

##### flow

If one element is reachable from the other by traversing connections, then the reachable element is larger than the source element.
In case of circular references the element with the smaller number of traversals to the other element is considered smaller. 
If elements are not connected they are compared by the fall-back comparator.
Flow comparator can be used for workflows and [PERT](https://en.wikipedia.org/wiki/Program_evaluation_and_review_technique) charts.

If this comparator's value is a String, then it is used as a name of the fallback comparator.
In the below example children in the [Sample Family](https://family.models.nasdanika.org/demos/mapping/) will be smaller than their parents and siblings will be compared using labels.

```yaml
container:
  self: 
    members:
      argument-type: Person
      comparator: 
        flow: label
```

If the value is a map, then it may have the following keys:

* ``condition`` - A boolean SpEL expression evaluated in the context of a connection being traversed. It may be used to traverse only connections which match the condition.
* ``fallback`` - Fallback comparator.

The below snippet shows the Internet Banking System Container diagram comparator: 

```yaml
container:
  self: 
    elements:
      path: 1
      comparator: 
        flow: 
          fallback: label
          condition: id != 'send-email'
```

The condition specified that a connection with ``sent-mail`` ID shall not be traversed.

![ibs-container-diagram](ibs-container-diagram.png "Container Diagram")

##### reverse-flow

Same as ``flow`` but with target nodes being smaller than source nodes.

### expression

A SpEL expression evaluated in the context of the feature element with ``other`` variable referencing the element to compare with. 
The expression has access to ``registry`` variable containing a map of diagram elements to semantic elements.

### key

A SpEL expression evaluated in the context of the feature element. 
The expression must return a value which would be used for comparison using the natural comparator.

### label

Uses the diagram element label converted to plain text as a sorting key. 
In the [Family mapping demo](https://family.models.nasdanika.org/demos/mapping/index.html) family members are sorted by label using the following feature map definition:

```yaml
container:
  self: 
    members:
      argument-type: Person
      comparator: label
```

### label-descending

Uses the diagram element label converted to plain text as a sorting key to compare in reverse alphabetical order. 

### natural

Uses the feature element's ``compareTo()`` method for comparable elements. Otherwise compares using the hash code. Nulls are greater than non-nulls.

### property

Uses diagram element property as a sorting key. A singleton map. For example:

```
property: label
```

### property-descending

The same as property, but compares in reverse alphabetical order.

## condition

A SpEL boolean expression evaluated in the context of the candidate diagram element with the following variables:

* ``value`` - semantic element of the candidate diagram element
*  ``path`` - containment path
* ``registry`` - a map of diagram element to semantic elements

## expression

A SpEL expression evaluating to a feature value in the context of the diagram element with with the following variables:

* ``value`` - semantic element of the diagram element
*  ``path`` - containment path
* ``registry`` - a map of diagram element to semantic elements

## greedy

Greedy is used with containment features and specifies what to do if a candidate object is already contained by another object:

* ``no-children`` - grab the object if it is contained by an ancestor of this semantic element. This is the default behavior.
* ``false`` - do not grab
* ``true`` - always grab

## path

Either an integer number or a list of boolean SpEL expressions to match the path. 
If an integer, then it is used to match path length as shown in the example below which matches only immediate children

```yaml
container:
  self: 
    elements:
      path: 1
```

If a list, then it matches if the list size is equal to the path length and each element evaluates to ``true`` in the context of a given path element.
Expressions have access to ``registry`` variable - a map of diagram elements to semantic elements.

## nop

If ``true`` then no mapping is performed and the chain mapper is not invoked. 
It can be used in scenarios with a default (chained) mapper to prevent the default behavior.

## position

A number specifying the position of the element in the feature collection.

## script

Script to evaluate the feature value. 
The script has access to the following variables:

* ``argument``
* ``argumentValue`` - feature value or value evaluated by ``expression``. 
* ``baseURI``
* ``context``
* ``registry``
* ``sourcePath``
* ``type``

Additional variables can be introduced by overriding ``getVariables()``  method.
Script class loader can be customized by overriding ``getClassLoader()`` method.

## script-engine

A boolean SpEL expression for selecting a script engine. 
The expression is evaluated in the context of a candidate ``javax.script.ScriptEngineFactory``.


If there is no ``script-engine`` expression and a script is loaded from a resource specified in ``script-ref``,
then the URI file extension (characters after the last dot) is used to match a script engine by extension..

## script-ref

Script reference resolved relative to the base URI.

## type

Type of the feature object to match. String as defined in Mapping > Initialization > type.
Can be used in ``other`` mappings.
 