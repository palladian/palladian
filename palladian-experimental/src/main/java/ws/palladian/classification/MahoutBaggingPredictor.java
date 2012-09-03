package ws.palladian.classification;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.mahout.classifier.df.Bagging;
import org.apache.mahout.classifier.df.DecisionForest;
import org.apache.mahout.classifier.df.builder.DecisionTreeBuilder;
import org.apache.mahout.classifier.df.builder.TreeBuilder;
import org.apache.mahout.classifier.df.data.Data;
import org.apache.mahout.classifier.df.data.DataConverter;
import org.apache.mahout.classifier.df.data.DataLoader;
import org.apache.mahout.classifier.df.data.Dataset;
import org.apache.mahout.classifier.df.data.DescriptorException;
import org.apache.mahout.classifier.df.node.Node;

import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * @author Philipp Katz
 */
public final class MahoutBaggingPredictor implements Predictor<String> {

    private Node node;
    private DataConverter converter;
//    private String[] labels;
    private DecisionForest forest;
    private Dataset dataset;

    @Override
    public void learn(List<Instance2<String>> instances) {
        
        TreeBuilder treeBuilder = new DecisionTreeBuilder();
        
        String attributes = getAttributeDescription(instances);
        
        // System.out.println(attributes);
        
        String[] inputData = getData(instances);
        try {
//            dataset = DataLoader.generateDataset(attributes, false, inputData);
            dataset = DataLoader.generateDataset(attributes, true, inputData);
        } catch (DescriptorException e) {
            throw new IllegalStateException(e);
        }
        // System.out.println(Arrays.toString(dataset.labels()));
        
        converter = new DataConverter(dataset);

        
        List<org.apache.mahout.classifier.df.data.Instance> theInstances = new ArrayList<org.apache.mahout.classifier.df.data.Instance>();
        for (Instance2<String> instance : instances) {
            String xyz = getData(instance.featureVector);
            double regValue = instance.target.equals("true") ? 1. : 0.;
            xyz = xyz.concat(",").concat(String.valueOf(regValue));
//            xyz = xyz.concat(",").concat(instance.target);
            org.apache.mahout.classifier.df.data.Instance inst = converter.convert(xyz);
            theInstances.add(inst);
            
        }
        
        Data data = new Data(dataset,theInstances);
        
        List<Node> trees = new ArrayList<Node>();
        
        for (int i =0; i < 10; i++) {
            Bagging bagging = new Bagging(treeBuilder, data);
            node = bagging.build(RandomUtils.JVM_RANDOM);
            trees.add(node);
            System.out.println("bagging " + i);
            
            
        }
        
        DecisionForest forest = new DecisionForest(trees);
        this.forest = forest;
        
//        labels = dataset.labels();
    }

    

private String[] getData(List<Instance2<String>> instances) {
    String[] data = new String[instances.size()];
    for (int i = 0; i < instances.size(); i++) {
        Instance2<String> instance = instances.get(i);
        String features = getData(instance.featureVector);
        features = features.concat(",").concat(instance.target);
        // System.out.println(features);
        data[i] = features;
    }
    return data;
    }



private String getData(FeatureVector featureVector) {
    StringBuilder line = new StringBuilder();
    boolean first = true;
    for (Feature<?> feature : featureVector) {
        if (first) {
            first = false;
        } else {
            line.append(",");
        }
            line.append(feature.getValue());
        // TODO missing value is "?"
    }
    return line.toString();
}



// https://cwiki.apache.org/MAHOUT/partial-implementation.html
    private String getAttributeDescription(List<Instance2<String>> instances) {
        FeatureVector featureVector = instances.get(0).featureVector;
        Feature<?>[] featureVectorArray = featureVector.toArray();
        // Attribute[] attributes = new Attribute[featureVector.size() + 1];
        StringBuilder attributeBuilder = new StringBuilder();
        for (int i = 0; i <  featureVectorArray.length; i++) {
            Feature<?> feature = featureVectorArray[i];
            if (feature instanceof NominalFeature) {
                // attributes[i] = Attribute.CATEGORICAL;
                attributeBuilder.append("C");
            } else if (feature instanceof NumericFeature) {
                // attributes[i] = Attribute.NUMERICAL;
                attributeBuilder.append("N");
            } else {
                // skip.
            }
            attributeBuilder.append(" ");
        }
        // attributes[featureVector.size()] = Attribute.LABEL;
        attributeBuilder.append("L");
        return attributeBuilder.toString();
    }

    @Override
    public CategoryEntries predict(FeatureVector vector) {
        String data = getData(vector);
        data = data.concat(",").concat("0");
//        data = data.concat(",").concat("X");
        // System.out.println("data:" + data);
        org.apache.mahout.classifier.df.data.Instance inst = converter.convert(data);
        // System.out.println("instance: " + inst);
//        double classificationResult = node.classify(inst);
        // System.out.println(classificationResult);
        System.out.println(forest.nbNodes());
        double classificationResult = forest.classify(dataset, RandomUtils.JVM_RANDOM, inst);
        System.out.println(classificationResult);
        // System.out.println(classificationResult);
        // System.out.println(toString());
        
        CategoryEntries ce = new CategoryEntries();
//        for (int i = 0; i < labels.length; i++) {
//            ce.add(new CategoryEntry(ce, new Category(labels[i]), (int)classificationResult == i?1.:0.));
//        }
        ce.add(new CategoryEntry(ce, new Category("true"), classificationResult));
        ce.add(new CategoryEntry(ce, new Category("false"), 1-classificationResult));
        return ce;
    }

    @Override
    public String toString() {
        return node.toString();
    }

}
