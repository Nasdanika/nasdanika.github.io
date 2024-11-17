This is old documentation - to be revised

[TOC levels=6]

Feature mapping value can be either a string or a map. If it is a string it is treated as a singleton map to ``true`` (unconditional mapping).

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

## argument-type

Specifies type of feature elements to be set/added. 
String or a list of strings. Each string is a type name as defined in [``type``](#type-1) optionally prefixed with ``!`` for negation.
In the case of a list of strings the result is a logical OR - if any of elements matches.
Only instances of matching types will be set/added.

If absent, the feature type is used.
Argument type can be used to restrict elements to a specific subtype of the feature type.

## comparator

Comparator is used for "many" features to order elements. 
A comparator instance is created by ``createComparator()`` method which can be overridden in subclasses to provide support for additional comparators.

The following comparators are provided "out of the box":

### clockwise

Compares elements by their angle relative to the node of the semantic element which holds the many reference. 
In the [Living Beings](https://graph.models.nasdanika.org/demo/living-beings/index.html) demo "Bird", "Fish", and "Bacteria" are compared by their angle to the "Living Beings" with the angle counted from "12 o'clock" - 90 degrees (default).

Feature mapping with comparators of "Bird", "Fish", and "Bacteria" are defined at the connections from "Living Beings" as:

```yaml
source: 
  elements:
    comparator: clockwise
```

To specify the base angle other than 90 degree use the map version of comparator definition where ``clockwise`` is the key mapping to a number or string value. 
The number value is used as the angle value in degrees. The string value is treated as a [Spring Expression Language (SpEL)](https://docs.spring.io/spring-framework/reference/core/expressions.html) expression evaluated in the context of the "parent" node. The expression may evaluate to a number or to a node. In the latter case the result is used to compute the angle between the context node and the result node.

In the Living Beings example "Streptococcus", ..., "Staphyllococcus" are compared relative to the "Bacteria" node with the base angle being the angle between the "Bacteria" node and "Living Beings" node. As such "Streptococcus" is the smallest node and "Staphyllococcus" is the largest. With the default angle of 90 degrees "Lactobacyllus" would be the smallest and "Streptococcus" would be the largest.

Feature mapping with comparators of "Streptococcus", ..., "Staphyllococcus" is defined at connections from "Bacteria" to the respective genus nodes as:

```yaml
source: 
  elements:
    comparator: 
      clockwise: incoming[0].source
```

``incoming[0]`` evaluates to the connection from "Living Beings" to "Bacteria" and ``source`` evaluates to "Living Beings".

### counterclockwise

Reverse of ``clockwise``.

### down-left

Compares nodes by their vertical order first with higher nodes being smaller and then by horizontal order with nodes on the right being smaller.
Nodes are considered vertically equal if they vertically overlap.  

### down-right

Compares nodes by their vertical order first with higher nodes being smaller and then by horizontal order with nodes on the left being smaller.
Nodes are considered vertically equal if they vertically overlap. 
This comparator can be used for org. charts.

### expression

A [Spring Expression Language (SpEL)](https://docs.spring.io/spring-framework/reference/core/expressions.html) expression evaluated in the context of the feature element with ``other`` variable referencing the element to compare with. 
The expression has access to ``registry`` variable containing a map of diagram elements to semantic elements.

### flow

If one element is reacheable from the other by traversing connections, then the reacheable element is larger than the source element.
In case of circular references the element with the smaller number of traversals to the other element is considered smaller. 
If elements are not connected they are compared by the fall-back comparator.
This comparator can be used for workflows and [PERT](https://en.wikipedia.org/wiki/Program_evaluation_and_review_technique) charts.

If this comparator's value is a String, then it is used as a name of the fallback comparator.
In the below example children will be smaller than their parents and siblings will be compared using labels.

```yaml
container:
  self: 
    members:
      argument-type: Person
      comparator: 
        flow: label
```

If the value is a map, then it may have the following keys:

* ``condition`` - A boolean [Spring Expression Language (SpEL)](https://docs.spring.io/spring-framework/reference/core/expressions.html) expression evaluated in the context of a connection being traversed. It may be used to traverse only connections which match the condition. For example, only [transitions](https://flow.models.nasdanika.org/references/eClassifiers/Transition/index.html) between [activities](https://flow.models.nasdanika.org/references/eClassifiers/Activity/index.html) in a process model.
* ``fallback`` - Fallback comparator.

### key

A [Spring Expression Language (SpEL)](https://docs.spring.io/spring-framework/reference/core/expressions.html) expression evaluated in the context of the feature element. The expression must return a value which would be used for comparison using the natural comparator as explained below.

### label

Uses diagram element label converted to plain text as a sorting key.
In the [Family mapping demo](https://family.models.nasdanika.org/demos/mapping/index.html) family members are sorted by label using the following feature map definition:

```yaml
container:
  self: 
    members:
      argument-type: Person
      comparator: label
```

### label-descending

Uses diagram element label converted to plain text as a sorting key to compare in reverse alphabetical order.

### left-down

Compares nodes by their horizontal order first with nodes on the right being smaller and then by vertical order with higher nodes being smaller.
Nodes are considered horizontally equal if they horizontally overlap.  

### left-up

Compares nodes by their horizontal order first with nodes on the right being smaller and then by vertical order with lower nodes being smaller.
Nodes are considered horizontally equal if they horizontally overlap.  

### natural

Uses feature element's ``compareTo()`` method for comparable elements. Otherwise compares using hash code. Nulls are greater than non-nulls.

### property

Uses diagram element property as a sorting key. Singleton map. For example:

```
property: label
```

### property-descending

The same as property, but compares in reverse alphabetical order.

### reverse-flow

Same as ``flow`` but with target nodes being smaller than source nodes.

### right-down

Compares nodes by their horizontal order first with nodes on the left being smaller and then by vertical order with higher nodes being smaller.
Nodes are considered horizontally equal if they horizontally overlap.  

### right-up

Compares nodes by their horizontal order first with nodes on the left being smaller and then by vertical order with lower nodes being smaller.
Nodes are considered horizontally equal if they horizontally overlap.  

### up-left

Compares nodes by their vertical order first with lower nodes being smaller and then by horizontal order with nodes on the right being smaller.
Nodes are considered vertically equal if they vertically overlap.  

### up-right

Compares nodes by their vertical order first with lower nodes being smaller and then by horizontal order with nodes on the left being smaller.
Nodes are considered vertically equal if they vertically overlap.  

## condition

A [Spring Expression Language (SpEL)](https://docs.spring.io/spring-framework/reference/core/expressions.html) boolean expression evaluated in the context of the candidate diagram element with the following variables:

* ``value`` - semantic element of the candidate diagram element
*  ``path`` - containment path
* ``registry`` - a map of diagram element to semantic elements

## expression

A SpEL expression evaluating to a feature value in the context of the diagram element with with the following variables:

* ``value`` - semantic element of the diagram element
*  ``path`` - containment path
* ``registry`` - a map of diagram elements to semantic elements

## greedy

Greedy is used with containment features and specifies what to do if a candidate object is already contained by another object:

* ``no-children`` - grab the object if it is contained by an ancestor of this semantic element. This is the default behavior.
* ``false`` - do not grab
* ``true`` - always grab

## path

Either an integer number o or a list of boolean SpEL expressions to match the path. 
If an integer then it is used to match path length as shown in the example below which matches only immediate children

```yaml
container:
  self: 
    elements:
      path: 1
```

If a list, then it matches if the list size is equal to the path length and each element evaluates to true in the context of a given path element.
Expression have access to ``registry`` variable - a map of diagram elements to semantic elements.

## nop

If ``true``, no mapping is performed, but the chain mapper is not invoked. 
It can be used in scenarios with a default (chained) mapper to prevent the default behavior.

## position

A number specifying the position of the element in the feature collection.

## script

Script to evaluate the feature value. 
The script has access to the following variables:

* ``argument``
* ``argumentValue``
* ``baseURI``
* ``context``
* ``registry``
* ``sourcePath``
* ``type``

Additional variables can be introduced by overriding ``getVariables()``  method.
Script class loader can be customized by overriding ``getClassLoader()`` method.

## script-engine

A boolean SpEL expression for selecting a script engine. 
The expression is evaluated in the context of a candidate ``javax.script.ScriptEngineFactory``.

If there is no ``script-engine`` expression and a script is loaded from a resource specified in ``script-ref``,
then the URI file extension (characters after the last dot) is used to match a script engine by extension..

## script-ref

Script reference resolved relative to the base URI.

## type

Type of the feature object to match. String as defined in Mapping Reference > type.
Can be used in ``other`` mappings.

