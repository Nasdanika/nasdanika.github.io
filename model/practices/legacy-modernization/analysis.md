
Analysis is the first phase of legacy modernization. 
It establishes the foundation: who has stake in the work, what artifacts exist, how they are structured, and how they execute. Decisions made in subsequent phases are bound to the depth and accuracy of analysis. 
Insufficient analysis is the most common cause of legacy modernization failure — not insufficient engineering.

## Identify stakeholders and capture concerns

Begin with explicit stakeholder identification. Legacy systems usually have:

- **Business subject-matter experts** with knowledge of business logic and edge cases.
- **Operations engineers** with knowledge of production behavior and incident history.
- **Original developers** (often unavailable or partially available) with implementation history.
- **Current maintainers** with ongoing operational experience.
- **Security and compliance owners** with knowledge of audit and control requirements.
- **Target-state architects** representing the destination.
- **Delivery management** representing time and budget.
- **End users** of systems integrated with the legacy system.

For each stakeholder, capture concerns explicitly. Concerns typically include:

- **Goals** — what they want from modernization.
- **Pain points** — what currently doesn't work.
- **Risks** — what they fear will go wrong.
- **Constraints** — what they cannot give up.

Author the stakeholder model as a [Nasdanika Product Management](https://product-management.models.nasdanika.org/) instance — personas with concerns, linkable to capabilities the modernization will deliver. 
Authoring the model in YAML or Xcore as a Maven artifact makes it composable with other modernization artifacts: capabilities can reference applications, applications can reference processes, and so on.

A typical analysis-phase output: a stakeholder-concern matrix showing which capabilities address which concerns. 
Conflicts between concerns appear as cells where two stakeholders' concerns pull in opposing directions; 
surface these conflicts explicitly rather than deferring them.

## Document the system context

Document the system in its surrounding context. 
The system being modernized is rarely standalone — it has upstream dependencies, downstream consumers, and lateral integrations.

For visual documentation you can use:

* [Draw.io site](https://nasdanika-templates.github.io/drawio-site/) - see [Internet Banking System](https://nasdanika-demos.github.io/internet-banking-system/index.html) demo.
* [C4 model](https://architecture.models.nasdanika.org/references/eSubpackages/c4/index.html) - see [Internet Banking System Architecture](https://nasdanika-demos.github.io/internet-banking-system-c4/cerulean/index.html) demo.

For each integration point, capture:

- **Direction** — inbound, outbound, bidirectional.
- **Protocol** — HTTP/REST, JMS, SOAP, file transfer, database, custom.
- **Coupling** — synchronous, asynchronous, batch.
- **Volume** — transactions per unit time, peak versus average.
- **Criticality** — would the consumer fail without this integration?

Integration points often constrain modernization choices more than the legacy system's internals do. 
A system with 50 inbound REST consumers can be modernized internally without external coordination; a system with 50 outbound JMS producers requires coordination with each downstream team.

## Inventory artifacts

Scan the legacy system and count artifacts by type. For each artifact type, capture:

- **File extension and format** — binary, XML, JSON, properties, source code.
- **Count** — the order of magnitude matters: 50 versus 5,000 changes the modernization approach materially.
- **Structure** — what schemas govern the artifact, what cross-references exist.
- **Loading mechanism** — parser, deserializer, classloader.

Typical legacy enterprise artifacts include:

- Java class files (loaded with [ASM](https://asm.ow2.io/) or similar)
- XML process definitions (loaded with StAX or DOM)
- XML configuration and properties files
- XSD schemas
- SOAP / WSDL service definitions
- Database schemas
- Shared library JARs

The count and structure of artifacts dictate the loader implementation effort.
A system with 2000 Java classes and 100 process XMLs requires loaders for both;
a system with 20 process XMLs and no custom Java classes requires only one. 
The order-of-magnitude ratio between artifact count and team size is a useful sanity check on modernization timeline assumptions.

## Document the metamodel

For each artifact type, document the internal structure. 
This becomes the foundation for the metamodel that subsequent phases (DSE, transformation) build against.

Capture for each artifact type:

- **Element types** — in XML, the element names; in code, the class kinds.
- **Attributes and references** — what fields each element has.
- **Cross-references** — how elements refer to other elements, both within and across artifacts.
- **Validation rules** — XSD constraints, business rules embedded in code conventions.
- **Documentation conventions** — where comments and metadata live.

A documented metamodel is an analysis-phase deliverable. 
Author it in [Xcore](https://github.com/Nasdanika-Templates/xcore-doc) and render it as a documentation site automatically using the xcore-doc template. 
The metamodel is what subsequent phases build against; investing in its quality early compounds later.

GenAI can accelerate metamodel authoring by analyzing sample artifacts and proposing structure. 
Validate outputs against the legacy system's actual conventions, which often deviate from formal specifications. 


## Document the execution model

Document how the legacy system actually executes. For workflow or integration engines, this typically includes:

- **Entry points** — what triggers execution: scheduled, message-driven, request-driven.
- **Execution units** — processes, jobs, tasks.
- **State management** — persistent versus ephemeral, where state lives.
- **Concurrency model** — thread pools, event loops, pooled connections.
- **Transaction boundaries** — where rollback semantics apply.
- **Failure modes** — retries, dead letters, circuit breakers, escalations.
- **Telemetry** — what observability exists today.

The execution model is the most-frequently-underestimated aspect of analysis. 
Engineers who have never operated the legacy system in production routinely propose modernization approaches that violate execution-model invariants the legacy system has held for years. 
Document these invariants explicitly so they can be referenced in subsequent design decisions.

### OpGraph as a generalized execution metamodel

For workflow and integration systems, the [OpGraph model](https://op-graph.models.nasdanika.org/) provides a generalized execution metamodel. 
Many XML-driven workflow engines map cleanly onto OpGraph concepts:

- **Nodes** correspond to Java `Supplier`/`Function`/`Consumer` and to Reactive Streams `Publisher`/`Processor`/`Subscriber`. They represent units of work.
- **Calls** carry request/reply semantics — invoke a target with input, receive a result. Functions or Processors may adapt arguments and results between source and target.
- **Transitions** carry fire-and-forget semantics — pass payload from source to target without expectation of return. A Function/Processor may transform payload along the transition.
- **Composite Suppliers, Functions, and Consumers** contain other elements and delegate. They support hierarchical decomposition.
- **Iterations and transactions** can be defined at the element level, at a composite level (the composite wraps elements), or at a call/transition level.
- **Job** represents execution data including position(s) — tokens — within the execution. Jobs may be ephemeral (in-memory) or persistent. Job implementations are pluggable through the [Nasdanika Capability framework](https://docs.nasdanika.org/core/capability/index.html) and Maven dependencies. Jobs are logically similar to Git repositories: changes in job state correspond to commits, jobs can be in-memory, local-persistent, or remote (with origins and push-triggered distribution).
- **Token** represents an execution position within a Job. A token has associated state (logically owned by the token, physically stored in the Job) and a current model element. Tokens are essential for iterations, retries, transactions, and restarts. They are similar to Git commit pointers and to [Nasdanika Graph Messages](https://github.com/Nasdanika/core/blob/master/graph/src/main/java/org/nasdanika/graph/message/Message.java) (which currently support a single parent and should be extended to support merges).

Mapping the legacy execution model onto OpGraph is itself an analysis activity. 
Concepts that don't map cleanly indicate either gaps in the metamodel (worth extending OpGraph for) or characteristics of the legacy engine that warrant explicit documentation as one-off semantics.

## Establish baseline test corpus

During analysis, begin capturing representative production traffic for use as a regression baseline in subsequent phases.
The corpus should cover:

- **Typical execution paths** — high-frequency cases.
- **Edge cases** — low-frequency but operationally important cases.
- **Failure modes** — known error paths and their expected behaviors.
- **Boundary conditions** — large inputs, empty inputs, malformed inputs.

The corpus is an asset that pays compounding dividends. DSE uses it to validate semantic equivalence with the legacy system.
Phase 2 (Model Transformation and Generation) uses it to validate equivalence between DSE-rehosted and transformed implementations.

## Capture decisions

Decisions made during analysis should be captured with traceability to:

- Stakeholder concerns they address.
- Architecture elements they affect.
- Other decisions they depend on or block.

A simple decision register integrated with an issue tracker (Jira or equivalent) is sufficient for most engagements.
For non-trivial choices, structured decision analysis using the [Nasdanika MCDA model](https://mcda.models.nasdanika.org/) is appropriate — alternatives, criteria, weights, and scores explicitly captured rather than buried in meeting notes.

Decisions can be ordered by *cost of delay* — how much value is unlocked by making the decision now versus deferring it.
Decisions blocking other decisions should be made first; decisions that block external teams (and therefore have long resolution lead times) should be surfaced earliest. 
Decisions requiring involvement of external teams or parties should be well-formulated, backed by research, and traceable to what they unblock.

## Architecture documentation

The output of analysis is an architecture documentation site that the rest of the practice references. Several composition options:

- **Site federation** — architecture documentation and legacy artifact documentation have independent lifecycles and well-defined URLs. They cross-reference each other.
- **Mounting** — documentation HTML application models generated from legacy definitions are mounted into the architecture documentation, the way [Nasdanika CLI documentation](https://docs.nasdanika.org/nsd-cli/index.html) is mounted into the [Nasdanika documentation site](https://docs.nasdanika.org/index.html).
- **Semantic model integration** — the system being migrated is modeled in C4 or other metamodel. Deployable legacy artifacts are modeled as containers within the system. Generated artifact documentation is linked from the corresponding container.

The semantic model approach has the additional benefit of supporting AI reasoning over the holistic picture — capability mapping, duplication detection, modernization impact analysis, and SME knowledge elicitation all become AI-tractable when the input is structured.

## Other documentation

Documentation outputs that compose into the architecture site:

- **Database schema documentation** — when the system has database dependencies, use the Nasdanika CLI to generate schema documentation that integrates with the architecture site.
- **XSD documentation** for any XML schemas the system exposes or consumes.
- **PlantUML diagrams** if there are PlantUML diagrams describing the system.
- **Visio diagrams** with a relatively little effort it is possible to create a component/command which reads Visio diagrams and either  converts them to, say, [Draw.io](https://docs.nasdanika.org/core/drawio/index.html) format or [maps](https://docs.nasdanika.org/core/mapping/index.html) to the architecture model. This approach is justified if there is a large number of diagrams which, makes manual migration more expensive, or if the diagrams are still maintained and have to remain sources of truth. 
- **JSON Schema documentation** for any JSON-based contracts.
- **Xcore models elicited from prompts and existing documents using GenAI** — particularly useful when the legacy documentation is incomplete.

These documentation outputs are independently composable.
Each can ship as its own Maven artifact and be mounted into a federated documentation site.

## Analysis deliverables

A complete analysis phase produces:

- A stakeholder/concern model with traceability to capabilities (Nasdanika Product Management).
- A system context model (C4) with documented integration points.
- An artifact type inventory with counts and loading mechanisms.
- A documented metamodel for each artifact type (Xcore, rendered as a documentation site).
- A documented execution model, mapped onto OpGraph where applicable.
- A baseline regression test corpus.
- A decision register integrated with the issue tracker.
- An architecture documentation site that composes all of the above.

These deliverables are the input to the next phase.
