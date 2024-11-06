[TOC levels=6]

## base-uri

This property is used to compute base URI for resolving references such as ``doc-ref`` and ``spec-ref``.
The base URI is resolved relative to the logical parent element up to the document (diagram) URI. 
For pages with links from diagram elements the logical parent is the first linking element.
For connections with sources, it is the connection source.
Otherwise, the element's container (``eContainer()``) is the logical parent.

One way to consistently set base URI's is to check "Placeholders" checkbox and then use ``%semantic-id%/`` or ``%id%`` value for base URI.

## config-prototype

See ``prototype`` below.

## documentation

Property value is used as documentation for semantic elements which extend ``Documented``. 

## doc-format

Documentation format - ``markdown`` (default), ``text``, or ``html``.
For ``doc-ref`` is derived from the reference extension if not set explicitly.

## doc-ref

Property value is used as a URI to load documentation for semantic elements which extend ``Documented``.
The URI is resolved relative to the ``base-uri``.

## feature-map

A YAML map defining how features of the semantic element and related semantic elements shall be mapped.

The map keys shall be one of the following:

### container

Mapping specification for the container element in container/contents permutation. Contains one or more of the following sub-keys with each containing a map of feature names to a mapping specification or a list of feature names.

* ``self`` - this element is a container 
* ``other`` - the other element is a container

#### Example

```yaml
container:
  other: elements
  self: 
    elements:
      argument-type: ArchitectureDescriptionElement
      path: 1
```

In the above example ``other`` specifies that this semantic element shall be added to the ``elements`` feature of its container regardless of the containment path length.
``self`` specifies that immediate children of this element (``path: 1``) shall be added to this semantic element ``elements`` collection if they are instances of ``ArchitectureDescriptionElement``

### contents

Mapping specification for the contents element in container/contents permutation. Contains one or more of the following sub-keys with each containing a map of feature names to a mapping specification or a list of feature names.

* ``self`` - this element is contained by the other 
* ``other`` - the other element is contained by this element

### end

For connections - mapping specification for the connection end feature to map the connection target semantic element to a feature of the connection semantic element.

### self

