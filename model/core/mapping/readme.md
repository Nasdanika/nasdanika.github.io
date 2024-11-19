(Semantic) mapping is a process of creating and populating Ecore models from other data sources, [Drawio diagrams](../drawio/index.html) in particular.
This module provides base mapping functionality and the Drawio module provides concrete implementation classes and several Drawio-specific comparators which use element properties and geometry.

This page provides a combined documentation for both generic and Drawio-specific mapping. 
Most code snippets below are taken from the [semantic mapping demo](https://nasdanika-demos.github.io/semantic-mapping/index.html).

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
    * **Resource set** - a group of related resources identified by a URI. Resource sets have associated packages, resource factories, adapter factories, and URI handlers not shown on the diagram.
    * **Resource** - a group of objects. A resource is identified by a URI.
    * **Object** - an instance of a metamodel class.
    
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
    * **Object** - ``Paul`` and ``Elias`` are instances of ``Man`` class. ``Elias`` references ``Paul`` as his father. 
               
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
The invocable is bound to the following elements by name:

* ``registry`` - a map of source elements to target elements
* ``contentProvider`` - content provider
* ``progressMonitor`` - progress monitor

Then it is invoked with the source object as a single positional argument.

It may return ``null`` - in this case ``type`` would be used to create a semantic element, if specified.

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
		Element source) {
	ArchitectureDescriptionElement architectureDescriptionElement = ArchitectureFactory.eINSTANCE.createArchitectureDescriptionElement();
	architectureDescriptionElement.setDescription("I was created by a Java initializer");
	return architectureDescriptionElement;				
}
```

Note the use of positional and named parameters:

* The first 4 parameters are bound positionally when [invocable URI is loaded](https://docs.nasdanika.org/core/capability/index.html#loading-invocables-from-uris)
* The 3 named parameters a bound by name by the mapper
* The last parameter is used for invocation (also positional)


#### type

``type`` property is used if there is no ``initializer`` or if it returned ``null``. 
The value of property is the type of the semantic element. 
Types are looked up in the factory packages in the following way:

* If the value contains a hash (``# ``) then it is treated as a type URI. For example ``ecore://nasdanika.org/models/family#//Man``.
* If the value contains a dot (``.``) then it is treated as a qualified EClass name with EPackage prefix before the dot. For example ``family.Man``. There may be more than one dot if EClass is defined in a sub-package. For example, ``exec.content.Markdown``. 
* Otherwise, the value is treated as an unqualified EClass name - registered EPackages (and sub-packages recursively) are sorted by Namespace URI, iterated, and the first found EClass with matching name is used to create a semantic element.

A combination of ``initializer`` and ``type`` can be used for mapping in different contexts.
For example, when loading a stand-alone model ``initializer`` would evaluate to ``null`` and then ``type`` would be used. 
When the same diagram loaded in the context of a larger model, ``initializer`` may evaluate to a semantic element looked up in that larger model.

Example:

```yaml
type: architecture.c4.System
```

A Java analogy for ``type``:

```java
Object isaDiagramElement = getClassLoader().loadClass(type).newInstance();
```

#### ref-id

``ref-id`` is some identifier to resolve to a semantic element. 

If there is already a semantic element created/resolved with ``initializer`` or ``type`` and ``isRefIProxydURI()`` returns true, 
then ``ref-id`` is treated as [EObject proxy](https://javadoc.io/static/org.eclipse.emf/org.eclipse.emf.ecore/2.37.0/org/eclipse/emf/ecore/EObject.html#eIsProxy()) URI resolved relative to the base URI (see below). 
You may need to use this approach in case of circular references between resources or if the semantic element type and URI are known, but the element itself is not available during the load time.

If there is no semantic element yet, then ``ref-id`` is used to look it up in the resource set. 
You may use a "physical" URI to load objects on demand, or "logical"/"semantic" URI for already loaded objects.
Say, ``ssn:123-45-6789`` to lookup a person by their SSN.

### page-element

If there is no semantic element for a diagram element yet and the diagram element's ``page-element`` property is set to ``true`` then its semantic element is 
set to the first found semantic element of diagram elements linking to the page. 

For example, on the [System Context Diagram](https://nasdanika-demos.github.io/internet-banking-system-c4/cerulean/index.html) the "Internet Banking System" element links to the [Container diagram](https://nasdanika-demos.github.io/internet-banking-system-c4/cerulean/references/elements/internet-banking-system/index.html) page where the "Internet Banking System" container is the page element.
As a result, both of these diagram elements map to the same target (semantic) element.

There should be one page element per page. Having more than one may lead to an unpredictable behavior.

Using ``page-element`` you can define a high-level structure on one diagram page, link other pages to the diagram elements and 
refine their definitions.
This process can be repeated to build a hierarchy of pages as demonstrated in the "Internet Banking System Architecture" demo mentioned above.

If the semantic element of a page element extends [``NamedElement``](https://ncore.models.nasdanika.org/references/eClassifiers/NamedElement/index.html) then the page name is used as element's name if hasn't been already set by other means.

### prototype

``prototype`` is a [Spring Expression Language](https://docs.spring.io/spring-framework/reference/core/expressions.html) (SpEL) expression evaluating to a diagram element.
The semantic element of that diagram element is copied and the copy is used as the semantic element of this diagram element. 
Also, prototype configuration (properties) is applied to this semantic element. 

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

A prototype must have a semantic element defined - the loading process will keep evaluating the prototype expression until it returns non-null or until the maximum number of passes is exceeded. 
In the latter case the loading process would fail.
If you want to inherit just configuration, but not the semantic element, then use ``config-prototype`` property instead of ``prototype``.

### selector

``selector`` is a Spring Expression Language (SpEL) expression evaluating to a diagram element.
The semantic element of that diagram element is used as the semantic element of this diagram element. 
Selectors allow to use the same semantic element on multiple diagrams. 

For example, in the "Internet Banking System" demo ``E-Mail System`` is defined on the System Context Diagram and selected (referenced) on the Container Diagram with ``getModel().getPage().getDocument().getModelElementByProperty('semantic-id', 'microsoft-exchange')`` expression.

The expression is evaluated in the context of the diagram element with access to the following variables:

* ``registry``
* ``pass``
* ``progressMonitor``

Please note that you may also use the [extended link syntax](../drawio/index.html#page-and-element-links) to associate more than one diagram element with a single semantic element.
If you are selecting by diagram element ``id`` or label, then the extended link syntax is preferable to using ``selector`` expression.

In the "Internet Banking System" C4 Model demo ``Single-Page Application`` is defined on the Container Diagram and linked from on the API Application Component Diagram with ``data:element/id,name,Container+Diagram/single-page-application)`` link.

### target-selector

Spring Expression Language (SpEL) expression evaluating to a target (semantic) element.
Target selectors are similar to initializers with the following differences:

* Target selectors are evaluated after initializers
* An initializer may evaluate to ``null``, but a target selector must eventually evaluate to a non-null value
* An initializer is evaluated once, but a target selector might be evaluated multiple times until it returns a non-null value
* A target selector is only evaluated if there isn't a target element already

Target selectors can be used to evaluate target elements using target elements of other elements. 
For example, a target selector of a child node may need a target element of its parent to resolve its own target element.

The expression is evaluated in the context of the diagram element with access to a ``registry``, ``pass`` and ``progressMonitor`` variables.

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

* ``comparator`` - used to sort reference elements, see [Comparators](comparators.html).
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

 