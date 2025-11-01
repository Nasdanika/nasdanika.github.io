(Semantic) mapping is a process of creating and populating Ecore models from other data sources, [Drawio diagrams](../drawio/index.html) in particular.
This module provides base mapping functionality and the Drawio module provides concrete implementation classes and several Drawio-specific comparators which use element properties and geometry.

This page provides a combined documentation for both generic and Drawio-specific mapping. 

Resources:

* [Semantic Mapping Medium Story](https://medium.com/nasdanika/semantic-mapping-3ccbef5d6c70) provides a high-level overview focusing more on the WHAT and the WHY while this page focuses more on the HOW.
* Most code snippets below are taken from the [semantic mapping demo](https://nasdanika-demos.github.io/semantic-mapping/index.html).

[TOC levels=6]

## EMF Ecore

This section is a brief introduction to EMF Ecore, which is used to create metamodels of problem domains.
These metamodels are then used as mapping targets. 
It is important to mention that metamodels and documentation generated from them have value on their own - they establish a common (ubiquitous) language.[^ubiquitous_language]

[^ubiquitous_language]: https://martinfowler.com/bliki/UbiquitousLanguage.html  

**Eclipse Modeling Framework (EMF)** is an Eclipse-based modeling framework and code generation facility for building tools and other applications based on a structured data model.
**Ecore** is the core (meta-)model at the heart of EMF. It allows expressing other models by leveraging its constructs. Ecore is also its own metamodel (i.e.: Ecore is defined in terms of itself).[^wikipedia-emf]

[^wikipedia-emf]: https://en.wikipedia.org/wiki/Eclipse_Modeling_Framework

Simply put, Ecore is a way to create (domain specific) languages understandable by both humans and computers.

```drawio-resource
./core/mapping/ecore.drawio
```

The above diagrams shows key Ecore concepts and their relationships. 
``E`` prefix is dropped for clarity. E.g. ``EPackage`` is shown as ``Package``.
More detailed documentation can be found here - https://ecore.models.nasdanika.org/ (work in progress)

* Metamodel
    * **Module** - Java/Maven module or an OSGi bundle. Contains zero or more models with each model containing a root package.
    * **Package** - a group of classifiers and sub-packages. Packages are identified (keyed) by namespace URI's.
    * **Classifier** - a class, data type or enumeration.
    * **Data type** - a bridge to the Java type system.
    * **Enumeration** - a collection of literals.
    * **Class** - contains zero or more structural features - attributes and references and zero or more operations. May inherit from zero or more superclasses. Can be abstract and can be an interface.
    * **Structural feature** - can hold single or multiple values
        * **Attribute** - holds a "simple" value such as String, Date, number.
        * **Reference** - relationship between two objects (EObjects). Unidirectional, but can be associated with another reference (opposite) to form a bi-directional relationship. References can be containment (composition) and non-containment (aggregation). 
* Model
    * **Resource set** - a group of related resources identified by a URI. Resource sets have associated packages, resource factories, adapter factories, and URI handlers.
    * **Resource** - a group of objects. A resource is identified by a URI.
    * **Model Element** - an instance of a metamodel class.
    
Below is a concrete example using the Family [metamodel](https://family.models.nasdanika.org/) and [model](https://family.models.nasdanika.org/demos/mapping/):

* Metamodel
    * **Module** - ``org.nasdanika.models.family:model`` Maven module, ``org.nasdanika.models.family`` Java module.
    * **Package** - ``family`` defined in ``family.ecore`` resource with ``ecore://nasdanika.org/models/family`` namespace URI, several Java packages.
    * **Class** - ``Man`` class inheriting from ``Person`` class.
    * Structural feature
        * **Attribute** - ``name`` attribute of ``Man`` (inherited from ``NamedElement``).
        * **Reference**
            * ``Person.father`` - single non-containment reference, 
            * ``Person.children`` - many non-containment reference.
            * ``Family.members`` - many containment reference.
            
* Model
    * **Resource set** - created with a factory which treats ``.drawio`` diagram files as resources with diagram elements mapped to the family model.
    * **Resource** - ``family.drawio`` file.
    * **Model Element** - ``Paul`` and ``Elias`` are instances of ``Man`` class. ``Elias`` references ``Paul`` as his father. 
               
It is important to note that resources are identified by URI's, not URL's. 
It allows to load resources from multiple data sources - files, URL's, databases, source repositories such as Git, binary repositories such as Maven.
Using Nasdanika classes it is also possible to load objects from multiple sources on access.
For example, a person's id and name can be loaded from, say, Excel file or a database. 
Some other attributes may be loaded by making HTTP requests only if the client code reads those attributes.

EMF Ecore provides facilities to read/write resources from/to XMI files and binary files.
It also provides [tools](https://eclipse.dev/ecoretools/) for creating models.      
Eclipse CDO allows to store models in distributed repositories.

Nasdanika provides factories to load models from Drawio diagrams, MS Excel, Java sources, PDF files, and CSV files.
It also provides URI handlers for loading models from classpath resources, GitLab & Maven repositories. 
There is also a documentation generator for Ecore models. 
In addition to the family model mentioned above, you can find examples of Ecore models and generated documentation on this site.              

## ContentProvider

For the purposes of mapping the source structures are abstracted by the [ContentProvider](https://github.com/Nasdanika/core/blob/master/mapping/src/main/java/org/nasdanika/mapping/ContentProvider.java) interface, whith the following methods:

* ``Collection<? extends S> getChildren(S element /*, Predicate<S> predicate */)`` - returns element children. 
* ``URI getBaseURI(S element)`` - URI for resolving resources such as documentation.
* ``Object getProperty(S element, String property)`` - returns property used to perform mapping.
* ``Marked asMarked(S element)`` - for reporting locations of errors.
* ``S getConnectionSource(S element)`` - If the element is a connection/association - returns its source.  Otherwise returns null.
* ``S getConnectionTarget(S element)`` - If the argument is a connection/association - returns its target. Otherwise returns null.
* ``String getName(S element)`` - Element name, e.g. Drawio element label text without HTML markup.
* ``String getDescription(S element)`` - Element description, e.g. Drawio element tooltip.
* ``Object getIdentity(S obj)`` - Object identity such as a unique ID or a URI. 


## DrawioContentProvider

[DrawioContentProvider](https://github.com/Nasdanika/core/blob/master/drawio/src/main/java/org/nasdanika/drawio/emf/DrawioContentProvider.java) is an implementation of the ``ContentProvider`` for working with Drawio diagrams. 
It loads mapping properties from YAML in ``config`` property or a YAML resource specified in ``config-ref`` property. 
It also takes element and page links into account in parent/child relationships - linked elements are considered children of linking elements.

DrawioContentProvider uses ``base-uri`` property for resolving the element base URI. 
If this property is not set, then the base URI is the URI of the drawio file plus a fragment, which is not significant for resolving relative URI's.
If ``base-uri`` property is set, then it is resolved relative to the parent's base URI or the Drawio resource URI if there is no parent.

## Loading Drawio resources

While you can use DrawioContentProvider directly and customize it to your needs, the primary usage scenario is to load Drawio diagrams as Ecore resources - mapping is performed behind the scenes:

```java
CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
Requirement<ResourceSetRequirement, ResourceSet> requirement = ServiceCapabilityFactory.createRequirement(ResourceSet.class);		
ResourceSet resourceSet = capabilityLoader.loadOne(requirement, progressMonitor);
File diagramFile = new File("diagram.drawio").getCanonicalFile();
Resource resource = resourceSet.getResource(URI.createFileURI(diagramFile.getAbsolutePath()), true);		
EObject root = resource.getContents().get(0);	
``` 

In the above code snippet a resource set is loaded from a CapabilityLoader and contains all resource factories and EPackages provided as capabilities.
It includes [ConfigurationLoadingDrawioResourceFactory](https://github.com/Nasdanika/core/blob/master/emf/src/main/java/org/nasdanika/emf/ConfigurationLoadingDrawioResourceFactory.java) which is registered to load ``.drawio`` and ``.png`` files.
This factory customizes mapping properties:

* ``mapping`` - if this property is set, its value is parsed as YAML which is used as property source.
* ``mapping-ref`` - if ``mapping`` property is not set and this property is set, then this property value treated as a location of a YAML resource containing mapping configuration. The location (URI) is resolved relative to the base URI of the element.

## Phases & properties

Mapping is a non-trivial process. 
In the case of diagrams the order in which diagram elements are mapped is not under control of the user - it depends on the order of creation of the diagram elements.
Therefore, the mapping process consists of several phases and some of them may involve multiple passes.
This section describes the mapping process in the order of phases. 
A phase section explains element configuration properties (keys) used for that phase.
In some phases only one property is used. In this case the phase name is the same as the property name.
In the mapping reference pages properties are ordered alphabetically.

### Initialization

In this first phase source elements are associated with (mapped to) target elements. 
For example, a person image can be associated with a person object from the family model. 
The phase is called "Initialization" because the mapping process is similar to assigning a value to a variable 
in languages like Java and virtually identical to what happens in JavaScript where objects are essentially maps.

The following sections explain the configuration properties used in the initialization phase.

#### initializer

``initializer`` property value shall be an [Invocable URI](../capability/index.html#loading-invocables-from-uris) resolved relative to the base URI.
The invocable is bound to the following arguments by name:

* ``registry`` - ``Consumer<BiConsumer<Map<EObject, EObject>,ProgressMonitor>>`` callback to obtain a map of source elements to target elements once all target elements are initialized (created), but not necessarily fully configured.
* ``contentProvider`` - content provider
* ``progressMonitor`` - progress monitor
* ``resourceSet`` - can be used to load target model elements
* ``capabilityLoader`` - can be used to load [capabilities](https://docs.nasdanika.org/core/capability/index.html), including [invocable URIs](https://docs.nasdanika.org/core/capability/index.html#loading-invocables-from-uris)

Then it is invoked with the source object as a single positional argument.

It may return ``null`` - in this case ``type`` would be used to create a target element, if specified.

This property can be used, for example, to look-up target elements defined elsewhere. 
Say, a list of family members can be defined in an MS Excel workbook, but family relationships in a diagram.

Another example is "progressive enrichment". 
For example, high-level architecture is defined in some model and a diagram uses 
initializers for already defined architecture elements and ``type`` for their sub-elements. 
This approach can be applied multiple times similar to how Docker images are assembled from layers and base images - you can layer fine-grained models/diagrams over coarse-grained ones.
If you are old enough to remember JPEG images being loaded over a slow dial-up connection - something like that.

In order to implement lookup initializers, override ``configureInitializer()``, ``configureInvocable()`` or ``getVariables()`` in sub-classes of ``AbstractMappingFactory`` or ``getVariables()`` in a subclass of ``ConfigurationLoadingDrawioResourceFactory``. 

##### Groovy initializer

###### Mapping

```yaml
initializer: initializer.groovy
```

###### Script

```groovy
import org.nasdanika.models.architecture.ArchitectureFactory

ArchitectureFactory.eINSTANCE.createDomain();
```

##### Java intitializer

###### Mapping

```yaml
initializer: data:java/org.nasdanika.demos.diagrams.mapping.Services::nodeInitializer
```

###### Initializer method

```java
public static ArchitectureDescriptionElement nodeInitializer(
		CapabilityFactory.Loader loader, 
		ProgressMonitor loadingProgressMonitor, 
		byte[] binding,
		String fragment,
		@Parameter(name = "contentProvider") ContentProvider<Element> contentProvider,
		@Parameter(name = "registry") Consumer<BiConsumer<Map<EObject, EObject>,ProgressMonitor>> registry,
		@Parameter(name = "progressMonitor") ProgressMonitor mappingProgressMonitor,
		@Parameter(name = "resourceSet") ResourceSet resourceSet,
		@Parameter(name = "capabilityLoader") CapabilityLoader capabilityLoader,
		Element source) {
	ArchitectureDescriptionElement architectureDescriptionElement = ArchitectureFactory.eINSTANCE.createArchitectureDescriptionElement();
	architectureDescriptionElement.setDescription("I was created by a Java initializer");
	return architectureDescriptionElement;				
}
```

Note the use of positional and named parameters:

* The first 4 parameters are bound positionally when invocable URI is loaded
* The 5 named parameters a bound by name by the mapper
* The last parameter is used for invocation (also positional)

#### type

``type`` property is used if there is no ``initializer`` or if it returned ``null``. 
The value of property is the type of the target element. 
Types are looked up in the factory packages in the following way:

* If the value contains a hash (``# ``) then it is treated as a type URI. For example ``ecore://nasdanika.org/models/family#//Man``.
* If the value contains a dot (``.``) then it is treated as a qualified EClass name with EPackage prefix before the dot. For example ``family.Man``. There may be more than one dot if EClass is defined in a sub-package. For example, ``exec.content.Markdown``. 
* Otherwise, the value is treated as an unqualified EClass name - registered EPackages (and sub-packages recursively) are sorted by Namespace URI, iterated, and the first found EClass with matching name is used to create a target element.

A combination of ``initializer`` and ``type`` can be used for mapping in different contexts.
For example, when loading a stand-alone model ``initializer`` would evaluate to ``null`` and then ``type`` would be used. 
When the same diagram loaded in the context of a larger model, ``initializer`` may evaluate to a target element looked up in that larger model.

Example:

```yaml
type: architecture.c4.System
```

A Java analogy for ``type``:

```java
Object isaDiagramElement = getClassLoader().loadClass(type).newInstance();
```

#### ref-id

``ref-id`` is some identifier to resolve to a target element. 

If there is already a target element created/resolved with ``initializer`` or ``type`` and ``isRefIProxydURI()`` returns true, 
then ``ref-id`` is treated as [EObject proxy](https://javadoc.io/static/org.eclipse.emf/org.eclipse.emf.ecore/2.37.0/org/eclipse/emf/ecore/EObject.html#eIsProxy()) URI resolved relative to the base URI (see below). 
You may need to use this approach in case of circular references between resources or if the target element type and URI are known, but the element itself is not available during the load time.

If there is no target element yet, then ``ref-id`` is used to look it up in the resource set. 
You may use a "physical" URI to load objects on demand, or "logical"/"semantic" URI for already loaded objects.
Say, ``ssn:123-45-6789`` to lookup a person by their SSN.

#### Contributor.initialize()

Initialization can be customized by creating a service capability of type ``AbstractMappingFactory.Contriutor`` and overriding its ``initialize()`` method.

### page-element

If there is no target element for a diagram element yet and the diagram element's ``page-element`` property is set to ``true`` then its target element is 
set to the first found target element of diagram elements linking to the page. 

For example, on the [System Context Diagram](https://nasdanika-demos.github.io/internet-banking-system-c4/cerulean/index.html) the "Internet Banking System" element links to the [Container diagram](https://nasdanika-demos.github.io/internet-banking-system-c4/cerulean/references/elements/internet-banking-system/index.html) page where the "Internet Banking System" container is the page element.
As a result, both of these diagram elements map to the same target element.

There should be one page element per page. Having more than one may lead to an unpredictable behavior.

Using ``page-element`` you can define a high-level structure on one diagram page, link other pages to the diagram elements and 
refine their definitions.
This process can be repeated to build a hierarchy of pages as demonstrated in the "Internet Banking System Architecture" demo mentioned above.

If the target element of a page element extends [``NamedElement``](https://ncore.models.nasdanika.org/references/eClassifiers/NamedElement/index.html) then the page name is used as element's name if hasn't been already set by other means.

### prototype

``prototype`` is a [Spring Expression Language](https://docs.spring.io/spring-framework/reference/core/expressions.html) (SpEL) expression evaluating to a diagram element.
The target element of that diagram element is copied and the copy is used as the target element of this diagram element. 
Also, the prototype configuration (properties) is applied to this target element. 

Example: ``getPage().getDocument().getModelElementById('web-server-prototype')``

Prototypes allow to define common configuration in one element and then reuse it in other elements. 
For example, a web server prototype may define an icon and then all web server elements would inherit that configuration.
Prototypes can be chained - you may create an inheritance hierarchy of diagram elements. 

Drawio classes provide convenience methods for finding diagram elements:

* ``Element.getModelElementById(String id)``
* ``Element.getModelElementByProperty(String name, String value)``
* ``Element.getModelElementsByProperty(String name, String value)``
* ``Document.getPageById(String id)``
* ``Document.getPageByName(String name)``

If you want to inherit just configuration, but not the target element, then use ``config-prototype`` property instead of ``prototype``.

### selector

``selector`` is a Spring Expression Language (SpEL) expression evaluating to a diagram element.
The target element of that diagram element is used as the target element of this diagram element. 
Selectors allow to use the same target element on multiple diagrams. 

For example, in the [Internet Banking System](https://nasdanika-demos.github.io/internet-banking-system-c4/cerulean/index.html) [E-Mail System](https://nasdanika-demos.github.io/internet-banking-system-c4/cerulean/references/elements/microsoft-exchange/index.html) is defined on the System Context Diagram and selected (referenced) on the [Container Diagram](https://nasdanika-demos.github.io/internet-banking-system-c4/cerulean/references/elements/internet-banking-system/index.html) with ``getModel().getPage().getDocument().getModelElementByProperty('semantic-id', 'microsoft-exchange')`` expression.

The expression is evaluated in the context of the diagram element with access to the following variables:

* ``registry``
* ``pass``
* ``progressMonitor``
* ``resourceSet``
* ``capabilityLoader``

Please note that you may also use the [extended link syntax](../drawio/index.html#page-and-element-links) to associate more than one diagram element with a single target element.
If you are selecting by diagram element ``id`` or label, then the extended link syntax is preferable to using ``selector`` expression.

In the "Internet Banking System" C4 Model demo ``Single-Page Application`` is defined on the Container Diagram and linked from the [API Application Component Diagram](https://nasdanika-demos.github.io/internet-banking-system-c4/cerulean/references/elements/internet-banking-system/references/elements/api-application/index.html) with ``data:element/id,name,Container+Diagram/single-page-application)`` link.

### target-selector

Spring Expression Language (SpEL) expression evaluating to a target element.
Target selectors are similar to initializers with the following differences:

* Target selectors are evaluated after initializers
* An initializer is evaluated once, but a target selector might be evaluated multiple times until it returns a non-null value or the maximum number of passes is exceeded
* A target selector is only evaluated if there isn't a target element already

Target selectors can be used to evaluate target elements using target elements of other elements. 
For example, a target selector of a child node may need a target element of its parent to resolve its own target element.

The expression is evaluated in the context of the diagram element with access to the following variables:
* ``registry``
* ``pass``
* ``progressMonitor``
* ``resourceSet``
* ``capabilityLoader``

### reference

Diagram elements can be associated with target elements' references. 
A Java analogy would be:

```java
List<Person> isaChildrenDiagramElement = familyWorkBook.findById("isa").getChildren();
```

``reference`` property associates a diagram element with a reference of the target element of the first matched ancestor for mapping purposes.
If the element has mapped descendants, their matching targets elements are added to the reference.

Reference value can be a string or a map. The string form is equivalent to the map form with just the ``name`` entry.


The map form supports the following keys:

* ``comparator`` - used to sort reference elements, see the [Comparators](#comparators) section.
* ``condition`` - a SpEL ``boolean`` expression evaluated in the context of the ancestor target element. If not provided, matches the first mapped ancestor. Has access to the following variables:
    * ``sourcePath`` - a list of ancestors starting with the parent
    * ``registry``
* ``expression`` - a SpEL ``EObject`` expression evaluated in the context of the ancestor target element. If not provided, the ancestor itself is used. Has access to the following variables:
    * ``sourcePath`` - a list of ancestors starting with the parent
    * ``registry``
* ``name`` - reference name
* ``element-condition`` - a SpEL ``boolean`` expression evaluated in the context of the descendant (contents) target element. If not provided, elements are matched by type compatibility with the reference type. Has access to the following variables:
    * ``sourcePath`` - source containment path
    * ``registry``
* ``element-expression`` - a SpEL ``EObject`` expression evaluated in the context of the descendant (contents) target element. If not provided, the descendant itself is used. Has access to the following variables:
    * ``sourcePath`` - source containment path
    * ``registry``
 
See [References](https://nasdanika-demos.github.io/semantic-mapping/references/index.html) demo for examples of using reference mapping.

### features

``features`` property defines mapping of target element structural features - references and attributes.
[Feature mapping demo](https://nasdanika-demos.github.io/semantic-mapping/features/index.html) provides a few examples of reference mapping.

The value of the property shall be a map with the following supported keys:

* end
* container 
* contents 
* self
* source 
* start
* target

#### end

For connections - mapping specification, as explained in the [Feature Mapping](#feature-mapping) section, for the connection end feature to map the connection target semantic element[^target_semantic] to a feature of the connection semantic element.

[^target_semantic]: Here and below the term "semantic element" is used interchangeably with the term "target element" to avoid confusion with connection target element.

```yaml
end: father
```

The above specification means "set ``father`` reference of the connection semantic element to the semantic element of its end (Joe)".

#### container

Mapping specification for the container element in container/contents permutation.
Contains one or more of the following sub-keys with each containing a map of feature names to a mapping specification or a list of feature names.

* ``self`` - this element is a container 
* ``other`` - the other element is a container

##### Example

```yaml
type: family.Polity
features:
  container:
    self: 
      residents:
        argument-type: family.Person
        path: 1
    other: constituents
```

The above example shows [Texas](https://nasdanika-demos.github.io/semantic-mapping/features/usa/texas/index.html) feature mapping:

* ``self`` means Texas itself. The mapping specifies that immediate children of this element (``path: 1``) shall be added to this (Texas) semantic element ``residents`` collection if they are instances of ``family.Person``. [Joe](https://nasdanika-demos.github.io/semantic-mapping/features/usa/texas/joe/index.html) diagram element is an immediate child of the Texas diagram element.
* ``other`` means [USA](https://nasdanika-demos.github.io/semantic-mapping/features/usa/index.html) because USA contains Texas. This mapping specifies that this (Texas') target element shall be added to the ``constituents`` feature of its container (USA) regardless of the containment path length. In the example the containment path length is ``1``.

Please note that when a diagram element is linked to a page, then the page's page element is logically merged with that element. 
In the [Internet Banking System Architecture](https://nasdanika-demos.github.io/internet-banking-system-c4/cerulean/index.html) [GetBalanceRequest](https://nasdanika-demos.github.io/internet-banking-system-c4/cerulean/references/elements/internet-banking-system/references/elements/api-application/references/elements/mainframe-banking-system-facade/references/elements/7WUxxUmOBlSPoxcwmfUW-26/index.html) is contained by [Internet Banking System](https://nasdanika-demos.github.io/internet-banking-system-c4/cerulean/references/elements/internet-banking-system/index.html) with ``path=3`` - they appear on different diagrams (pages), but these diagrams are connected with page links.

#### contents

Mapping specification for the contents element in container/contents permutation. 
Contains one or more of the following sub-keys with each containing a map of feature names to a mapping specification or a list of feature names.

* ``self`` - this element is contained by the other 
* ``other`` - the other element is contained by this element

##### Examples

###### USA

```yaml
contents:
  other: 
    country:
      path: 2
```

The above feature map for USA means that ``other`` is either Florida, Texas, Jane or Joe - they are all contained in USA directly or indirectly.
Path 2 means that only Jane and Joe match this mapping. And ``country`` means that their ``country`` reference shall be set to USA. 

###### Joe

```yaml
contents:
  self: 
    country:
      path: 2
```

The above feature map for Joe means that ``other`` is either Texas or USA - they both contain Joe, Texas directly and USA transitively.
Path 2 means that only USA matches this mapping. And ``country`` means that Joe's ``country`` reference shall be set to USA. 

#### self

A map of feature names to Spring Expression Language (SpEL) expressions or a list of expressions evaluating to the feature value or feature element value.

The expression is evaluated in the context of the source diagram element and has access to the following variables:

* ``value`` - semantic element
* ``registry`` - a map of diagram element to semantic elements

#### source

For connections - mapping specification for the connection source feature to map the connection semantic element to a feature of the connection source semantic element.
If there are no connection semantic elements, then the connection target semantic element is used instead (pass-through connection).

```yaml
source: father
```

The above specification at the [Jane -> Joe connection](https://nasdanika-demos.github.io/semantic-mapping/features/usa/florida/jane/father/index.html)  means to set connection source (Jane) ``father`` feature to connection target semantic element (Joe) because the connection itself doesn't have  semantic elements. 
In pseudo-code:

```java
connection.getSource().setFather(connection.getTarget());
```

#### start

For connections - mapping specification for the connection start feature to map the connection source semantic element to a feature of the connection semantic element.

```yaml
start: child
```

The above specification means "set ``child`` reference of the connection semantic element to the semantic element of its start (Jane)".

#### target

For connections - mapping specification for the connection target feature to map the connection semantic element to a feature of the connection target semantic element.
If the connection doesn't have semantic elements (pass-through connection), then the connection source semantic element is used instead.

```yaml
target: children
```

The above specification at the Jane -> Joe connection means to set connection target (Joe) ``children`` feature to the connection source semantic element (Jane) because the connection itself doesn't have semantic elements. 
In pseudo-code:

```java
connection.getTarget().getChildren().add(connection.getSource());
```

### features-ref

``features-ref`` property value shall be a string which is treated as a URI resolved relative to the base URI. 
Resource at the URI is parsed as YAML.

### Representations

For semantic elements which extend [ModelElement](https://ncore.models.nasdanika.org/references/eClassifiers/ModelElement/index.html)
the loading process injects representations which can be used in Markdown documentation and as icons in generated HTML documentation. 

The loading process injects two representations:

* ``drawio`` - a Drawio diagram containing pages where the page element maps to this target element.
* ``image`` - loaded from diagram element style ``image``. 

Representation reduce documentation effort and drive consistency.

#### Filtering

Representations can be customized (filtered) by creating a service capability of type ``AbstractMappingFactory.Contributor`` implementing ``AbstractDrawioFactory.RepresentationElementFilter`` and implementing its ``filterRepresentation()`` method.
This functionality can be used, for example to style elements based on information retrieved from external systems. For example:

* Development status - Planned/Backlog, In Progress, Blocked, Done
* Runtime status - Operational, Overloaded, Failed, Planned maintenance
* Quality/technical debt status - OK, Warning, Danger

### Configuration

After diagram elements are mapped to target elements (initialized) and their features are mapped, they are configured using their diagram element properties 
as explained below.

#### config-prototype

With ``config-prototype`` property you can inherit configuration from another diagram element.
Property value shall be a Spring Expression Language (SpEL) expression evaluating to a diagram element.
Diagram element configuration (properties) is applied to this semantic element. 

Example: ``getDocument().getModelElementById('web-server-prototype')``

Config prototypes allow to define common configuration in one element and then reuse it in other elements. 
For example, a web server prototype may define an icon and then all web server element would inherit that configuration.
Config prototypes can be chained - you may create an inheritance hierarchy of diagram elements. 

#### Documentation

Documentation properties can be used to add documentation to target elements which implement [Documented](https://ncore.models.nasdanika.org/references/eClassifiers/Documented/index.html) interface.

Documentation can be provided in ``documentation`` property in Markdown, plain text, or HTML. 
Markdown is the default documentation format. 
You can modify it by setting ``doc-format`` property. Supported values are ``markdown``, ``text``, and ``html``.

It might be more convenient to maintain documentation in an external resource. 
In this case specify the documentation resource URI in ``doc-ref`` property. 
The resource URI is resolved relative to the base URI of the diagram element. 
If ``doc-format`` is not set, it is inferred from the resource extension - HTML for ``.htm`` and ``.html``, text for ``.txt``, Markdown otherwise. 

Documentation may also be configured via ``configuration`` or ``configuration-ref``.

#### Label

If the target element extends [NamedElement](https://ncore.models.nasdanika.org/references/eClassifiers/NamedElement/index.html)
and its name is not set, then diagram element label converted to plain text is used as semantic element name.

#### Markers

If the target element implements [Marked](https://ncore.models.nasdanika.org/references/eClassifiers/Marked/index.html) then the loading process
adds diagram element markers to the semantic element. 
It allows to track provenance of data elements which might important in scenarios where model elements are loaded from diverse sources and even a single element may be loaded from several sources. For example, attributes are loaded from an Excel workbook and relationships from a Drawio diagram.
The loading process is aware of Git repositories - if it detects that the diagram file is under source control it would store Git-specific information
in markers - repository path, branch, commit hash, remotes, and head references. 

#### identity

If the target element extends [``StringIdentity``](https://ncore.models.nasdanika.org/references/eClassifiers/StringIdentity/index.html), ``identity`` property can be used to specify the ``id`` attribute.
If this property is not provided, then Drawio model element ID is used as identity. 
Drawio element id's are editable, but duplicate id's are not allowed on the same page. 
You may have duplicate semantic id's in different containers on the same page. 
In this case you may use ``identity``. 
Identity can also be set using the ``configuration`` and ``configuration-ref`` YAML, this property is a shortcut way.

#### configuration

Target elements may be configured by providing a YAML configuration map in the ``configuration`` property
or a URI of a configuration resource in the ``configuration-ref`` property. The URI is resolved relative to the base URI of the diagram element.

Configuration YAML maps target element features (attributes and references) to their values as shown in the below example:

```yaml
location: %id%/index.html
icon: /images/mapping.svg
children:
  - Action:
      location: mapping-reference.html
      text: Mapping Reference
      content:
        Interpolator:
          source:
            Markdown:
              style: true
              source:
                exec.content.Resource: mapping-reference.md
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

#### Tooltip

If the target element extends [NamedElement](https://ncore.models.nasdanika.org/references/eClassifiers/ModelElement/index.html)
and its description is not set, then diagram element tooltip is used as semantic element description.

#### Contributor.configure()

Configuration can be customized by creating a service capability of type ``AbstractMappingFactory.Contributor`` and overriding its ``configure()`` method.

### Operations

Using ``operations`` and ``operations-ref`` properties you may specify 
target element operations to be invoked. 
``operations`` value shall be a YAML map of invocation target to operation names and then to invocation specifications explained below,
``operations-ref`` shall be a URI of a resource containing a YAML map.
The URI is resolved relative to the diagram element base URI. 

The invocation target is either ``self``, ``source`` or ``target`. 
``source`` and ``target`` are applicable only to connections.

The invocation specification is either a map or a list of maps. 
The sections below describe the keys supported by the maps.

Examples:

```yaml
type: Fox
operations:
  self:
    eats: 
      arguments:
        food: "#registry.get(outgoingConnections[0].target)"
      pass: 2
```  

```yaml
operations:
  source:
    eats: 
      arguments:
        food: "#registry.get(target)"
```        

#### arguments

A map of parameter names to SpEL expression evaluating their values in the context of the iterator element (see below).
Argument names are used for operation selection/matching - a candidate operation must have parameters with matching names. 
The map does not have to contain entries for all operation parameters. 
Nulls are used as arguments for parameters which are not present in the map. 

#### iterator

An optional SpEL expression which returns a value to iterate over and invoke the operation for every element. 

* If the result is ``java.util.Iterator`` then it is used AS-IS.
* If the result is Iterable, Stream, or array, an iterator is created to iterate over the elements.
* If the result is ``null``, then an empty iterator is created. 
* Otherwise, a singleton iterator is created wrapping the result.

It allows to invoke the operation zero or more times. 
If not defined, the iterator contains the source diagram element. 

#### pass

An optional integer specifying the pass in which this operation shall be invoked.
Use for ordering operation invocations. 

In the above example, because we want the Fox to eat the Hare after the Hare eats the Grass, we need so set ``pass`` to ``1`` for the Fox.

#### selector

An optional SpEL boolean expression evaluated in the context of the operation to disambiguate overloaded operations.

### invoke

The last phase of mapping is invoking [Invocable URI](../capability/index.html#loading-invocables-from-uris)s specified in ``invoke`` property. 
URIs are resolved relative to the base URI. 

Invocables are bound to the following arguments by name:

* ``target`` - target (semantic) element, can be null
* ``pass``
* ``registry`` - a map of source elements to target elements
* ``contentProvider``
* ``progressMonitor``
* ``resourceSet``
* ``capabilityLoader``

Then it is invoked with the source element as a single positional argument.

If the invokable return ``false`` it means that it could not complete its job in this pass and it will in invoked again in subsequent passes until it returns anything other than ``false``.

This functionlity can be used for procedural/imperative mapping in configuration/declarative mapping is not enough
or you just prefer to do things procedurally.

#### Groovy mapper

##### Mapping

```yaml
invoke: mapper.groovy
```

##### Script

```groovy
import org.nasdanika.models.nature.Color

System.out.println("---");
System.out.println("Source: " + args);
System.out.println("Target: " + target);
System.out.println("Pass: " + pass);

target.setColor(Color.BROWN);

return pass > 2; // Just to test multiple invocations
```

#### Java mapper

##### Mapping

```yaml
invoke: data:java/org.nasdanika.demos.diagrams.mapping.Services::connectionMapper
```

##### Mapping method

```java

public static void connectionMapper(
		CapabilityFactory.Loader loader, 
		ProgressMonitor loadingProgressMonitor, 
		byte[] binding,
		String fragment,
		@Parameter(name = "target") Object target,
		@Parameter(name = "pass") int pass,
		@Parameter(name = "contentProvider") ContentProvider<Element> contentProvider,
		@Parameter(name = "registry") Map<EObject, EObject> registry,
		@Parameter(name = "progressMonitor") ProgressMonitor mappingProgressMonitor,
		@Parameter(name = "resourceSet") ResourceSet resourceSet,
		@Parameter(name = "capabilityLoader") CapabilityLoader capabilityLoader,
		Connection source) {

	System.out.println("--- Java mapper ---");				
	System.out.println("Connection: " + source);		
	System.out.println("Pass: " + pass);		
}	
```

Note the use of positional and named parameters:

* The first 4 parameters are bound positionally when invocable URI is loaded
* The 5 named parameters a bound by name by the mapper
* The last parameter is used for invocation (also positional)


## Mapping Selector

Mapping selector can be used to select zero or more target elements for feature mapping.
If it is not provided, then the diagram element's target element is used for feature mapping, if it is present.

Mapping selector shall be a YAML document containing either a single string or a list of strings.
The strings are treated as Spring Expression Language (SpEL)] expression
evaluating to a target element or a collection of target elements to use for feature mapping.
Expressions are evaluated in the context of the diagram element and have access to the following variables:

* ``registry``
* ``progressMonitor``
* ``resourceSet``
* ``capabilityLoader``

Mapping selectors may be used to associate multiple semantic elements with a diagram element for feature mapping purposes.

Mapping selector can be defined in ``mapping-selector`` property or in an external resource with URI specified in ``mapping-selector-ref`` property.
The resource URI is resolved relative to the base URI of the diagram element.

## Feature Mapping

This section explains the structure of feature map values.
The mapping value can be either a string or a map. If it is a string it is treated as a singleton map to ``true`` (unconditional mapping).

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

### argument-type

Specifies the type of feature elements to be set/added. 
String or a list of strings. Each string is a type name as defined above 
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

### comparator

Comparator is used for sorting elements of "many" features. See the [Comparators](#comparators) section below for a list of available comparators and their configurations.

### condition

A SpEL boolean expression evaluated in the context of the candidate diagram element with the following variables:

* ``value`` - semantic element of the candidate diagram element
* ``path`` - containment path
* ``registry`` - a map of diagram element to semantic elements
* ``progressMonitor``
* ``resourceSet``
* ``capabilityLoader``

### expression

A SpEL expression evaluating to a feature value in the context of the diagram element with with the following variables:

* ``value`` - semantic element of the diagram element
* ``path`` - containment path
* ``registry`` - a map of diagram element to semantic elements
* ``progressMonitor``
* ``resourceSet``
* ``capabilityLoader``

### greedy

Greedy is used with containment features and specifies what to do if a candidate object is already contained by another object:

* ``no-children`` - grab the object if it is contained by an ancestor of this semantic element. This is the default behavior.
* ``false`` - do not grab
* ``true`` - always grab

### invoke

[Invocable URI](../capability/index.html#loading-invocables-from-uris) resolved relative to the base URI.

The invocable is bound to the following arguments by name:

* ``argumentValue`` - feature value or value evaluated by ``expression``. 
* ``baseURI``
* ``context``
* ``progressMonitor``
* ``registry``
* ``sourcePath``
* ``type``
* ``resourceSet``
* ``capabilityLoader``

It is invoked with ``argument`` as positional argument and shall return feature value.

### path

Either an integer number or a list of boolean SpEL expressions to match the path. 
If an integer, then it is used to match path length as shown in the example below, which matches only immediate children

```yaml
container:
  self: 
    elements:
      path: 1
```

If a list, then it matches if the list size is equal to the path length and each element evaluates to ``true`` in the context of a given path element.
Expressions have access to ``registry`` variable - a map of diagram elements to semantic (target) elements.

### nop

If ``true`` then no mapping is performed and the chain mapper is not invoked. 
It can be used in scenarios with a default (chained) mapper to prevent the default behavior.

### position

A number specifying the position of the element in the feature collection.
Please note that while this key is supported using it may lead to loading errors if the feature collection is smaller than the position.
Because the loading order is generally not controlled by the diagram author, only ``0`` position is guaranteed to work all the time.

### type

Type of the feature object to match. String as defined in the Initialization / type section.
Can be used in ``other`` mappings.

## Comparators
Comparators are used for "many" features to order elements. 
A comparator instance is created by ``AbstractDrawioFactory.createMapperComparator()`` method which can be overridden in subclasses to provide support for additional comparators.

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

#### flow

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

The condition specifies that a connection with ``sent-mail`` ID shall not be traversed.

#### reverse-flow

Same as ``flow`` but with target nodes being smaller than source nodes.

### enumerate

Sorts model elements using enumerate value.
Elements without enumerate value are considered equal to any other elements including those
with enumerate value. 
This is done to allow chaining with, say, flow comparator. 
As a result, this comparator will violate the transitivity requirement if some elements don't have enumerate value. 
Therefore, it shall be chained with other comparators. For example, flow and then position or label.
     
Enumerate value is treated as path of dot-separated values and two enumerate values are compared
element-by-element with elements containing only digits parsed and compared as integers.
For example, ``20`` would be greater than ``3``, ``1.1.1`` would be greater than ``1.1`` and smaller than ``2.5.6`` or ``3``.
Numbers are considered smaller than strings ``1.12`` is smaller than ``1.a``
 
Practical use - ordering connections emanating from the same node. Say, excursions from the same location.  
If those excursions have multiple segments, then this comparator can be chained with the flow comparator
and possibly terminated by the position or label comparator just in case.

Example:

```yaml
container:
  self: 
    elements:
      path: 1
      comparator: 
        enumerate:
          fallback:          
            flow: 
              fallback: label
              condition: id != 'send-email'
```

### reverse-enumerate

Reversed ``position`` comparator.

### expression

A SpEL expression evaluated in the context of the feature element with ``other`` variable referencing the element to compare with. 
The expression has access to  the following variables:

* ``registry`` 
* ``progressMonitor``
* ``resourceSet``
* ``capabilityLoader``

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

### position

Uses the diagram element position in the parent element for sorting. 
Example:

```yaml
container:
 self: 
members:
 comparator: position
```

### reverse-position

Reversed ``position`` comparator.

### property

Uses diagram element property as a sorting key. A singleton map. For example:

```
property: label
```

### property-descending

The same as property, but compares in reverse alphabetical order.

## Contributors

In addition to loading Contributors via the capability framework they can get added to the contributors list programmatically.
This scenario can be useful for contributors which are specific for a given digaram or piece of functionality.

### Reflective contributors

``ReflectiveContributor`` provides two method annotations - ``@Initalizer`` and ``@Configurator`` which allow to contribute to
the mapping process using annotated methods. 
This can be used for fine-grained contributions. 
For example, you may have a UI wireframe diagram with high-level structure and configuration defined in the diagram and 
some details (e.g. values retrieved from external systems) injected by annotated methods.

Both annotations have the following attributes: 

* ``value`` - If not blank (default), the value shall be a [Spring boolean expression](https://docs.spring.io/spring-framework/reference/core/expressions.html) which is evaluated in the context of the source with target as <code>target</code> variable. 
* ``sourceType`` - source type matching is done by the first parameter type. This attribute can be used to narrow the match.
* ``targetType`` - target type matching is done by the second parameter type. This attribute can be used to narrow the match.
* ``priority`` - methods with higher priority value are invoked before methods with lower priority value

Annotated methods for both annotations must have at least two parameters to accept source and target arguments. 

Initializer methods can have up to 5 parameters with additional parameters listed below:

3. ``BiConsumer<EObject, BiConsumer<EObject, ProgressMonitor>> elementProvider``
4. ``Consumer<BiConsumer<Map<EObject, EObject>, ProgressMonitor>> registry``
5. ``ProgressMonitor progressMonitor``

Configurator methods can have up to 6 parameters with additional parameters listed below:

3. ``Collection<EObject> documentation``
4. ``Map<S, T> registry``
5. ``boolean isPrototype``
6. ``ProgressMonitor progressMonitor``


#### Example

##### Configurator

```java
public class Configurators {
    
    @Configurator("id == 'body-root-container'")
    public void configure(Node source, Container target) {       
        Appearance appearance = BootstrapFactory.eINSTANCE.createAppearance();      
        target.setAppearance(appearance);
        appearance.setBackground(Color.PRIMARY);
    }

}
```

In the above snippet ``configure`` method matches a source node with id ``body-root-containter`` which is mapped to ``Container``. 
The method sets the container background color.

##### Client code

```java
ReflectiveContributor<Element, EObject> rc = new ReflectiveContributor<>(List.of(new Configurators()));  
Document document = Document.load(new File("bootstrap.drawio"));        

ConnectionBase connectionBase = ConnectionBase.SOURCE;
ContentProvider<Element> contentProvider = new DrawioContentProvider(
        document, 
        Context.BASE_URI_PROPERTY, 
        MAPPING_PROPERTY, 
        MAPPING_REF_PROPERTY, 
        connectionBase);

CapabilityLoader capabilityLoader = new CapabilityLoader();
ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
Requirement<ResourceSetRequirement, ResourceSet> requirement = ServiceCapabilityFactory.createRequirement(ResourceSet.class);       
ResourceSet resourceSet = capabilityLoader.loadOne(requirement, progressMonitor);
        
ConfigurationLoadingDrawioFactory<EObject> drawioFactory = new ConfigurationLoadingDrawioFactory<EObject>(
        contentProvider, 
        capabilityLoader, 
        resourceSet, 
        progressMonitor) {

            @Override
            protected EObject getByRefId(Element obj, String refId, int pass, Map<Element, EObject> registry) {
                return null;
            }
    
};

drawioFactory.getContributors().add(rc);

Transformer<Element,EObject> modelFactory = new Transformer<>(drawioFactory);
List<Element> documentElements = new ArrayList<>();
Consumer<Element> visitor = documentElements::add;
@SuppressWarnings({ "rawtypes", "unchecked" })
Consumer<org.nasdanika.graph.Element> traverser = (Consumer) org.nasdanika.drawio.Util.traverser(visitor, connectionBase);
document.accept(traverser);

Map<Element, EObject> modelElements = modelFactory.transform(documentElements, false, progressMonitor);

List<EObject> cnt = new ArrayList<>();
modelElements.values()
    .stream()
    .distinct()
    .filter(modelElement -> modelElement != null && modelElement.eContainer() == null)
    .forEach(cnt::add);

// Saving for manual inspection
URI xmiURI = URI.createFileURI(new File("target/bootstrap.xml").getAbsolutePath());
Resource xmiResource = resourceSet.createResource(xmiURI);
xmiResource.getContents().addAll(cnt);
xmiResource.save(null);

HtmlGenerator htmlGenerator = HtmlGenerator.load(
        Context.EMPTY_CONTEXT, 
        null, 
        progressMonitor);
        
Producer<Object> processor = htmlGenerator.createProducer(cnt.get(0), progressMonitor);
Object result = processor.produce(0);

Files.writeString(Path.of("target", "bootstrap.html"), (String) result);
```

In the above snippet:

* ``Configurator`` class is instantiated and added at the reflective contributor at line 1.
* ``Document`` is loaded from a file at line 2 and then a content provider is created from it.
* ``ResourceSet`` is obtained from ``CapabilityLoader``
* ``ConfigurationLoadingDrawioFactory`` is instantiated with the content provider, capability loader, and resource set passed to its constructor.
* Reflective contributor is added to the loading factory contributors.
* ``Transformer`` is created from the factory and all diagram elements are added to its list of sources.
* Diagram elements are transformed (mapped) to [Bootstrap model](https://bootstrap.models.nasdanika.org/) elements. ``configure`` method is invoked as part of this process.
* The model is saved to an XML file.
* HTML is generated from the model using ``HtmlGenerator`` and saved to a file.







 