A map of feature names to a [Spring Expression Language (SpEL)](https://docs.spring.io/spring-framework/reference/core/expressions.html) expression or a list of expressions evaluating to the feature value or feature element value.

The expression is evaluated in the context of the source diagram element and has access to the following variables:

* ``value`` - semantic element
* ``registry`` - a map of diagram element to semantic elements

### source

For connections - mapping specification for the connection source feature to map the connection semantic element to a feature of the connection source semantic element.
If the connection semantic element is ``null``, then the connection target semantic element is used instead.

### start

For connections - mapping specification for the connection start feature to map the connection source semantic element to a feature of the connection semantic element.

### target

For connections - mapping specification for the connection target feature to map the connection semantic element to a feature of the connection target semantic element.
If the connection semantic element is ``null``, then the connection source semantic element is used instead.

## feature-map-ref

Property value is used as a URI to load feature map YAML specification.
The URI is resolved relative to the ``base-uri``.

## initializer

[Spring Expression Language (SpEL)](https://docs.spring.io/spring-framework/reference/core/expressions.html) expression evaluating to a semantic element.
Takes precedence over ``type``. May return ``null`` - in this case ``type`` would be used to create a semantic element, if specified.

This property can be used, for example, to look-up semantic elements defined elsewhere. 
Say, a list of family members can be defined in an MS Excel workbook, but family relationships in a diagram.

Another example is "progressive enrichment". For example, high-level architecture is defined in some model and a diagram uses 
initializers for already defined architecture elements and ``type`` for their sub-elements. 
This approach can be applied multiple times similar to how Docker images are assembled from layers and base images.

In order to implement lookup initializers, override ``getVariables()`` or ``configureInitializerEvaluationContext()`` in sub-classes of ``AbstractDrawioFactory``. 
In Drawio resource factories override ``getVariables()``.
Set variables from which the initializer expression would obtain semantic elements.

## initializer-script

Initializer script to execute. 
The script has access to the following variables:

* ``baseURI``
* ``diagramElement``
* ``logicalParent``
* ``registry``
* ``semanticElement``

Additional variables can be introduced by overriding ``getVariables()`` or ``configureInitializerScriptEngine()`` methods.
Script class loader can be customized by overriding ``getClassLoader()`` method.

## initializer-script-engine

A boolean SpEL expression for selecting a script engine. 
The expression is evaluated in the context of a candidate ``javax.script.ScriptEngineFactory`` with the following variables:

* ``diagramElement``
* ``registry``
* ``semanticElement``

If there is no ``initializer-script-engine`` expression and a script is loaded from a resource specified in ``initializer-script-ref``,
then the URI file extension (characters after the last dot) is used to match a script engine by extension.

## initializer-script-ref

Initializer script reference resolved relative to the base URI.

## mapping-selector

Mapping selector is used to select one or more semantic elements for feature mapping.
If it is not provided, then the diagram element's semantic element is used for feature mapping.

Mapping selector shall be a YAML document containing either a single string or a list of strings.
The strings are treated as [Spring Expression Language (SpEL)](https://docs.spring.io/spring-framework/reference/core/expressions.html) expression
evaluating to a semantic element or a collection of elements to use for feature mapping.
Expressions are evaluated in the context of the diagram element and have access to a ``registry`` variable.

Mapping selectors may be used to associate multiple semantic elements with a diagram element for feature mapping purposes.

## mapping-selector-ref

Property value is used as a URI to load mapping selector YAML.
The URI is resolved relative to the ``base-uri``.

## operation-mapping

Allows to invoke semantic element operations for mapping purposes.
For example, if a ``Course`` diagram element contains ``Student`` elements then ``Course.enroll(Student)`` operation may be used for mapping.
The operation may check for course capacity, validate student status, etc.

Operation mapping is a map of operation names to operation specification.
Operation specification can be either a map or a list of maps. The first case is treated as a singleton list.

The operation specification map supports the following entries:

### arguments

A map of parameter names to SpEL expression evaluating their values in the context of the iterator element (see below).
Argument names are used for operation selection/matching - a candidate operation must have parameters with matching names.  
The map does not have to contain entries for all operation parameter. 
Nulls are used as arguments for parameters which are not present in the map. 

### iterator

An optional SpEL expression which returns a value to iterate over and invoke the operation for every element. 
If the result is ``java.util.Iterator`` then it is used AS-IS.
If the result is Iterable, Stream, or array, an iterator is created to iterate over the elements.
If the result is null, then an empty iterator is created. 
Otherwise, a singleton iterator is created wrapping the result.

It allows to invoke the operation zero or more times. E.g. ``enroll`` for every student. 
If not defined, the iterator contains the source diagram element. 

### pass

An optional integer specifying the pass in which this operation shall be invoked.
Use for ordering operation invocations. 

For example, in the [Nature model](https://nature.models.nasdanika.org/index.html) you may want the [Fox](https://nature.models.nasdanika.org/references/eClassifiers/Fox/index.html) to [eat](https://nature.models.nasdanika.org/references/eClassifiers/Animal/references/eOperations/eats-1/index.html)
the [Hare](https://nature.models.nasdanika.org/references/eClassifiers/Hare/index.html), but only after the Hare eats the [Grass](https://nature.models.nasdanika.org/references/eClassifiers/Grass/index.html).
In this case you don't specify the pass for ``Rabbit.eats()``, so it defaults to zero.
An you set the pass for ``Fox.eats()`` to ``1``.

### selector

An optional SpEL boolean expression evaluated in the context of the operation to disambiguate overloaded operations.

## operation-mapping-ref

Property value is used as a URI to load operation mapping YAML.
The URI is resolved relative to the ``base-uri``.

## page-element

``true`` is for true value and any other value or absence of the property is considered false. 
There should be one page element per page. Having more than one may lead to an unpredictable behavior.
For the first top level page the page element becomes a document element.
For pages linked from model elements the page element is logically merged with the linking element.

This allows to define a high-level structure on a top level diagram page, link other pages to the diagram elements and 
refine their definitions. 

If the semantic element of a page element extends ``NamedElement`` then the page name is used as element name if it is not already set
by other means.

## processor

A SpEL expression evaluating to zero or more ``org.nasdanika.drawio.model.util.AbstractDrawioFactory.Processor``s. 
The expression can evaluate to ``null``, a processor instance, iterator, iterable, array, or stream. 
It is evaluated in the context of the diagram element with ``semanticElement``, ``pass``, and ``registry`` variables.
Additional variables can be introduced by overriding ``getVariables()`` method.

## prototype

[Spring Expression Language (SpEL)](https://docs.spring.io/spring-framework/reference/core/expressions.html) expression evaluating to a diagram element.
The semantic element of that diagram element is copied and the copy is used as the semantic element of this diagram element. 
Also, prototype configuration (properties) is applied to this semantic element. 
Prototypes can be chained. 

Example: ``getDocument().getModelElementById('6ycP1ahp__4fXEwP3E-2-5')``

Prototypes allow to define common configuration in one element and then reuse it in other elements. 
For example, a web server prototype may define an icon and then all web server elements would inherit that configuration.
Prototypes can be chained - you may create an inheritance hierarchy for diagram elements. 

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

A prototype must have a semantic element. 
If you want to inherit just configuration, but not the semantic element, then use ``config-prototype`` property instead of ``prototype``.

## ref-id

Some identifier to resolve a semantic element. 

If there is already a semantic element created/resolved with ``initlizer``, ``type``, or ``initializer-script`` and ``isRefIProxydURI()`` returns true, 
then ``ref-id`` is treated as EObject proxy URI resolved relative to the base URI. 

Otherwise it is used to look up a semantic element with ``getByRefId()`` method. In this case you would need to override ``getByRefId()`` method and define what the reference id means in your case.
For example, you may use semantic URI's to lookup elements. Say ``ssn:123-45-6789`` to lookup a person by SSN.

Resource factories treat ref-id's as URI's resolved relative to the diagram element base URI. 
Resolved URI's are then passed to ``ResourceSet.getEObject(URI uri, true)``.

## reference 

Associates a diagram element with a reference of the semantic element of the first matched logical ancestor for mapping purposes.
If the element has mapped descendants, their matching semantic elements are added to the reference.

Reference value can be a string or a map. The string form is equivalent to the map form with just the ``name`` entry.

The map form supports the following keys:

* ``comparator`` - see ``comparator`` in "Feature mapping reference".
* ``condition`` - a SpEL ``boolean`` expression evaluated in the context of the logical ancestor semantic element. If not provided, matches the first mapped ancestor. Has access to the following variables:
    * ``sourcePath`` - a list of logical ancestors starting with the logical parent
    * ``registry``
* ``expression`` - a SpEL ``EObject`` expression evaluated in the context of the logical ancestor semantic element. If not provided, the logical ancestor itself is used. Has access to the following variables:
    * ``sourcePath`` - a list of logical ancestors starting with the logical parent
    * ``registry``
* ``name`` - reference name
* ``element-condition`` - a SpEL ``boolean`` expression evaluated in the context of the descendant (contents) semantic element. If not provided, elements are matched by type. Has access to the following variables:
    * ``sourcePath`` - source containment path
    * ``registry``
* ``element-expression`` - a SpEL ``EObject`` expression evaluated in the context of the descendant (contents) semantic element. If not provided, the descendant itself is used. Has access to the following variables:
    * ``sourcePath`` - source containment path
    * ``registry``

## script

Script to execute. 
The script has access to the following variables:

* ``baseURI``
* ``diagramElement``
* ``logicalParent``
* ``pass``
* ``registry``
* ``semanticElement``

Additional variables can be introduced by overriding ``getVariables()`` or ``configureScriptEngine()`` method.
Script class loader can be customized by overriding ``getClassLoader()`` method.
If the script evaluates to ``false`` it is executed again in subsequent passes until it evaluates to any value other than ``false``, including ``null``.
An exception is thrown if the maximum number of passes is exceeded.

## script-engine

A boolean SpEL expression for selecting a script engine. 
The expression is evaluated in the context of a candidate ``javax.script.ScriptEngineFactory`` with the following variables:

* ``diagramElement``
* ``pass``
* ``registry``
* ``semanticElement``

If there is no ``script-engine`` expression and a script is loaded from a resource specified in ``script-ref``,
then the URI file extension (characters after the last dot) is used to match a script engine by extension.

## script-ref

Script reference resolved relative to the base URI.

## selector

[Spring Expression Language (SpEL)](https://docs.spring.io/spring-framework/reference/core/expressions.html) expression evaluating to a diagram element.
The semantic element of that diagram element is used as the semantic element of this diagram element. 

Example: ``getDocument().getModelElementById('6ycP1ahp__4fXEwP3E-2-5')``

Selectors allow to use the same semantic element on multiple diagrams. 
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

## semantic-id

If the semantic element extends ``StringIdentity``, ``semantic-id`` property can be used to specify the ``id`` attribute.
If this property is not provided, then Drawio model element ID is used as a semantic ID.  

## semantic-selector

[Spring Expression Language (SpEL)](https://docs.spring.io/spring-framework/reference/core/expressions.html) expression evaluating to a semantic element.

Semantic selectors are similar to initializers with the following differences:

* Semantic selectors are evaluated after initializers
* An initializer may evaluate to ``null``, but a semantic selector must eventually evaluate to a non-null value
* An initializer is evaluated once, but a semantic selector might be evaluated multiples time until it returns a non-null value
* A semantic selector is only evaluated if there isn't a semantic element already

Semantic selectors can be used to evaluate semantic elements using semantic elements of other elements. 
For example, a semantic selector of a child node may need a semantic element of its parent to resolve its own semantic element.

Override ``getVariables()`` to provide variables to the evaluator.

## spec

Loads semantic element from the property value YAML using EObjectLoader. 

Example:

```yaml
icon: fas fa-user
```

## spec-ref

Loads semantic element from the property value URI using EObjectLoader. 
The URI is resolved relative to the ``base-uri``.

### tag-spec

TODO

### tag-spec-ref

TODO

## top-level-page

Page elements from top level pages are mapped to the document semantic element.
By default a page without incoming links from other pages is considered to be a top level page.
This property allows to override this behavior. 
``true`` value indicates that the page is a top level page. Any other value is treated as ``false``.

## type

Type of the semantic element. Types are looked up in the factory packages in the following way:

* If the value contains a hash (``# ``) then it is treated as a type URI. For example ``ecore://nasdanika.org/models/family#//Man``.
* If the value contains a dot (``.``) then it is treated as a qualified EClass name with EPackage prefix before the dot. For example ``family.Man``. May contain more than one dot if EClass is defined in a sub-package. For example, ``exec.content.Markdown``. 
* Otherwise, the value is treated as an unqualified EClass name - EPackages are iterated in the order of their registration and the first found EClass with matching name is used to create a semantic element. 

Type is used to create a semantic element if there is no ``initializer`` or the initializer expression evaluated to ``null``.
A combination of ``initializer`` and ``type`` can be used for mapping in different contexts.
For example, when loading a stand-alone model ``initializer`` would evaluate to ``null`` and then ``type`` would be used. 
When the same diagram loaded in the context of a larger model, ``initializer`` may evaluate to a semantic element looked up in that larger model.

## Namespaces

It is possible to maintain multiple mappings at a single element using namespace prefixes.
In subclasses of ``AbstractDrawioFactory`` override ``getPropertyNamespace()`` method. 
By default this method returns an empty string which is used as a prefix for the configuration properties.
