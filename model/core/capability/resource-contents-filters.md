
A `ResourceContentsFilter` is a capability that transforms the contents of an EMF [`Resource`](https://www.eclipse.org/modeling/emf/) from one form to another.
Filters operate on already-loaded model objects rather than on byte streams, and they compose into chains driven by filename qualifiers.
This section explains the pattern, what it enables, and how the Capability framework's discovery mechanism makes it composable.

## The pattern

EMF's standard `Resource.Factory` mechanism dispatches resource loading by file extension.
The Nasdanika extension generalizes this: a filename can declare a *chain* of qualifiers processed by `ResourceContentsFilter` capability that transforms the previous stage's contents into the next stage's contents.

A few examples make the pattern concrete:

- `my-product.pm.md` loads as Markdown (the `.md` factory), then is transformed to a [Product Management Model](https://product-management.models.nasdanika.org/) by the filter which can handle the `.pm` qualifier.
- `internet-banking-system.c4.drawio` loads as a draw.io diagram, then is mapped to a [C4 Model](https://architecture.models.nasdanika.org/references/eSubpackages/c4/index.html).
- `architecture.ecore.md` loads as Markdown and is transformed to an Ecore model.
- `report.html.pm.md` chains further - Markdown to Product Management model to HTML.
- `adams.family.xlsx` loads as Excel and is mapped to a family model.

Qualifiers are read right-to-left: the rightmost qualifier (position 0) is the source format, and each qualifier to the left is a transformation stage applied to the output of the previous one.

From the client code's perspective, every one of these is just `ResourceSet.getResource(uri)` returning the target model.
The conversion machinery is hidden behind the standard EMF abstraction.
Consumer code does not need to know which authoring surface produced the contents, which filters were applied, or how many stages the pipeline contained.

## Use cases

Several distinct patterns share the same `ResourceContentsFilter` machinery.

**Source-format normalization.** Different teams author the same target model from different surfaces.
A Product Management model can be authored as Markdown (`.pm.md`), as a draw.io diagram (`.pm.drawio`), or as a EMF JSON YAML file (`.yaml` ); all three produce the same target Ecore model.
Consumer code reads the model without knowing which surface produced it.
Each authoring team picks the format that fits their work; the model layer normalizes the difference.

**Cross-format composition.** A draw.io diagram can reference Markdown files via prototype references; the loaded diagram resource transitively pulls in the Markdown resources and maps both into the same target model.
An Excel file can reference a draw.io diagram that references Markdown - the pipeline composes through all three formats.
Each authoring surface plays to its strengths (geometry for the diagram, prose for the documentation, tabular data for the spreadsheet), and the filter chain composes them at load time.

**Enrichment from external systems.** A filter can call out to external services during transformation - resolving cross-references against a live catalog, looking up authoritative definitions from a federated model, invoking an LLM to produce derived contents.
The source filename does not change; the loaded model carries the enriched contents.

**Access control and projection.** A filter that knows the security principal can project content per-audience.
Different consumers loading the same source receive different target models; the consumer code does not see the projection logic.
The principal can be acquired ambiently (from OS user, environment variable, JAAS subject) for single-principal contexts, or bound explicitly to the `ResourceSet` for multi-principal server contexts (HTTP, gRPC, MCP).
The same filter implementation serves both deployment shapes - the difference is where the principal is acquired, not in the filter code.
This is the deployment shape of audience-scoped access control at the resource-loading boundary.

## Implementation approaches

A `ResourceContentsFilter` can be implemented in either of two ways depending on the complexity of the transformation.

**Declarative via NSML.** For filters which are expressible as model-to-model mapping rules in [NSML](https://github.com/Nasdanika-Models/nasdanika-semantic-mapping-language), the Nasdanika Semantic Mapping Language.
The mapping is itself a model artifact - inspectable, diagrammable, citable, AI-generable, and AI-reviewable.
Writing a new filter becomes writing a new transformation rather than writing custom Java loader code.
The bar for adding a new (source-format, target-model) pair drops to *"write the mapping rules."*

**Hand-written Java.** For filters that require complex stateful processing, cross-cutting concerns that do not fit cleanly into NSML rules, or performance-critical paths, the filter can be implemented as a Java class registered as a capability.
The two implementation approaches compose - an NSML-based filter can delegate to a Java helper, and a Java-based filter can invoke NSML for specific sub-transformations.

## Composition with CLI command pipelines

The filename pipeline composes with the [Nasdanika CLI](https://docs.nasdanika.org/nsd-cli/) command pipelines. A CLI invocation like:

```
nsd model internet-banking-system.c4.md html-app site
```

runs the filename pipeline (`.md` to Markdown to C4) to produce the input for the command pipeline (`html-app` to `site`).
Two grammars meet at the model boundary: filename grammar produces the typed model, command grammar consumes and transforms it.
The combined sentence is a single CLI invocation that hides arbitrary depth of loading and processing behind a uniform interface.
New authoring surfaces extend the filename grammar; new operations extend the command grammar; the user's invocation composes both grammars at the point of use.

## Creating and registering a Resource Content Filter

### Filter implementation

```java
public class MarkdownToEcoreResourceContentsFilter implements ResourceContentsFilter {
    
    private static final String ECORE_QUALIFIER = "ecore";

    private static final Logger LOGGER = LoggerFactory.getLogger(MarkdownToEcoreResourceContentsFilter.class);  

    @Override
    public List<EObject> load(
            Resource resource, 
            List<EObject> contents, 
            String[] qualifiers, 
            int qualifierPosition,
            Map<?, ?> options) throws IOException {
        
        if (ECORE_QUALIFIER.equals(qualifiers[qualifierPosition])) {
            return contents
                .stream()
                .flatMap(e -> e instanceof Document document ? map(resource, document).stream() : Stream.of(e))
                .toList();
        }
        return ResourceContentsFilter.super.load(resource, contents, qualifiers, qualifierPosition, options);
    }
    
    @Override
    public List<EObject> save(
            Resource resource, 
            List<EObject> contents, 
            String[] qualifiers, 
            int qualifierPosition,
            Map<?, ?> options) throws IOException {
        
        if (ECORE_QUALIFIER.equals(qualifiers[qualifierPosition])) {
            throw new UnsupportedOperationException("Saving to Markdown format is not supported");
        }
        return ResourceContentsFilter.super.save(resource, contents, qualifiers, qualifierPosition, options);
    }
    
    private List<EObject> map(Resource resource, Document document) {       
        return document.getChildren()
            .stream()
            .map(e -> {
                if (e instanceof Heading heading && heading.getLevel() == HeadingLevel.H1) {
                    return mapHeadingToEpackage(resource, document.getContent(), heading);
                }
                // TODO - add an error or a warning to the resource that only H1 headings are supported
                return e;
            })
            .toList();
    }

    private EPackage mapHeadingToEpackage(Resource resource, String markdown, Heading heading) {
        Transformer<Object,EModelElement> markdownTransformer = new Transformer<>(new MarkdownToEcoreFactory(resource, markdown));              
        try (ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER)) {
            return (EPackage) markdownTransformer.transform(List.of(heading), false, progressMonitor).get(heading);
        }       
    }

}
```

### Capability factory 

```java
public class MarkdownToEcoreResourceContentsFilterCapabilityFactory extends ServiceCapabilityFactory<Void, ResourceContentsFilter> {
    
    @Override
    public boolean isFor(Class<?> type, Object requirement) {
        return ResourceContentsFilter.class == type && requirement == null;
    }

    @Override
    protected CompletionStage<Iterable<CapabilityProvider<ResourceContentsFilter>>> createService(
            Class<ResourceContentsFilter> serviceType, 
            Void serviceRequirement, 
            Loader loader,
            ProgressMonitor progressMonitor) {
        
        return wrap(new MarkdownToEcoreResourceContentsFilter());
    }

}

```

### module-info.java

```java
provides CapabilityFactory with MarkdownToEcoreResourceContentsFilterCapabilityFactory;
```

## Related projects

- [Markdown model](https://markdown.models.nasdanika.org/)
- [NSML](https://github.com/Nasdanika-Models/nasdanika-semantic-mapping-language)
- [Product Management model](https://product-management.models.nasdanika.org/)
