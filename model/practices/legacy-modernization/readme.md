
Modernizing legacy enterprise systems is a process of *binding decisions* under risk, resource, and time constraints.
The decision space is large - there is no single best approach. 
Different points in the space match different combinations of legacy technology, target state, available expertise, deadline, and tolerance for disruption.

This practice provides a structured approach to navigating that decision space.
It assumes the modernization team operates in a real enterprise environment with limited resources, hard deadlines, and stakeholders whose concerns differ.

## Stakeholders and concerns

Modernization is rarely a single-stakeholder activity.
The practice begins by identifying stakeholders and capturing their concerns, because their concerns drive design choices.
The starter set worth carrying into any modernization engagement:

- **Business subject-matter experts** own the processes the legacy system implements. Concern: continuity of business logic and intent preservation through modernization.
- **Operations and on-call engineers** support the legacy system in production. Concern: production observability, troubleshooting access, post-modernization runbook continuity.
- **The modernization team** executes the work. Concern: deterministic translation, model-validated transformations, automated regression coverage.
- **Security and compliance** owns control posture. Concern: control gaps during transition, audit trail through modernization, equivalence of compliance posture.
- **Target-state architects** own the destination. Concern: alignment with target reference architecture, minimum bespoke surface area.
- **Delivery management** owns timeline and budget. Concern: time-to-value, visible progress, predictable milestones.

Different stakeholders weight different concerns. 
Conflicts between concerns are common and material — for example, "fastest delivery" (delivery management) often conflicts with "most modern target architecture" (target-state architects). 
The practice surfaces these conflicts as explicit trade-offs rather than burying them in technical decisions.

