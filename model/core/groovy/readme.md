
[TOC levels=6]

## Groovy DSL for EMF Models

This module lets you author EMF models (Ecore metamodels, or instances of any registered metamodel) as Groovy scripts instead of hand-writing XMI.
A script describes a tree of `EObject`s; the loader evaluates it and the result becomes the contents of an EMF `Resource`.

The DSL is **metamodel-driven**: nothing is hard-coded per `EClass` or per feature.
Every entry point, every nested element and every attribute/reference the builder accepts is read reflectively from the `EPackage` you resolve
against.
Point it at a different metamodel and you get a different DSL for free.

### 1. User guide

#### 1.1 File naming and how a script is loaded

The DSL plugs into Nasdanika's capability/`ResourceContentsHandler` pipeline. 
A file name is a chain of dot-separated **qualifiers** read right-to-left:

| File name | Qualifier chain | Meaning |
|-----------|-----------------|---------|
| `model.groovy` | `groovy` | Generic DSL - top-level names come from whatever metamodel the generic resolver can see. |
| `family.ecore.groovy` | `groovy` -> `ecore` | The `ecore` qualifier binds the DSL to `EcorePackage`, so `ePackage`, `eClass`, `eReference`, … are available as entry points. |

* The rightmost `.groovy` is handled by the source handler, which compiles the text to a `javax.script.CompiledScript`.
* The qualifier to its left (`ecore`) selects the transform handler that installs the metamodel-specific DSL, evaluates the script and turns the result
  into `EObject[]`. 

You normally never touch the API directly - you just load the resource:

```java
ResourceSet resourceSet = capabilityLoader.loadOne(
        ServiceCapabilityFactory.createRequirement(ResourceSet.class), progressMonitor);

File file = new File("src/test/resources/family.ecore.groovy").getCanonicalFile();
Resource resource = resourceSet.getResource(
        URI.createFileURI(file.getAbsolutePath()), true);

// resource.getContents() now holds the EPackage(s) built by the script
```

#### 1.2 A first example

The whole authoring surface is "a method call names a feature or a child type, and a closure configures that child."
Here is a minimal Ecore package:

```groovy
ePackage {

    name 'familymodel'
    nsURI 'https://family.models.nasdanika.org'
    nsPrefix 'org.nasdanika.models.family'

    eClass {
        name 'NamedElement'

        eAttribute {
            name 'name'
            eType 'EString'
            lowerBound 1
            upperBound 1
        }
    }
}
```

This builds an `EPackage` named `familymodel` containing one `EClass`, `NamedElement`, with a single-valued `name : EString` attribute.

#### 1.3 Entry points and nesting

Top-level names (`ePackage`, `eClass`, …) are **entry points**: every concrete `EClass` in the bound metamodel whose decapitalised simple name is unambiguous
is installed as a root keyword.
Inside a builder closure, a bare call such as `eClass { … }` or `eAttribute { … }` is **type dispatch**: a child of that type
is created and auto-routed into the one containment feature that accepts it (`EPackage.eClassifiers`, `EClass.eStructuralFeatures`, …).

If a call name matches a **structural feature** of the current element instead, it is **feature dispatch**
(`name 'x'`, `eType 'EString'`, `eSuperTypes namedElement`).
Feature dispatch is tried before type dispatch.

```groovy
ePackage {
    name 'test'                       // feature dispatch: sets EPackage.name

    eClass {                          // type dispatch: new EClass into eClassifiers
        name 'Address'                // feature dispatch on the EClass

        eReference {                  // type dispatch: new EReference into eStructuralFeatures
            name 'residents'
            eType "/eClassifiers[name='Person']"
        }
    }
}
```

When a child type would be ambiguous (it fits more than one containment feature) or the feature is typed by an abstract class, name the concrete type
explicitly, e.g. `eClassifiers('EClass') { name 'Company' }`.

#### 1.4 Setting attributes - the three call forms

A feature can be set three ways. Two of them work; **one is a trap**:

| Form | Example | Dispatch |
|------|---------|----------|
| Command call | `lowerBound 1` | method call -> feature |
| Explicit call | `lowerBound(1)` | method call -> feature |
| Assignment | `lowerBound = 1` | property assignment -> feature |

