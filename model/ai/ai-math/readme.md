
* [Sources](https://github.com/Nasdanika/ai/tree/main/math)
* [Maven Central](https://central.sonatype.com/artifact/org.nasdanika.ai/math)
* [Javadoc](https://javadoc.io/doc/org.nasdanika.ai/math)

This module contains wrappers of some [Apache Commons Math](https://commons.apache.org/proper/commons-math/) [regression](https://haifengl.github.io/regression.html) classes as ``FittedPredictor.Fitter<double[], double[], Double>``.

## Example

```java
PolynomialPredictorFitter ppf = new PolynomialPredictorFitter(1);

WeightedObservedPoints wobs = new WeightedObservedPoints();
wobs.add(1, 2);
wobs.add(2, 2.9);
wobs.add(3, 4.1);
wobs.add(4, 5);
wobs.add(5, 6);
        
FittedPredictor<double[], double[], Double> predictor = ppf.fit(
        wobs.toList(), 
        p -> new double[] { p.getX() }, 
        p -> new double[] { p.getY(), p.getY() + 10 });

System.out.println(predictor.getError());

double[] prediction = predictor.predict(new double[] { 6.0 });              
System.out.println(Arrays.toString(prediction));
```


