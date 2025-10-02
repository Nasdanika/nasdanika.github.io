
* [Sources](https://github.com/Nasdanika/ai/tree/main/smile)
* [Maven Central](https://central.sonatype.com/artifact/org.nasdanika.ai/smile)
* [Javadoc](https://javadoc.io/doc/org.nasdanika.ai/smile)


This module contains wrappers of some [Smile](https://haifengl.github.io/) [regression](https://haifengl.github.io/regression.html) classes as ``FittedPredictor.Fitter<double[], double[], Double>``.

## Examples

### Random forest

```java
double[][] data = createData();
List<Object> dataList = Arrays.asList(data);
RandomForestPredictorFitter rfpf = new RandomForestPredictorFitter();
FittedPredictor<double[], double[], Double> predictor = rfpf.fit(
        dataList, 
        e -> { 
            double[] s = (double[]) e;
            double[] f = new double[s.length -1];
            System.arraycopy(s, 0, f, 0, f.length);
            return f;
        },
        e -> new double[] { ((double[]) e)[((double[]) e).length - 1] });       

System.out.println(predictor.getError());
double[] prediction = predictor.predict(new double[] { 6, 7, 8 });
System.out.println(prediction[0]);
```

### OLS

```java
double[][] data = createData();
List<Object> dataList = Arrays.asList(data);
OLSPredictorFitter olspf = new OLSPredictorFitter();
FittedPredictor<double[], double[], Double> predictor = olspf.fit(
        dataList, 
        e -> { 
            double[] s = (double[]) e;
            double[] f = new double[s.length -1];
            System.arraycopy(s, 0, f, 0, f.length);
            return f;
        },
        e -> new double[] { ((double[]) e)[((double[]) e).length - 1] });       

System.out.println(predictor.getError());

double[] prediction = predictor.predict(new double[] { 6, 7, 8 });
System.out.println(prediction[0]);
```

#### Composition

```java
double[][] data = createData();
List<Object> dataList = Arrays.asList(data);
OLSPredictorFitter olspf = new OLSPredictorFitter();

Fitter<double[], double[], Double> other = new AbstractDoubleFitter() {
    
    @Override
    protected Function<double[][], double[][]> fit(double[][] features, double[][] labels) {
        return input -> {
            double[][] result = new double[labels.length][];
            for (int i = 0; i < result.length; ++i) {
                result[i] = new double[labels[i].length];
                result[i][0] = 0.22;
            }
            return result;
        };
    }
    
};

FittedPredictor.Fitter<double[], double[], Double> composite = olspf.compose(other);

FittedPredictor<double[], double[], Double> predictor = composite.fit(
        dataList, 
        e -> { 
            double[] s = (double[]) e;
            double[] f = new double[s.length -1];
            System.arraycopy(s, 0, f, 0, f.length);
            return f;
        },
        e -> new double[] { ((double[]) e)[((double[]) e).length - 1] });       

System.out.println(predictor.getError());

double[] prediction = predictor.predict(new double[] { 6, 7, 8 });
System.out.println(prediction[0]);
```

The ``other`` fitter above always predicts ``0.22`` - just for testing.

#### Recursive (Autoregression)

```java
OLSRecursivePredictorFitter fitter = new OLSRecursivePredictorFitter();

double[][] data = {
    { 1, 2, 3, 4.1, 5, 6, 7 },
    { 2, 3, 4, 4.9, 6, 7, 8 },
    { 3, 4, 5, 6.1, 7, 8, 9 },
    { 4, 5, 6, 6.9, 8, 9, 10 },
    { 5, 6, 7, 8.1, 9, 10, 11 },            
    { 6, 7, 8, 9.1, 10, 11, 12 },           
    { 7, 8, 9.1, 10, 11, 12, 13 },          
    { 8, 9.1, 10, 11, 12, 13, 14 }          
};              

int labels = 3;

List<Object> dataList = org.assertj.core.util.Arrays.asList(data);
FittedPredictor<double[], double[], Double> predictor = fitter.fit(
        dataList, 
        e -> { 
            double[] s = (double[]) e;
            double[] f = new double[s.length - labels];
            System.arraycopy(s, 0, f, 0, f.length);
            return f;
        },
        e -> { 
            double[] s = (double[]) e;
            double[] l = new double[labels];
            System.arraycopy(s, s.length - labels, l, 0, l.length);
            return l;
        });     

System.out.println(predictor.getError());

double[] prediction = predictor.predict(new double[] { 6, 7, 8, 9 });
System.out.println(prediction[0]);      
```

#### Adapting features and labels

```java
Map<String,Double> ageMap = Map.of(
    "Alice", 25.0,
    "Bob", 30.0,
    "Eve", 35.0,
    "Mallory", 40.0
);

Map<String,Double> weightMap = Map.of(
        "Alice", 100.0,
        "Bob", 120.0,
        "Eve", 140.0,
        "Mallory", 0.0
    );
        
OLSPredictorFitter olspf = new OLSPredictorFitter();
Fitter<String, double[], Double> featureAdapted = olspf.adaptFeature(name -> new double[] { ageMap.get(name) });
Fitter<String, Double, Double> labelAdapted = featureAdapted.adaptLabel(
        weight -> new double[] { weight },
        wa -> wa[0]
);

FittedPredictor<String, Double, Double> predictor = labelAdapted.fit(
        List.of("Alice", "Bob", "Eve"),
        Function.identity(),
        weightMap::get);        

System.out.println(predictor.getError());

double prediction = predictor.predict("Mallory");
System.out.println(prediction);
```

### Regression Tree

```java
double[][] data = createData();
List<Object> dataList = Arrays.asList(data);
RegressionTreePredictorFitter rtpf = new RegressionTreePredictorFitter();
FittedPredictor<double[], double[], Double> predictor = rtpf.fit(
        dataList, 
        e -> { 
            double[] s = (double[]) e;
            double[] f = new double[s.length -1];
            System.arraycopy(s, 0, f, 0, f.length);
            return f;
        },
        e -> new double[] { ((double[]) e)[((double[]) e).length - 1] });       

double[] prediction = predictor.predict(new double[] { 6, 7, 8 });
System.out.println(prediction[0]);
```

### MLP

```java
double[][] data = createData();
List<Object> dataList = Arrays.asList(data);
MLPPredictorFitter mlppf = new MLPPredictorFitter();
FittedPredictor<double[], double[], Double> predictor = mlppf.fit(
        dataList, 
        e -> { 
            double[] s = (double[]) e;
            double[] f = new double[s.length -1];
            System.arraycopy(s, 0, f, 0, f.length);
            return f;
        },
        e -> new double[] { ((double[]) e)[((double[]) e).length - 1] });       

System.out.println(predictor.getError());
double[] prediction = predictor.predict(new double[] { 6, 7, 8 });
System.out.println(prediction[0]);
```

The above example uses regression MLPs with a single output. 
As such there is an MLP per label element.
An implementation using a single MLP with multiple outputs will be provided in the future. 

