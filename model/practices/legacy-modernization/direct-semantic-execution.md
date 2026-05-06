
Direct Semantic Execution (DSE) — known industrially as **rehosting** or **lift-and-shift** — is the practice of executing legacy artifacts AS-IS by building a runtime engine that loads them, resolves their cross-references, validates them, and executes them with their original semantics preserved.

DSE is Phase 1 of the recommended two-phase modernization approach. 
It removes the dependency on the legacy vendor's runtime while preserving legacy semantics exactly. 
Phase 2 ([Model Transformation and Generation](transformation-and-generation.html)) modernizes the runtime gradually after Phase 1 is delivered.

## When DSE is the right choice

DSE is the right Phase 1 choice when:

- There is a hard deadline to be off the legacy vendor's stack.
- The legacy system is large enough that activity-by-activity rewriting would not meet the deadline.
- The team has deep knowledge of the legacy semantics but lacks the resource depth to rewrite at scale.
- The business logic is stable — modernization is a runtime move, not a business rules redesign.
- Risk tolerance is low — semantic equivalence with the legacy is required.

DSE is the wrong choice when:

- The legacy semantics are themselves the problem (the business needs different behavior).
- The legacy artifacts are unrecoverable — corrupted, missing, or undocumented to the point of being unloadable.
- The team has resource depth to rewrite and a target architecture that demands it.

## Trade-offs

DSE is conservative by design. The trade-offs are well-defined:

- **Lower risk than rewrite.** Semantic preservation is mechanical rather than interpretive. Behavior changes are minimized.
- **Faster delivery than rewrite.** Building a runtime engine for a closed set of element types is bounded work; rewriting all the elements is unbounded.
- **Larger long-term technical debt.** The modernized system still carries the legacy element model, even if it no longer uses the legacy vendor's runtime.
- **Phase 2 mitigates the technical debt.** Once DSE is delivered, transformation/generation can gradually replace legacy elements with target-runtime artifacts per-deployment-unit.

The trade-off framing is essential. 
DSE is not a final state for most systems; it is a *bridge* state that removes vendor dependency and
creates conditions for incremental modernization.
Senior architects who object to DSE on grounds of long-term debt are usually objecting to it as a final state, which it is not.

## Bounded scope: implement what is used, not what exists

A common misconception about clean-room reengineering is that it requires re-implementing the entire legacy runtime. 
It does not. The DSE engine implements only the subset of the legacy runtime that the system being modernized actually uses.

Concretely:

- A legacy product may offer dozens of ways to work with JMS messaging; the system may use one or two of them. Only one or two processors need implementations.
- A workflow engine may support a wide range of transaction modes; the system may use only one or two. Only those modes need preservation.
- A messaging primitive may have many advanced features; the system may use basic JMS settings. Only the basic settings need engine support.

This bounded scope is what makes DSE feasible inside a hard deadline. 
The unbounded version - re-implementing every feature the legacy vendor ever shipped - is genuinely infeasible. 
The bounded version - re-implementing what's actually used - is bounded engineering with a deterministic completion criterion.

The analysis-phase artifact inventory drives this scoping: count activity types in use, enumerate the parameter sets that appear in production, 
document the transaction modes the system depends on. 
Each count becomes a concrete chunk of engineering work with a clear scope.

This bounded scope also clarifies risk versus rewrite alternatives. 
Rewriting 1000 processes containing 5,000 activity instances is materially riskier than implementing 50 activity processors and validating each against its 100-or-so usages in the existing process definitions. 
The math favors implementing fewer things deeply over translating more things shallowly.

## Engine architecture

A DSE runtime engine consists of several layers, separated by responsibility.

### Loaders

Loaders populate the model from legacy artifacts. Typical implementations:

