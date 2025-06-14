This module provides building blocks for AI solutions on top of EMF Ecore models.

* [Sources](https://github.com/Nasdanika/ai/tree/main/emf)
* [Javadoc](https://javadoc.io/doc/org.nasdanika.ai/emf)


[TOC levels=6]

## Similarity

``org.nasdanika.ai.emf.similarity`` package provides a number of interfaces and classes for computing similarity between EObjects as 
[connections](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/Connection.html) between [EObjectNodes](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/emf/EObjectNode.html).

### Similarities

Similarity can be of any type.
The similarity package provides concrete classes for the below similarity types:

* ``java.lang.Double``
* ``java.lang.Float``
* ``EStructuralFeatureSimilarity`` - similarity which has an aggregated similarity value and similarity values for individual features.
    * ``DoubleEStructuralFeatureSimilarity`` - binding to ``Double``.
    * ``FloatEStructuralFeatureSimilarity`` - binding to ``Float``.
    
### Connections

* ``EStructuralFeatureConnection`` - a connection with a value and a structural feature (attribute or reference) indicating similarity of a feature of the target to the source 
    * ``DoubleEStructuralFeatureConnection`` - binding to ``Double``.    
    * ``FloatEStructuralFeatureConnection`` - binding to ``Float``.    
* ``SimilarityConnection`` - a connection which indicates how much its target is similar to the source. Similarity does not have to be symmetrical. 
    * ``DoubleSimilarityConnection`` - binding of ``SimilarityConnection`` to ``Double``.
    * ``EStructuralFeatureVectorSimilarityConnection`` - binding to ``EStructuralFeatureSimilarity``
        * ``DoubleEStructuralFeatureVectorSimilarityConnection`` - binding to ``Double``.
        * ``FloatEStructuralFeatureVectorSimilarityConnection`` - binding to ``Float``.        
    * ``FloatSimilarityConnection`` - binding of ``SimilarityConnection`` to ``Float``.

### Connection factories

* ``SimilarityConnectionFactory`` - abstract base class for creating similarity connections.
    * ``EStructuralFeatureVectorSimilarityConnectionFactory`` - abstract base class for ``EStructuralFeatureVectorSimilarityConnection`` factories with ``EStructuralFeatureSimilarity`` value. Computes value from the outgoing ``EStructuralFeatureConnection``s.
        * ``DoubleEStructuralFeatureVectorSimilarityConnectionFactory`` - binding to ``Double``. Computes value as a weighted sum of feature values. The default feature weight is ``1.0``, override ``getFeatureWeight()`` to customize.
        * ``FloatEStructuralFeatureVectorSimilarityConnectionFactory`` - binding to ``Float``. Computes value as a weighted sum of feature values. The default feature weight is ``1.0``, override ``getFeatureWeight()`` to customize.
    * ``DoubleEStructuralFeatureSimilarityConnectionFactory`` - creates ``DoubleSimilarityConnection`` by computing its value from outgoing ``DoubleEStructuralFeatureConnection``s using a weighted sum. The default feature weight is ``1.0``, override ``getFeatureWeight()`` to customize.
    * ``FloatEStructuralFeatureSimilarityConnectionFactory`` - creates ``FloatSimilarityConnection`` by computing its value from outgoing ``FloatEStructuralFeatureConnection``s using a weighted sum. The default feature weight is ``1.0``, override ``getFeatureWeight()`` to customize.
    * ``MessageCollectorSimilarityConnectionFactory`` - abstract base class for connections factories which collect messages to compute similarity (see graph similarity below)
        * ``DoubleMessageCollectorSimilarityConnectionFactory`` - binding to ``DoubleSimilarityConnection``.
        * ``EStructuralFeatureVectorMessageCollectorSimilarityConnectionFactory`` - computes ``EStructuralFeatureSimilarity`` from message ``EReferenceConnection``s in the message path.
            * DoubleEStructuralFeatureVectorMessageCollectorSimilarityConnectionFactory - binding to ``Double``

### Graph similarity

``EObjectGraphMessageProcessor`` and its subclass ``DoubleEObjectGraphMessageProcessor`` can be used to compute similarity
between model objects by constructing a [graph](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/emf/package-summary.html) on top of model objects and their relationships and then sending messages between graph nodes.
Similarity may be computed from message values and message paths.  

The below code snippet shows how to use the above classes:

```java
DoubleEObjectGraphMessageProcessor<Void> messageProcessor = new DoubleEObjectGraphMessageProcessor<>(false, familyResource.getContents(), progressMonitor) {
    
    /**
     * Override this method to filter messages:
     * - Drop long messages
     * - Pass messages only through certain types of connections
     *   or from/to certain types of nodes
     */
    @Override
    protected boolean test(Message<Double> message, ProgressMonitor tpm) {
        if (message.depth() > 20 || message.value() < 0.000001) {
            return false;
        }
        Element recipient = message.recipient();                
        if (recipient instanceof EObjectNode) {
            EObject eObject = ((EObjectNode) recipient).get();
            return eObject instanceof Person || eObject instanceof EClass;
        }
        return true; 
    }
    
    /*
     *  Customize connection weights and message values,
     *  return null for connections which shall not be traversed
     */  
    
    @Override
    protected Double getOutgoingConnectionWeight(Connection connection) {
        return connection instanceof EClassConnection ? 1.0 : null;
    }
    
    @Override
    protected Double getIncomingConnectionWeight(Connection connection) {
        return connection instanceof EClassConnection ? 1.0 : null;
    }
    
    @Override
    protected Double getIncomingEReferenceWeight(EReference eReference) {
        return eReference == EcorePackage.Literals.ECLASS__ESUPER_TYPES ? 1.0 : null;
    }
    
    @Override
    protected Double getOutgoingEReferenceWeight(EReference eReference) {
        return eReference == EcorePackage.Literals.ECLASS__ESUPER_TYPES ? 1.0 : null;
    }
    
    @Override
    protected Double getConnectionMessageValue(
            BiFunction<Connection, Boolean, Double> state,
            Connection activator, 
            boolean incomingActivator, 
            Node sender, 
            Connection recipient,
            boolean incomingRrecipient, 
            Message<Double> parent, 
            ProgressMonitor progressMonitor) {
        
        Double connectionMessageValue = super.getConnectionMessageValue(
                state, 
                activator, 
                incomingActivator, 
                sender, 
                recipient, 
                incomingRrecipient,
                parent, 
                progressMonitor);
        
        if (connectionMessageValue != null) {
            return 0.8 * connectionMessageValue;
        }
        
        return connectionMessageValue;
    }
    
};

DoubleEStructuralFeatureVectorMessageCollectorSimilarityConnectionFactory similarityConnectionFactory = 
    new DoubleEStructuralFeatureVectorMessageCollectorSimilarityConnectionFactory();

// Optional selector to send messages only from some graph nodes
Function<Map<Element, ProcessorInfo<BiFunction<Message<Double>, ProgressMonitor, Void>>>, Stream<BiFunction<Message<Double>, ProgressMonitor, Void>>> selector = processors -> {
    ...
};

// Optional message filter - can be used instead of overriding the test() method
// or in combination with the test() method.
BiFunction<Message<Double>, ProgressMonitor, Message<Double>> messageFilter = (m,p) -> {
    ...
};

messageProcessor.processes(
        1.0, 
        selector, 
        messageTransformer,
        similarityConnectionFactory, 
        progressMonitor);

Collection<DoubleEStructuralFeatureVectorSimilarityConnection> similarityConnections = similarityConnectionFactory.createSimilarityConnections();
```

[TestFamilySimilarity](https://github.com/Nasdanika-Demos/family-semantic-mapping/blob/main/src/test/java/org/nasdanika/models/family/demos/mapping/tests/TestFamilySimilarity.java) provides several examples of computing similarity of family members:

* Relatives - messages are sent through the "parents" reference
* Gender - messages are sent through the [EClassConnection](https://javadoc.io/doc/org.nasdanika.core/graph/latest/org.nasdanika.graph/org/nasdanika/graph/emf/EClassConnection.html) and then through the supertypes reference

#### Performance

On a Windows 11 desktop with Intel i7-14700 2.1 GHz CPU:

* Single thread - 500K messages/second
* Cached thread pool (5-20) - 600k message/second

### Applications

#### Graph RAG & fine tuning

* Find objects similar to a given (context) object 
* Generate descriptions for the context objects and similar objects
* Use generated descriptions in a chat prompt or to fine-tune a model. In the latter case similar objects can be used to generate questions/answers.

In case of the family model it might be important that [Paul](https://nasdanika-demos.github.io/family-semantic-mapping/references/members/paul/index.html) is a 
[Man](https://family.models.nasdanika.org/references/eClassifiers/Man/index.html) and a [father](https://family.models.nasdanika.org/references/eClassifiers/Person/references/eStructuralFeatures/father/index.html) of [Lea](https://nasdanika-demos.github.io/family-semantic-mapping/references/members/lea/index.html) who is a [Woman](https://family.models.nasdanika.org/references/eClassifiers/Woman/index.html).

In the case of an [architecture](https://architecture.models.nasdanika.org/index.html) model, [Internet Banking System](https://nasdanika-demos.github.io/internet-banking-system-c4/cerulean/index.html) for example, it might be important that [API Application](https://nasdanika-demos.github.io/internet-banking-system-c4/cerulean/references/elements/internet-banking-system/references/elements/api-application/index.html) is a [Container](https://architecture.models.nasdanika.org/references/eSubpackages/c4/references/eClassifiers/Container/index.html).
It might be especially important for proprietary architecture models. 
Let's say that API Application is a container image to be deployed to Kubernetes following organization's guidelines.

It may also be important that the [Internet Banking System](https://nasdanika-demos.github.io/internet-banking-system-c4/cerulean/references/elements/internet-banking-system/index.html) belongs to a specific line of business or product portfolio.

#### Decision analysis

In Multi-criteria Decision Analysis message sending can be used to compute alternatives' weights by sending a message from the goal.
Messages would pass through the criteria (flat, hierarchy, network) and be collected by the alternative nodes. 
Message paths can be used to explain reasoning and to detect inconsistencies.

See also [Task and design spaces with Visual Collaborative Multi-Criteria Decision Analysis](https://medium.com/nasdanika/task-and-design-spaces-visual-collaborative-multi-criteria-decision-analysis-2017c823b496)

#### Product recommender

In [banking](https://bank.models.nasdanika.org/) and other businesses message passing can be used to compute similarity between
[customers](https://bank.models.nasdanika.org/references/eClassifiers/Customer/index.html) and [products](https://bank.models.nasdanika.org/references/eClassifiers/Product/index.html).

Customer to customer similarity can be computed using customer location (country/state/city), income, net worth, demographics.

If products are organized into product groups/categories, product-to-products similarity can be computed by traversing the category hierarchy.
A checking account product might be more similar to a saving account product (both deposits) than to a credit card. 
Or vice versa - both checking and credit card accounts are used for payments.

Then customer-product similarity can be computed by sending messages from a customer node to similar customers and then from
those customer nodes to the products they use.

#### Nature model

This scenario is to demonstrate different types of similarity.

In the [Nature](https://nature.models.nasdanika.org/diagram.html) model as shown [here](https://nasdanika-demos.github.io/semantic-mapping/operations/index.html)
we may compute:

* Food similarity. For example, Fox it more similar to Hare than to Grass because Fox eats Hare, but doesn't eat Grass. Computing this similarity would require the following traversal:
    * Object -> class (type)
    * Class -> operation (method)
    * Operation -> parameters
    * Parameter -> parameter type
    * Parameter type -> object (instance)
* Reproduction - a female fox would have greater similarity with a male fox than to a female fox or other living beings. Computing this similarity would require traversal from an object to its class and then to other instances of the class.

Using the above similarities we can solve a "life optimization problem": given a distribution of nutrients on 2D surface compute distribution of living beings - grass, hares and foxes. 
Living beings shall be close to their food so they don't starve to death. 
They should be far enough from each other so they don't exhaust their food supply.
Living beings of the same species shall be close enough to beings of the opposite sex to reproduce.

From the above similarities distances/forces can be computed to use in a force layout graph to compute distributions.