All three end up calling the same feature-setting code. Strings, numbers and enum literals are coerced to the feature's data type automatically
(`eType 'EString'` resolves the classifier; `lowerBound 1` coerces to `int`; enum attributes accept the literal name).

#### 1.5 Numbers: `upperBound = -1` vs `upperBound(-1)` vs `upperBound -1`

This is the single most important authoring gotcha, and it only bites with **negative** numbers
(notably `upperBound = -1`, EMF's "unbounded" / `*` multiplicity).

A bare command call works for a **positive** literal:

```groovy
lowerBound 1        // parsed as lowerBound(1)
upperBound 1        // parsed as upperBound(1)
```

But the same shape with a negative literal does **not** call the method.
Groovy parses `upperBound -1` as the binary expression `upperBound - 1`: it *reads* the
`upperBound` property and subtracts `1`, then throws the result away. 
The feature is never set.

```
form 'x = -1'   : [ASSIGN upperBound = -1]     // sets the feature
form 'x(-1)'    : [METHOD upperBound([-1])]     // sets the feature
form 'x -1'     : [READ upperBound]             // reads & subtracts, never sets
```

So, to set an unbounded multiplicity, use **either**:

```groovy
upperBound = -1     // assignment form
upperBound(-1)      // explicit-parentheses form
```

and **never**:

```groovy
upperBound -1       // silently does nothing; upperBound keeps its default
```

The rule generalises: **whenever an argument starts with a unary `-` (or `+`),
use the assignment or parenthesised form.** Positive bare literals are fine.

#### 1.6 Variables and inline children

A child built by an entry point or a type-dispatch call returns its `EObject`,
so you can capture it in a `def` variable and reuse it later in the script
(typically as a reference target or a super type):

```groovy
ePackage {
    name 'familymodel'

    def namedElement = eClass {       // capture the EClass
        name 'NamedElement'
        eAttribute { name 'name'; eType 'EString'; lowerBound 1; upperBound 1 }
    }

    eClass {
        name 'Family'
        eSuperTypes namedElement      // reference the captured EObject directly
    }
}
```

`eSuperTypes namedElement` passes an `EObject` straight through - no name
resolution needed because you already hold the object.

#### 1.7 References and name resolution

A reference value (an `eType`, an `eSuperTypes`, an `eOpposite`, or any
non-containment reference) can be given as:

1. an **`EObject`** you already hold (a `def` variable) - used as-is;
2. a **string selector** - a relative path or cross-resource URI;
3. a **closure selector** - navigate the built model programmatically.

String and closure selectors are resolved **deferred**: the whole script is
built first, then every reference is resolved against the finished model.
That is why you can refer forward to an element defined later in the file.

##### Relative paths (string selectors)

A string selector is an EMF URI-fragment-style path. Each `/`-separated step is
a standard EMF fragment segment, so attribute predicates, indexes and eKeys all work:

```groovy
// Anchored at the resource root ('/'): walk eClassifiers, match by name predicate
eType "/eClassifiers[name='Person']"

// Relative to THIS element, with '..' walking up to the container:
eOpposite "../eStructuralFeatures[name='children']"
```

Path rules:

- **Leading `/`** - anchored at the root container of the current element.
- **No leading `/`** - relative to the current element itself.
- **`..`** - navigate to `eContainer()` (errors if it steps past the root).
- **`.`** - a no-op step, used as the `./<name>` prefix to **disambiguate** a
  classifier name from a metamodel classifier of the same name (see below).
- Each segment's leading `@` is optional - `eClassifiers[name='Person']` and
  `@eClassifiers[name='Person']` are equivalent. EMF requires predicate values
  to be **quoted**: `[name='Person']`.
- **Contains `#`** - treated as a (possibly relative) cross-resource URI and
  resolved through the resource set (this is where global objects and proxies
  live); the URI is resolved against the script's own base URI.

> Inside a double-quoted Groovy string the single quotes in `[name='Person']`
> need no escaping. Inside a single-quoted Groovy string, escape them:
> `'/eClassifiers[name=\'Person\']'`. Triple-quoted strings avoid the issue.

##### Closure selectors

Instead of a path, hand `eType` a closure. It is evaluated deferred with the
reference element as its delegate (`DELEGATE_FIRST`) and as its single argument,
so you can navigate EMF directly. It may return an `EObject` **or** a path
string (which is then resolved as above):

```groovy
eReference {
    name 'primaryResident'
    // eContainer() is the EClass; its eContainer() is the EPackage:
    eType { eContainer().eContainer().getEClassifier('Person') }
}
```

##### Type/classifier name resolution: simple, uncapitalized, qualified, hash

When a reference is given a **bare name** (no `/`), the loader first tries to resolve it as a metamodel **classifier** via the `EPackageResolver`.
A name may take one of these forms:

| Form | Example | Resolution |
|------|---------|------------|
| **Simple** | `'Person'` | The class named `Person`. Resolved within the package of the current element first, then globally. |
| **Uncapitalized** | `'person'` | Same as simple - names match both exactly and decapitalised, so `Person` and `person` both resolve to the `Person` class. Decapitalised names are also what the **entry-point keywords** use (`eClass`, `ePackage`). |
| **Qualified** | `'c4.Component'`, `'architecture.c4.Component'` | The trailing segment is the class name; the leading segments are an `EPackage` name path matched against the package's full path *or any trailing sub-path of it*. Use this to disambiguate a simple name shared by several packages. |
| **URI (hash)** | `'http://www.eclipse.org/emf/2002/Ecore#EClass'` | `<nsURI>#<ClassName>` - the package is looked up by namespace URI (in the resolver's packages, then the resource set's package registry), then the classifier by name. Always unambiguous across a whole resource set. |

```groovy
// Simple - EString classifier from EcorePackage:
eAttribute { name 'street'; eType 'EString' }

// URI / hash form - fully qualified, never ambiguous:
def person = eObject('http://www.eclipse.org/emf/2002/Ecore#EClass') {
    name 'Person'
}
```

**Ambiguity.** If a simple name matches concrete classes in more than one
package and there is no current-package match to break the tie, resolution
fails with an error listing the qualified alternatives - qualify the name (use
the dotted or hash form) to fix it.

**Name collisions with paths.** A bare name is tried as a metamodel classifier
*before* being treated as a model path. If a local element shares its name with
a metamodel classifier and you mean the **local** one, prefix with `./` to force
path resolution:

```groovy
eType "./MyLocalType"     // resolve as a path step, not as a metamodel classifier
```

`eObject('<name>') { … }` accepts the same simple / qualified / URI name forms
and is the way to instantiate a type whose simple name is ambiguous, or to be
explicit about which package a type comes from.

#### 1.8 Global objects and cross-resource references

`global '<uri>'` registers the current element under a URI so it can be
referenced from other resources in the same resource set:

```groovy
ePackage {
    name 'test'
    global 'urn:test'                 // the package itself

    eObject('http://www.eclipse.org/emf/2002/Ecore#EClass') {
        name 'Person'
        global 'urn:test/Person'      // a class, globally addressable
    }
}
```

Another resource can then resolve `urn:test/Person`, and within a script a
`#`-bearing selector or `ref('<uri>')` reaches across resources.

> `global` is a *real method* on the builder, so `global '<uri>'` always
> registers - it wins over feature/type dispatch. If your metamodel happens to
> declare a feature literally named `global`, that feature is still settable via
> the assignment form `global = value` (which routes through property
> dispatch, not the method).

#### 1.9 Script return value

The contents of the resulting resource come from what the script produces, in
this order:

1. If the script's **last expression** is an `EObject`, an array, or an iterable
   of `EObject`s, those are flattened and used.
2. Otherwise the **roots** created through entry points (`ePackage { }`, …) are
   used.

So a single root needs no explicit return - the trailing `ePackage { … }`
expression *is* the value. To emit several roots, return a list literal:

```groovy
[
    ePackage { name 'test' /* … */ },
    ePackage { name 'test-2' /* … */ }
]
```

#### 1.10 Error diagnostics

Build and resolution failures are tagged with their source location (URI and
line) before being rethrown as a `DslException`. Deferred reference failures -
an unresolved path, or a target whose type doesn't match the reference - report
the line the reference was authored on, even though resolution runs after the
script finishes. The reported column is always `-1` (the Groovy runtime does not
expose it).

#### 1.11 Saving DSL resources

By default a Groovy DSL resource is **read-only**: it loads a `.groovy` script into an EMF model,
but saving it throws `UnsupportedOperationException`. This is the right default because the script -
not the in-memory model - is the source of truth, and there is no general way to serialize an
arbitrary model back into DSL syntax.

A script opts into saving by registering an **`onSave`** callback. Because the callback is defined in
the script itself, it can persist wherever the script's data actually lives: back into the `.groovy`
source (a *self-writing script*), or into an external system such as Jira or a SQL database.

##### The `onSave` callback

```groovy
onSave { source, outputStream, options ->
    // ...
}
```

The callback receives three arguments:

| Argument       | Description                                                                                     |
|----------------|-------------------------------------------------------------------------------------------------|
| `source`       | The original `.groovy` script text this resource was loaded from.                               |
| `outputStream` | The target the content is written to (the resource URI - i.e. the `.groovy` file - by default). |
| `options`      | The save options `Map` passed to `Resource.save(...)`.                                          |

Registering a callback replaces any previously registered one - the last `onSave` wins.
If no callback is registered, `save` throws `UnsupportedOperationException`.

> **Truncation caveat.** EMF opens the output stream against the resource URI (the `.groovy` file)
> *before* the callback runs, which truncates the file immediately. A callback that writes nothing
> therefore leaves the source **empty**. Write `source` back to preserve it.

##### Patterns

###### 1. Verbatim write-back with side effects

The common case for scripts that source their data from an external system: do the real persistence
as a side effect, then write the source back unchanged so the `.groovy` file is preserved.

```groovy
ePackage {
    name 'familymodel'
    nsURI 'https://family.models.nasdanika.org'
    // ...
}

onSave { source, outputStream, options ->
    // Real persistence lives in the side effect - e.g. push to Jira or a database.
    jiraClient.updateIssues(dsl.roots)

    // Preserve the script file.
    outputStream.write(source.getBytes('UTF-8'))
}
```

###### 2. Self-modifying script

The script rewrites its own source, e.g. appending DSL fragments for newly discovered or modified
objects so the next load picks them up.

```groovy
onSave { source, outputStream, options ->
    def fragment = discoverNewPeople().collect { person ->
        """
        eClass {
            name '${person.name}'
        }
        """.stripIndent()
    }.join('\n')

    outputStream.write(source.getBytes('UTF-8'))
    outputStream.write(fragment.getBytes('UTF-8'))
}
```

###### 3. Read-only (default)

Register no callback. Saving the resource throws `UnsupportedOperationException`.

```groovy
ePackage {
    name 'savemodel'
    nsURI 'urn:test-save'
    // ... no onSave block
}
```

##### Triggering a save

Saving goes through the standard EMF `Resource.save(...)` API. Note the two forms:

```java
Resource groovyResource = resourceSet.getResource(
        URI.createFileURI("family.ecore.groovy"), true);

// (a) Save to the resource URI (the .groovy file). EMF opens - and truncates - the file first,
//     so an onSave callback must write `source` back to avoid emptying it.
groovyResource.save(null);

// (b) Save to a caller-supplied stream. No truncation of the source file; useful for tests or
//     for capturing the written content.
ByteArrayOutputStream out = new ByteArrayOutputStream();
groovyResource.save(out, null);
String written = out.toString(StandardCharsets.UTF_8);
```

##### How it works

- On **load**, the handler buffers the script source (the source handler consumes the stream to
  compile it and does not retain the text) and stores it, then evaluates the script - which may
  register an `onSave` callback via the `DslContext`.
- On **save**, the handler delegates to `DslContext.save(source, outputStream, options)`, which
  invokes the callback. If none was registered it throws `UnsupportedOperationException`, matching the
  default `ResourceContentsHandler.save` behaviour.
- A single handler instance serves both load and save for a resource's lifetime, so the captured
  source and callback are available at save time.

---

### 2. Developer guide

This section shows how to add DSL support for a **new metamodel** - i.e. a new file qualifier (like `ecore`) that binds the generic reflective DSL to a
specific `EPackage` and installs that package's classes as entry points. 
The worked example is the Ecore binding itself: `EcoreResourceContentsHandler` and `EcoreResourceContentsHandlerCapabilityFactory`.

#### 2.1 The handler / factory pair

Two classes work together per metamodel:

- A **`ResourceContentsHandler<EObject[]>`** subclass of `DslResourceContentsHandler`.
  Its only job is to supply the metamodel (one or more `EPackage`s, or a custom
  resolver) to the generic base. The base does everything else: resolve the
  upstream source handler to a `CompiledScript`, build a `DslContext`, install
  the entry points into the script bindings, evaluate, resolve deferred
  references and normalise the result to `EObject[]`. The resolver may provide custom 
  bindings by overrriding `bindings()` method.

- A **`ServiceCapabilityFactory`** that decides *which file qualifier* this
  handler serves, requests the upstream `.groovy` source handler, and wires the
  two together.

#### 2.2 Writing an EPackage-specific handler

Subclass `DslResourceContentsHandler` and pass your metamodel's `EPackage`(s) to
the protected constructor. That's the entire handler:

```java
package org.nasdanika.groovy;

import javax.script.CompiledScript;

import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.nasdanika.capability.emf.ResourceContentsHandler;

public class EcoreResourceContentsHandler extends DslResourceContentsHandler {

    public EcoreResourceContentsHandler(Resource resource,
            ResourceContentsHandler<CompiledScript> sourceHandler) {
        super(resource, sourceHandler, EcorePackage.eINSTANCE);   // <-- the metamodel
    }
}
```

The base constructor that takes `EPackage...` wraps them in an `EPackageResolver`
bound to the resource's `ResourceSet`. The resolver is what turns those packages
into:

- the set of **entry-point keywords** (`names()` - every concrete class with an
  unambiguous decapitalised simple name);
- **classifier-by-name** lookup for the simple / qualified / URI forms described
  in [§1.7](#typeclassifier-name-resolution-simple-uncapitalized-qualified-hash);
- **global** registration / `get(id)` against the resource set.

To bind to your own metamodel, pass *its* `EPackage` instead of
`EcorePackage.eINSTANCE` (you can pass several to merge packages into one DSL):

```java
super(resource, sourceHandler, MyMetamodelPackage.eINSTANCE);
// or
super(resource, sourceHandler, FooPackage.eINSTANCE, BarPackage.eINSTANCE);
```

You can also override the `bindings()` method to supply custom bindings.
For example, you can load a capability from the capability loader which creates a handler.
This allows loading data from external sources.
I.e. the script resource becomes a data reference - it does not hold the data but the configuration and logic for how to get the data.
For example, a [Jira](https://jira.models.nasdanika.org/) or [SQL](https://sql.models.nasdanika.org/) database.
In the case of Jira, the loaded information would be projected onto the Jira metamodel.
In the case of SQL, an Ecore model can be generated from the SQL metadata - statically or dynamically - and the loaded data can be projected onto that metamodel.

#### 2.3 Writing the capability factory

The factory does three things: (1) declare which requirement it satisfies via
`isFor`, (2) request the upstream source handler that produces a
`CompiledScript`, and (3) construct your handler around it.

```java
public class EcoreResourceContentsHandlerCapabilityFactory
        extends ServiceCapabilityFactory<ResourceContentsHandler.Requirement,
                                          ResourceContentsHandler<EObject[]>> {

    @Override
    public boolean isFor(Class<?> type, Object serviceRequirement) {
        return ResourceContentsHandler.class.equals(type)
            && serviceRequirement instanceof ResourceContentsHandler.Requirement req
            && EObject[].class.equals(req.getContentsType())
            && req.getQualifiers().length > 0
            // the qualifier at the current index selects this handler:
            && "ecore".equalsIgnoreCase(req.getQualifiers()[req.getQualifierIndex()]);
    }

    @Override
    protected CompletionStage<Iterable<CapabilityProvider<ResourceContentsHandler<EObject[]>>>>
            createService(Class<ResourceContentsHandler<EObject[]>> serviceType,
                          ResourceContentsHandler.Requirement serviceRequirement,
                          Loader loader, ProgressMonitor progressMonitor) {

        // Ask for a CompiledScript from the qualifier to our LEFT (index - 1),
        // i.e. the '.groovy' source handler.
        ResourceContentsHandler.Requirement sourceReq = ResourceContentsHandler.createRequirement(
                serviceRequirement.getResource(),
                CompiledScript.class,
                serviceRequirement.getQualifiers(),
                serviceRequirement.getQualifierIndex() - 1);

        var sourceServiceReq = ServiceCapabilityFactory.createRequirement(
                ResourceContentsHandler.class, null, sourceReq);

        return loader.load(sourceServiceReq, progressMonitor)
                .thenApply(providers -> createHandler(providers, serviceRequirement.getResource()));
    }

    protected EcoreResourceContentsHandler createHandler(
            Resource resource, ResourceContentsHandler<CompiledScript> sh) {
        return new EcoreResourceContentsHandler(resource, sh);   // <-- your handler
    }
    // createHandler(Iterable<...>, Resource) maps each provider - see the source file.
}
```

Key points when adapting for your own qualifier:

- **`isFor`** - match the qualifier name your files use. For `foo.groovy` files
  carrying a `foo` qualifier, compare against `"foo"`. The Ecore factory matches
  the qualifier *at the current index* (`getQualifierIndex()`); the generic
  `DslResourceContentsHandlerCapabilityFactory` instead matches `groovy` at index `0` and serves plain `.groovy` files.
- **`createRequirement(..., getQualifierIndex() - 1)`** - this is the chaining
  step. You request a `CompiledScript` from the handler one qualifier to the
  left (the `.groovy` source), which is what your handler then evaluates.
- **`createHandler`** - return *your* handler subclass so it is bound to *your*
  metamodel.

#### 2.4 Registering the factory

Capability factories are discovered through `ServiceLoader`. Add your factory to
the `provides CapabilityFactory with …` clause in
`module-info.java`:

```java
provides CapabilityFactory with
    CompiledScriptResourceContentsHandlerCapabilityFactory,
    DslResourceContentsHandlerCapabilityFactory,
    EcoreResourceContentsHandlerCapabilityFactory,    // <-- your factory here
    GroovyResourceFactoryCapabilityFactory;
```

On the classpath (non-modular) instead add the fully-qualified class name to
`META-INF/services/org.nasdanika.capability.CapabilityFactory`.

Once registered, loading `something.<your-qualifier>.groovy` through a
Nasdanika resource set automatically runs your DSL.

#### 2.5 Custom resolvers (beyond a fixed EPackage list)

If a static list of `EPackage`s is not enough - e.g. you want to resolve types
from a registry, apply naming conventions, or change how globals are stored -
implement
`DslContext.Resolver` directly and pass it to the other `DslResourceContentsHandler` constructor:

```java
public DslResourceContentsHandler(
        Resource resource,
        ResourceContentsHandler<CompiledScript> sourceHandler,
        Resolver resolver) { … }
```

A `Resolver` must provide:

| Method | Responsibility |
|--------|----------------|
| `classByName(base, name)` | Concrete `EClass` for a simple/qualified/URI name (entry points, `eObject('…')`, type dispatch). |
| `classifierByName(base, name)` | Any `EClassifier` (incl. data types) for the same name forms - used when resolving bare-name reference selectors. |
| `classByInstanceClass(base, clazz)` | `EClass` for a Java instance class token. |
| `candidates(base, featureType, targetType)` | Concrete subtypes usable as reference *wrapper* types (for the containment-reference policy). |
| `names()` | Map of decapitalised name → `EClass` for the **unambiguous** classes to install as entry-point keywords. |
| `global(id, element)` / `get(id)` | Register / look up globally addressable objects. |

The provided `EPackageResolver` (extending `AbstractResolver`,
which implements `global`/`get` against a `NasdanikaResourceSet`) is a complete
reference implementation of all of the above - start by subclassing or copying
it rather than implementing `Resolver` from scratch.