- **StAX** (Streaming API for XML) for XML-driven legacy definitions. Streaming is preferred over DOM for large artifacts.
- **[ASM](https://asm.ow2.io/)** for loading and analyzing class files.
- **Format-specific readers** (Excel, CSV, properties files) for tabular and configuration data.

Loaders produce Ecore model instances.
The instances reference each other via Ecore proxies and *logical URIs* — for example, a `java:com.myorg.MyClass` reference points to a Java class regardless of where the class file is stored physically. 
Logical URIs are resolved at load time or on demand.

GenAI can accelerate loader generation: given a metamodel and sample artifacts, generate the parsing code, then validate against representative inputs. 

Loading might be:

* Partial - only what matters
* One-way - no writing back to the legacy format

### Resource factories

Wrap loaders for URI-based artifacts in Ecore resource factories associated with file or URI extensions. 
This allows model loading from a wide variety of locations:

- Local filesystem.
- Maven repositories via the [Nasdanika Maven URI Handler](https://docs.nasdanika.org/core/maven/index.html#uri-handler).
- GitLab repositories via the Nasdanika GitLab URI Handler.
- HTTP URLs.

Composing these handlers gives federated model loading: a deployment-unit definition in a local repository can reference a shared library in Maven via a `mvn:` URI, which can reference a configuration file in GitLab via a `gitlab:` URI. 
Consumer code is unaware of where each artifact actually lives.

### Validation

After loading, validate the model. Validation typically catches:

- Cross-reference resolution failures — an artifact references a class or schema that doesn't exist.
- Schema violations — XML doesn't conform to its XSD.
- Business rules embedded in legacy conventions — for example, property names that must follow a pattern.

Treat diagnostics from validation as first-class artifacts.
Validation surfaces latent issues in the legacy system that may have been masked by the legacy runtime's tolerance.
Surfacing these as part of analysis pays compounding dividends — the same diagnostics inform subsequent phases.

### Adapter factories and adapters

One way to implement executability is to use adapter factories and adapters. 
This pattern is used by the Nasdanika HTML application generation pipeline: a model element is adapted to an executor that knows how to handle that element type. 
Adapters can be registered statically or discovered dynamically through the capability framework.

### Processors

Each model element type has a corresponding *processor* — a Java class that executes that element type at runtime.
The processor architecture has several layers, each chosen for a specific reason.

**Component**: lifecycle management. `start`/`stop` methods and their async counterparts (`startAsync`/`stopAsync`) returning `CompletionStage<Diagnostic>` for start and `CompletionStage<Void>` for stop. Start/stop can be parallelized but does not need backpressure — `CompletionStage` is the right abstraction. Reactive Streams `Publisher` or Project Reactor `Mono` would be over-engineered for lifecycle.

**Publisher**: for elements that emit job requests — for example, a JMS listener or an HTTP endpoint. Implements `org.reactivestreams.Publisher` directly, not `Flux` or `Mono`. Implementing the raw `Publisher` interface preserves contract compatibility with downstream consumers regardless of which reactive library they use; `Flux.from(publisher)` can wrap it on the consumer side. Publishers start emitting on subscription, and subscription happens only after all components have started — this prevents events from arriving before downstream processors are ready. A JMS listener, for example, opens its connection in `start()` but begins receiving messages on subscription.

**Processor / Function**: for elements that transform input to output. Synchronous form: `O process(I input, C context)`. Async form: `Mono<O> processAsync(I input, C context)`. `Mono` is appropriate because individual processing steps are single-result; `Flux` is used only when iteration produces multiple results. The async form leverages Project Reactor's backpressure for actual data flow, where backpressure matters.

`C` is the context, providing access to the token (or being the token), the job state, this token's state, and the telemetry context.

#### Why these layer choices

The lifecycle/event-emission/processing layer split corresponds to three different concurrency requirements:

- **Lifecycle** is fan-out parallelism with no backpressure — `CompletionStage` is the simplest abstraction that supports it.
- **Event emission** crosses framework boundaries — raw `Publisher` is the lowest common denominator.
- **Processing** is the load-bearing data flow — `Mono`/`Flux` provides the backpressure machinery the data path actually needs.

Mixing these layers is a common error. 
Using `Mono`/`Flux` for lifecycle adds backpressure machinery that lifecycle doesn't need; 
using `CompletionStage` for processing loses backpressure where it does matter.

### Iteration

Iteration is implemented by an `IterationFilter` that creates a `Flux` from the input via `Flux<E> iterate(I input, C context)`, processes elements by delegating to a target `Processor<E, R>`, and combines results via an `Accumulator` (e.g., collecting to a `List`). 
Synchronous iteration follows the same pattern with `Iterator<E>` instead of `Flux`.

`Accumulator` may have synchronous and asynchronous methods, or separate sync/async flavors, depending on whether the reduction itself can benefit from backpressure.

### Fault handling

Faults - business-level outcomes that prevent an activity from completing - are first-class control flow in many legacy workflow engines (see [Analysis: Faults versus errors](analysis.html#faults-versus-errors)). 
The DSE engine must preserve the distinction between faults and technical errors; a naive collapse of the two loses the workflow's intended routing semantics.

The runtime representation: a fault is an exception with payload. 
The exception type identifies the fault category; the payload carries the data the legacy system would have routed alongside a fault transition. 
The engine catches faults at activity boundaries and routes execution along the fault transition appropriate to the fault type, passing the payload as input to the target activity. 
Technical errors propagate to a separate error-handling layer (retries, dead letters, escalation) and do not activate fault transitions.

Implementation considerations:

- **Model fault types as first-class elements** in the metamodel, not as opaque strings. The capability framework can then dispatch fault transitions by type the same way it dispatches activity processors by element type.
- **Preserve payload typing.** Where the legacy system carries structured fault payload, the DSE engine should preserve that structure rather than collapsing it to a string. The downstream activities that handle the fault may depend on field-level access to the payload.
- **Fault transitions activate the same processor pipeline** as normal-path transitions. The only difference is the routing decision at the source activity - the downstream side is unchanged. This keeps the processor model orthogonal to the fault model.
- **Distinguish fault propagation from error propagation in telemetry.** A fault is a normal-path business outcome and should be visible as such; conflating it with an error in dashboards trains operators to ignore signals that matter.

### Capability-based processor creation

Decouple processor creation from the engine via the [Nasdanika Capability framework](https://docs.nasdanika.org/core/capability/index.html).
Processor types are registered as service types; model elements are requirement objects. The factory request is roughly: *create a `Processor` for a given `Transition` element*. The factory may itself request other capabilities — an LLM client for an Agent processor, a JMS connection factory for a JMS processor.

This decoupling allows processor implementations to be added or replaced without modifying the engine. New processor types can be contributed via Maven dependencies — the engine resolves them via the capability framework at startup.

Processor types may be passed via element properties or by subclassing the model. In legacy engines where elements have explicit types — for example, an Activity that sends an HTTP request versus one that sends a JMS message — the element type is the natural dispatch key.

### Swappable implementations: testing and quality attributes

Capability-based processor creation is more than a deployment-time abstraction. 
The same model element can have multiple processor implementations registered, and the capability framework selects
among them based on the assembly context. 
This swappability is load-bearing for testing and for matching processor implementations to specific quality attributes — without forking the workflow definitions or the activity processor logic.

#### Testing variants

Test execution typically substitutes one or more capability variants:

- **Embedded JMS** in place of a production broker. An activity that reads from JMS uses the same processor implementation; the JMS resource itself is provided by an embedded broker (ActiveMQ Artemis or similar) instead of a real one.
- **Ephemeral (in-memory) job factory** for tests. Job state lives in memory and is gone when the test ends. No cleanup required, deterministic across runs, faster than persistent variants.
- **Canned-response request/reply** for activities that call out to external services. The activity processor is unchanged; the underlying service client is replaced with one that returns pre-recorded or programmed responses.

The canned-response variant has two distinct flavors that are often conflated but serve different testing purposes:

- **Stubs** return canned data when called. They do not verify how they were called. Use stubs when the test cares about workflow behavior, not about the interaction with the dependency.
- **Mocks** are pre-programmed with expectations about how they will be called. They verify that the expected calls were made with the expected arguments. Use mocks when the test cares about correctness of the interaction itself.

Both are appropriate. Conflating them produces tests that are either over-specified (verifying calls that don't matter) or under-specified (missing real interaction errors).

#### Production variants for quality attributes

Different deployment units have different quality requirements. 
Capability variants address them without forking the workflow definitions.

- **Ephemeral job factory** for processes that don't require persistence. Many request/reply workflows and stateless transformations don't need durable state. Ephemeral jobs are faster and lighter; activity processors run identically.
- **JGit-backed job factory** for agentic flows requiring explainability. Each step in the workflow corresponds to a Git commit, so the entire execution history is auditable as a repository. For systems where "explain what the agent did" is a regulatory or operational requirement, this gives auditable provenance for free.
- **GitLab-backed job factory** built on the GitLab URI Handler or REST API. Distributed agentic flows and human-in-the-loop review benefit from GitLab's existing UI, merge-request workflows, and branch policies. A workflow that requires human approval at a step can pause, push the current state to a GitLab branch, and resume when a merge request is approved. The REST API is used directly for queries that don't require a checkout.
- **XA distributed transactions** for legacy systems that depend on two-phase commit semantics across multiple resource managers — for example, a JMS receive coordinated with a database update. XA-aware processor variants participate in the distributed transaction protocol where the legacy system requires it; non-XA variants are used where local transactions suffice. The capability framework selects variants per deployment unit, so systems with mixed transaction requirements coexist without code-level distinction. XA is heavyweight; only adopt the XA variant where the legacy system actually depends on it.

The general pattern: the workflow definition and the processor logic stay the same; the resource implementations that processors interact with — brokers, job stores, external service clients, transaction managers — vary by deployment context. 
This separation is what makes the same DSE engine deployable across testing, development, staging, and multiple production configurations without forks in the codebase.

### Job and Token

Job and Token implementations follow the OpGraph generalized model:

- **Job** holds execution state. Jobs may be ephemeral (in-memory; supports retries within an execution but not across restarts), local-persistent (file-backed; survives restarts), or remote (distributed; supports cross-process execution).
- **Token** holds execution position. Tokens are essential for retries, transactions, and restarts.

Job implementations are pluggable. 
JGit-backed jobs are particularly useful: they give Git semantics to job state, enabling distributed execution via push triggers, an audit trail of what happened, and standard Git tooling for forensic analysis.

## Telemetry and observability

Production observability is a stakeholder concern (operations engineers). 
DSE engines must emit telemetry tied to model elements so that operational tooling can show "what is currently executing" overlaid on the source artifact.

Approaches:

- **Model-based telemetry** with traces tagged by model element URI. Visualizations overlay traces on diagrams of the legacy artifacts.
- **Centralized telemetry** (OpenTelemetry, Datadog, etc.) with structured attributes carrying the model element URI as a span attribute.
- **Hybrid**: emit to both. Centralized for production monitoring, model-based for engineering troubleshooting.

The Nasdanika model-based telemetry approach (see [Model-Based Telemetry as Code](https://medium.com/nasdanika/model-based-telemetry-as-code-cd1541478be6)) integrates with the documentation generator to render trace data on model element pages, creating a unified troubleshooting surface. 
Engineers debugging a production issue can navigate from a trace span to the source artifact's documentation directly.

A useful design for the troubleshooting surface:

- **Embedded Draw.io** to display currently active components.
- **Timeline scrubber** to play execution forward and back.
- **C4 view selector** — when C4 is the architecture model, render four diagrams (or fewer, configurable) showing control propagation. Lower-level diagrams may auto-update or use tabs when control passes from one container to another.

## Documentation generation

There should be a documentation generator for the loaded model.
There is a number of examples/demos of a documentation generator, [Crew AI Swimlanes](https://nasdanika-demos.github.io/latest-ai-development-swimlanes/) being one of them.

The generated documentation site is composable with the architecture site, creating a unified surface for understanding the system being modernized.

The documentation generator can pull additional information from connected systems:

- **Issue tracker integration** — pull modernization status from Jira and auto-color architecture elements as TODO/Planned, In Progress, Done, Blocked. Show work items on element pages.
- **Telemetry back-end integration** — pull live operational data and overlay it on diagrams and element pages for system health views.

## Risk mitigation

DSE's risk profile depends on how completely the legacy semantics are preserved. Risk mitigation activities:

- **Element-type coverage analysis** — enumerate every element type in the legacy artifacts; verify a processor exists for each. Coverage gaps are blockers for cutover.
- **Regression testing against the analysis-phase corpus** — replay representative production traffic through the DSE engine and compare outputs to legacy outputs. Differences are either bugs in the engine or undocumented legacy behaviors that warrant explicit documentation.
- **Telemetry parity** — production monitoring after cutover must be at least as informative as the legacy monitoring on day one.
- **Cutover phasing** — cut over by deployment unit, not by big-bang. Each unit's cutover validates the engine against that unit's specific semantics, and a failed cutover is a per-unit rollback, not a system-wide one.

## Connection to Phase 2

DSE is Phase 1. After delivery, the system is off the legacy vendor's runtime but still uses the legacy element model.
Phase 2 ([Model Transformation and Generation](transformation-and-generation.html)) modernizes the element model gradually.

DSE's design influences Phase 2 in specific ways:

- The metamodel built during DSE is the input to Phase 2's transformations.
- Capability-based processor creation generalizes naturally to capability-based generators.
- The federated model substrate (Maven URI Handler, GitLab URI Handler) supports Phase 2 artifacts the same way it supports Phase 1 artifacts.
- The regression test corpus continues to validate equivalence as deployment units migrate.

DSE is not a dead end if Phase 2 is part of the plan. 
It is the foundation Phase 2 stands on.

