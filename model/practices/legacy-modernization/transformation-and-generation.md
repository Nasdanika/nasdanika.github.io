
Model Transformation and Generation is Phase 2 of the recommended two-phase modernization approach. 
After [Direct Semantic Execution](direct-semantic-execution.html) has removed the dependency on the legacy vendor's runtime (Phase 1), transformation and generation gradually replace legacy element types with modern runtime artifacts, per-deployment-unit.

This phase is also known industrially as *replatforming* (in the cloud-migration 6Rs vocabulary) or *forward engineering* (in the broader software engineering literature).

## When to enter Phase 2

Phase 2 is appropriate when:

- Phase 1 has been delivered and the legacy vendor dependency is removed.
- The system needs to evolve in ways the legacy element model cannot easily support.
- Specific deployment units are candidates for re-platforming based on independent business need.
- Resources and time exist to do the work without re-introducing the time pressure that drove Phase 1.

Phase 2 is not appropriate when:

- The legacy semantics are stable and adequate, and the cost of transformation exceeds the cost of continuing to maintain the DSE-rehosted system.
- The deployment unit is scheduled for retirement.

Phase 2 is incremental. Some deployment units may go through it; others may stay rehosted indefinitely.
The decision is per-unit, not per-system, and each unit's transformation is independently rollable.

## Transformation approach

The model loaded during DSE is the input. The transformation produces a new model in a target metamodel — for example Ecore-based — which is then used to generate code, configuration, or other runtime artifacts.

The OpGraph-to-Ecore transformation is the canonical example. 
OpGraph operations transform to Ecore operations with explicit parameter types; complex operation outputs become Ecore classes. 
The result is a strongly-typed model with documented operation signatures, inputs, and outputs.


## GenAI in the generation loop

GenAI fits cleanly into Phase 2 when the context provided to it is structured. 
Random GenAI applied to legacy artifacts produces unreliable output; GenAI applied within a structured target metamodel, with documented operation signatures and validation, is more reliable.

The pattern:

1. The target Ecore operation has a documented signature — input types, output type, intent in natural language.
2. The legacy operation's behavior is documented in the source model (extracted via DSE).
3. GenAI is asked to produce an implementation of the target operation that preserves the source behavior, given both the target signature and the source documentation.
4. The output is validated through:
   - Syntactic parsing using the [Nasdanika Java model](https://java.models.nasdanika.org/) or the underlying [JavaParser](https://github.com/javaparser/javaparser).
   - Code generation using Ecore modules and then standard Java compiler API.
5. If validation fails, the prompt is updated with the diagnostic and resubmitted, up to a configurable retry limit.

This is structurally similar to the Elicitor pattern: GenAI inside a closed-loop structure where its output is validated against a formal specification, with retries on failure.
The validation harness is the load-bearing piece.
Without validation, GenAI-generated code is unreliable.
With validation that includes syntactic checking, the generated code is of higher quality.

This is also the right way to leverage AI in modernization — well-framed tasks with clearly defined interfaces, not unstructured exposure to large bodies of legacy code. 
Asking an AI to "modernize this legacy project" produces nothing useful; asking it to "implement this Ecore operation with this signature, preserving the behavior described in this source model" produces high-quality results when the validation harness is sound.

## Per-deployment-unit migration

Phase 2 is not a single transformation event; it is a series of per-deployment-unit transformations. 
Each deployment unit is migrated independently. 
The DSE engine continues to execute deployment units that haven't been migrated; the new runtime executes those that have.

Per-unit migration has several advantages:

- **Risk is bounded.** A failed migration affects one deployment unit, not the whole system.
- **Rollback is possible.** The DSE engine still has the artifacts; reverting a unit means switching the deployment unit back to DSE.
- **Resources scale to the work.** Small deployment units can be migrated by one engineer in a sprint; larger ones get larger teams.
- **Business prioritization applies.** Units whose evolution is most important get migrated first; stable units stay rehosted.

The federated model substrate supports per-unit migration naturally: each migrated unit publishes its target artifacts as a Maven artifact; the runtime resolves units transitively, regardless of whether they are DSE-rehosted or fully migrated.

## Validation and equivalence testing

The most important Phase 2 capability is *equivalence testing* — verifying that the transformed model behaves the same as the source DSE execution for representative inputs.

Approaches:

- **Replay** — capture production traffic during DSE operation; replay through the new runtime; compare outputs.
- **Property-based testing** — generate input cases automatically, run both implementations, assert equivalence.
- **Differential testing** — run both implementations side-by-side in production; compare outputs in real time; flag differences.

Equivalence testing scales with the size of the test corpus.
The corpus built during analysis and refined during DSE pays compounding dividends in Phase 2 — units migrate against an existing regression baseline rather than requiring per-unit test authoring.

## Outcome

A successfully-completed Phase 2 produces:

- Modern runtime artifacts replacing the legacy element model, per-deployment-unit, on the target architecture.
- Federated documentation reflecting the post-transformation state.
- Equivalence test corpora for regression coverage.
- A deprecation path for the DSE engine, gradually consumed as deployment units migrate.

The DSE engine is not retired immediately at Phase 2 start; it continues to support unmigrated units. 
It is retired when the last unit is migrated, or kept indefinitely as a maintenance vehicle for any unit business chooses not to migrate.
