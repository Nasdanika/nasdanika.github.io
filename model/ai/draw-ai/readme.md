![Nasdanika Draw.AI](/images/nasdanika-draw-ai-logo.png)

Nasdanika Draw.AI is a collection of practices, patterns, and tools intended to move the creation of agentic systems to the left - away from code-first development and toward visual, model-driven expression accessible to subject-matter experts and citizen developers.
Instead of requiring people to understand frameworks, APIs, or orchestration runtimes, Nasdanika Draw.AI aims to enable them to **draw what they want**, 
and the system interprets, executes, or generates agentic code for a specific runtime from those diagrams.

This approach builds on a core insight running through the Nasdanika ecosystem: diagrams, models, and metamodels are **executable knowledge structures**.
They capture intent, context, and semantics in a form that both humans and AI agents can understand and act upon. 


[TOC levels=6]

## The Semantic Membrane

Nasdanika Draw.AI isn’t a diagramming tool - there is a number of general-purpose diagramming tools available.  
It isn’t an AI framework or runtime, although it might be.  
It’s the semantic membrane between them — the thing that turns diagrams into executable systems.

![Nasdanika Draw.AI](/images/semantic-membrane.png)

> Not above the glass. 
>
> Not below the glass. 
>
> It is the glass.

## Why This Matters

Most organizations have deep expertise locked in the heads of SMEs — HR specialists, operations leads, architects, analysts, planners. 
These people understand the domain far better than developers, but they are rarely empowered to shape the systems that automate or augment their work.

Nasdanika Draw.AI changes that dynamic:

- SMEs and citizen developers express intent visually, without waiting for developers.  
- Developers/technology focus on wiring diagrams to tools and data sources, securing, scaling, refinement.

The result is a shift-left transformation where diagrams become models, models become agent contexts, and agents execute on top of clear semantics.

---

## Example: Professional Knowledge Graph (HR as Code)

An illustration of Nasdanika Draw.AI’s value is the **Professional Knowledge Graph** — a digital twin of a person’s professional experience.

In the [HR as Code](https://medium.com/nasdanika/model-based-hr-as-code-270f7052c6c5) framing, the challenge is clear: decades of nuanced experience, niche skills, and contextual knowledge cannot be compressed into a résumé without losing meaning.
The real source of truth is the CV in your head — rich, interconnected, and impossible to flatten.

With Nasdanika Draw.AI, a person can **draw** their professional universe:

- Roles, projects, and responsibilities  
- Skills, certifications, and tools  
- People they collaborated with  
- Systems they touched  
- Artifacts they produced  
- Outcomes they influenced  

Each node becomes part of a structured model. Each connection encodes meaning. And once the model exists, **agents can operate on top of it**:

- Generate tailored résumés for specific job posts  
- Analyze skill gaps  
- Recommend career paths  
- Match candidates to roles  
- Evaluate experience against job requirements  
- Produce interview preparation materials  
- Summarize past projects or generate portfolio narratives  

This is the essence of shifting left: the job seeker (or HR specialist) doesn’t need to understand JSON schemas, Ecore, or agentic runtimes. 
They simply draw their experience, and Nasdanika Draw.AI turns that into a living, executable knowledge graph that agents can reason over.

---

## The Bigger Picture

Nasdanika Draw.AI sits at the intersection of diagrams, semantic mapping, and agentic execution. 
It leverages the full Nasdanika stack — models, capabilities, processors, semantic mapping, and agentic runtimes — to make agentic systems **designable by anyone who can think visually**.

- Draw first, formalize later.  
- Let diagrams become models.  
- Let models become agent contexts.  
- Let agents operate on bounded contexts with clear semantics.  
- Let SMEs and citizen developers shape the system long before code enters the picture.

In other words: **Draw what you want to happen, and agents will do it.**

## Getting started

You can start by defining a metamodel and notation or notations which fit your needs using the resources below.
You can also start adopting the [Visual schema](https://nasdanika-demos.github.io/illustrations/visual-schema/index.html) to define data structures
and generating documentation from it. 
You may use tags to define agents' bounded contexts. 

For Web UI you may use [Functional wireframes](https://medium.com/nasdanika/web-ui-wireframes-in-draw-io-documentation-html-generation-4241c8d04d86).

## Resources

### Medium stories

* [The Pillars of Nasdanika](https://medium.com/nasdanika/pillars-of-nasdanika-d9c4acff1137)
* [Nasdanika 2025+](https://medium.com/nasdanika/nasdanika-2025-9b3be3cb6ef3)
* [Model-based HR... as code](https://medium.com/nasdanika/model-based-hr-as-code-270f7052c6c5)

### CrewAI metamodel and demos

* [Metamodel](https://crew-ai.models.nasdanika.org/)
* Demos
    * [Agents as nodes](https://nasdanika-demos.github.io/latest-ai-development/)
    * [Agents as process swimlanes](https://nasdanika-demos.github.io/latest-ai-development-swimlanes/) - more compact, aligns to process modeling - not AI-specific.