This stakeholder framing is one application of the [Nasdanika Product Management model](https://product-management.models.nasdanika.org/) — modernization is treated as a product, with personas, concerns, and capabilities mapped formally rather than discussed informally.

## The decision space

Software development is the process of binding decisions. 
Legacy modernization is the same process applied to existing decisions that have already been bound — by previous teams, in previous languages, against previous business requirements.

The decision space for modernization spans several axes:

- *Approach axis*: rehost, replatform, repurchase, refactor, retire, retain (the industry-standard 6Rs).
- *Phasing axis*: big-bang versus incremental cutover.
- *Coupling axis*: tight integration with the surrounding system versus clean isolation.
- *Risk-time-cost axis*: trade-offs between speed, risk reduction, and total cost.

Different points in this space match different problems. 
There is no universally correct answer. 
The practice provides a framework for making the choice explicitly rather than defaulting to whatever the team is comfortable with.

A common failure mode in enterprise modernization: decisions are made in review meetings where participants without deep familiarity with the legacy system or its constraints provide generalized guidance. 
Generic recommendations that ignore actual constraints can hurt more than they help. 
The complexity of legacy modernization decisions exceeds what informal debate-based decision-making can resolve well. 
The practice recommends structured decision analysis ([Nasdanika MCDA model](https://mcda.models.nasdanika.org/)) for non-trivial choices, with criteria, alternatives, and trade-offs surfaced explicitly.

## Per-unit selection and parallel approaches

Modernization choices apply at the deployment-unit level, not at the system level.
A system being modernized typically contains many deployment units of varying complexity, business risk, documentation quality, and team familiarity.
The right approach for one unit may differ from the right approach for another.

Two principles follow:

**Select per unit.** After the architecture is documented, evaluate each deployment unit against its own constraints.
A high-technical-complexity unit (custom retry logic, distributed transaction coordination) with low business-logic complexity may be a poor candidate for rewrite but a good candidate for rehosting. 
A low-technical-complexity unit (a thin REST adapter) with stable, well-understood logic may be a fine candidate for rewrite.
Documentation quality and team familiarity with both source and target shift the calculus further.

**Run multiple approaches in parallel where resources allow.** When the team includes engineers familiar with the target framework and engineers familiar with the source platform, both can work concurrently on different units. 
This serves two purposes: it matches each engineer's strengths to appropriate work, and it hedges project-level risk. 
If one approach fails for a particular unit, the other approach is available as a fallback that the team has already practiced on adjacent units.

The selection itself is a decision that benefits from structured analysis ([Nasdanika MCDA model](https://mcda.models.nasdanika.org/)) - alternatives, criteria, weights, and trade-offs explicitly surfaced rather than chosen by team comfort or visibility.

## Two-phase approach

This practice recommends a two-phase approach for most legacy modernization with hard deadlines:

**Phase 1 — Direct Semantic Execution (DSE)**, also known as *rehosting* or *lift-and-shift*. Load existing legacy artifacts AS-IS into a model. Build a runtime engine that executes the model directly. The legacy system's semantics are preserved exactly, with no behavior translation. This phase removes the dependency on the legacy vendor's runtime while preserving the legacy semantics. Risk is minimized because behavior preservation is mechanical rather than interpretive.

**Phase 2 — Model Transformation and Generation**, also known as *replatforming* or *forward engineering*. Once Phase 1 is delivered and the legacy runtime dependency is removed, transform the model gradually into more modern forms — typically through code generation to a target runtime. Phase 2 is incremental and per-deployment-unit, removing the need for a single large rewrite.

The phasing is deliberate. Phase 1 buys time and removes external dependencies. 
Phase 2 modernizes the runtime under conditions that are no longer time-constrained.

Phase 1 alone is a defensible end state for systems whose business logic is stable and whose target architecture is "off the legacy vendor's stack" rather than "on a specific modern platform." 
Many systems benefit from delivering Phase 1 and never executing Phase 2.

## Practice pages

- **[Analysis](analysis.html)** — Understanding the legacy system, its stakeholders, its artifacts, and its execution model. Establishes the foundation for choices in subsequent phases.
- **[Direct Semantic Execution](direct-semantic-execution.html)** — Loading and executing legacy artifacts AS-IS via a model-driven runtime engine.
- **[Model Transformation and Generation](transformation-and-generation.html)** — Generating modern runtime artifacts from the legacy model, gradually and per-deployment-unit.

## Underlying models and capabilities

The practice composes several Nasdanika models and capabilities:

- **[OpGraph](https://op-graph.models.nasdanika.org/)** — generalized workflow execution model. Provides nodes, calls, transitions, jobs, tokens, transactions. Applicable to most XML-driven workflow engines and many other execution-graph systems.
- **[Product Management](https://product-management.models.nasdanika.org/)** — personas, concerns, and capabilities for modernization-as-product.
- **[MCDA](https://mcda.models.nasdanika.org/)** — Multiple Criteria Decision Analysis for non-trivial choices.
- **[Enterprise architecture model](https://enterprise.models.nasdanika.org/)** — for documenting the legacy system in its surrounding context.
- **[C4 model](https://c4.models.nasdanika.org/)** — for architectural views (system, container, component, code).
- **[Capability framework](https://docs.nasdanika.org/core/capability/index.html)** — for pluggable processor and generator implementations.
- **[Maven URI Handler](https://docs.nasdanika.org/core/maven/index.html#uri-handler)** and the GitLab URI Handler — for federated model loading across repositories.
- **Documentation generator** — produces HTML documentation from Ecore models, with cross-reference resolution and search.
- **Model-based telemetry** — observability emitted with traces tagged by model element URI, supporting unified troubleshooting across models and runtime.

## Beyond Legacy Code: Intent Elicitation and "Agentization"

Because this methodology is fundamentally rooted in **intent elicitation** rather than mere syntax translation, its utility extends far beyond legacy software codebases.
The process of unbinding decisions, mapping their topology, and visualizing their flow applies equally to non-software artifacts, organizational transitions, 
and greenfield architectural design.

### Modeling Business Procedures

In many enterprises, the "legacy system" is not a compiled application, but a brittle, human-driven business procedure documented in massive PDFs, 
SharePoint wikis, or fragmented spreadsheets.
These artifacts suffer from the exact same traceability and intent-loss issues as undocumented code.

By applying this model-driven methodology to business procedures, we can extract the procedural logic (the inputs, conditions, and required outputs) 
and populate the same semantic model.

This transforms static, text-heavy operating procedures into visual, executable graphs.
It allows business analysts and enterprise architects to visually debug human workflows with the same rigor used for software integration pipelines.

GenAI can be used to elicit intent from unstructured documents into a structured model governed by a metamodel.
It can, as well, be used to elicit the initial metamodel.

### Business Acquisition as Enterprise Modernization

At a macro level, business acquisition is structurally identical to system modernization.
When an enterprise acquires a company, the objective is to "graft" the existing business into the parent organization.
This requires preserving the unique operational intent - the core value proposition and specialized capabilities that justified the acquisition in the first place
- while cleanly migrating shared, commodity functions such as HR and accounting.

This challenge applies to organizations of all scales. 
In mom-and-pop shops, the operational "legacy" is often completely unwritten, residing entirely within the owners' heads. 
In massive corporate mergers, that legacy intent is scattered across the minds of Subject Matter Experts (SMEs) and buried in disparate documents, 
frequently getting lost during the transition. 
By applying model-driven intent elicitation to an acquisition, architects can capture and map the organizational and procedural topology of the acquired business.
This ensures the critical, unique business logic is explicitly understood, modeled, and preserved rather than destroyed during integration.

### The Isomorphism of Integration Flows and Agentic AI

When we abstract software architecture to a topological level, a fascinating symmetry emerges between legacy
enterprise middleware and modern Generative AI frameworks.

Legacy Service Component Architecture (SCA) and integration flows (e.g., moving data through a pipeline of transformers and routers)
are structurally identical to modern "Agentic AI" flows.
Both are rooted in classic systems engineering paradigms, such as the PMBOK® process model (Inputs → Tools & Techniques → Outputs)
or the IDEF0 functional modeling standard (Inputs, Controls, Outputs, Mechanisms).

Consider the current industry excitement around "Agent Skills".
In modern AI frameworks, developers equip autonomous agents with specific capabilities—such as API access, Python interpreters,
or strictly scoped instructional scripts - to accomplish tasks.
Architecturally, this is indistinguishable from the "Tools and Techniques" phase of the PMBOK model.

Whether a transformation node is executed by a legacy XML routing script, a deterministic Java function,
or an LLM-powered Agent utilizing a semantic "skill," the underlying topology is exactly the same: an actor applies a specific tool
to an input to produce a defined output based on defined constraints.

### Greenfield Development and "Agentization"

Because an Agentic flow is essentially a modernized integration flow, this model-driven approach serves as the perfect
scaffolding for greenfield application development and the **agentization** of existing processes.

Jumping straight into writing Python scripts for AI agents is just as dangerous as code-first legacy modernization; it creates brittle, unmanageable AI spaghetti code.
Instead, architects can use the visual semantic model to design the greenfield process first:

1. **Design the Topology:** Use visual, auto-laid out graphs to define the inputs, activities, and outputs of the desired business process.
2. **Assign the Actors:** Determine which nodes in the graph require deterministic execution (standard code) and which nodes require non-deterministic reasoning (Generative AI agents).
3. **Generate the Scaffolding:** Use the metamodel to generate the rigid execution graph, providing the AI agents with the exact semantic context and boundaries they need to operate safely.

By treating AI agents simply as new types of actors operating within a classic, well-defined integration topology, 
enterprises can rapidly modernize and "agentize" their operations without sacrificing architectural governance or traceability.

## The Universal Paradigm: The Intent Elicitation Engine

While this practice specifically addresses legacy modernization and greenfield agentization, these are two specific applications of a broader paradigm.

In this paradigm there is a core consisting of cross-referencing metamodels (DSLs) and models which live in cross-referencing source repositories and are published to
cross-referencing web sites and binary repositories in the same as Java/Maven modules and cross-referencing Javadocs.

Then there are components to populate the model and to use the model information:

* A component which populates the model from legacy artifacts
* A component which uses GenAI to produce a structured model from unstructured text. 
* A component generating cross-referenced model documentation with visualizations, integrated full-text search and AI contextual search/chat
* A code/solution generator - used the model to generate code, infrastructure, ...
* Fine-grained CLI and web tools for specific tasks

### The Audit & Compliance Use Case

Consider the domain of SOX (Sarbanes-Oxley) compliance. Auditors must routinely parse massive, text-heavy "Process Narratives" (e.g., Procure-to-Pay workflows)
to identify implicit branching logic, control gates, and Segregation of Duties (SoD) violations. 
Currently, this requires humans to read static PDFs and manually sketch decision trees. 
The GenAI Adapter can ingest these complex governance documents and systematically elicit the underlying decision chart, projecting a dense 40-page text narrative 
into an auto-laid out, deterministic graph. Then a documentation site can be generated from this graph.
It transforms static compliance text into a visually auditable formal model.

