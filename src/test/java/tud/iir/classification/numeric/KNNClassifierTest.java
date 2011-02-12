package tud.iir.classification.numeric;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;

import tud.iir.classification.Instances;

public class KNNClassifierTest extends TestCase {

    // private static final Logger LOGGER = Logger.getLogger(KNNClassifierTest.class);

    public KNNClassifierTest(String name) {
        super(name);
    }

    @Test
    public void testKNNClassifier() {

        // create some instances for the vector space
        Instances<NumericInstance> trainingInstances = new Instances<NumericInstance>();

        NumericInstance trainingInstance = null;
        List<Double> features = null;

        // instance 1
        trainingInstance = new NumericInstance(trainingInstances);
        features = new ArrayList<Double>();
        features.add(3d);
        features.add(4d);
        features.add(5d);
        trainingInstance.setFeatures(features);
        trainingInstance.setClassNominal(true);
        trainingInstance.setInstanceCategory("A");
        trainingInstances.add(trainingInstance);

        // instance 2
        trainingInstance = new NumericInstance(trainingInstances);
        features = new ArrayList<Double>();
        features.add(3d);
        features.add(6d);
        features.add(6d);
        trainingInstance.setFeatures(features);
        trainingInstance.setClassNominal(true);
        trainingInstance.setInstanceCategory("A");
        trainingInstances.add(trainingInstance);

        // instance 3
        trainingInstance = new NumericInstance(trainingInstances);
        features = new ArrayList<Double>();
        features.add(4d);
        features.add(4d);
        features.add(4d);
        trainingInstance.setFeatures(features);
        trainingInstance.setClassNominal(true);
        trainingInstance.setInstanceCategory("B");
        trainingInstances.add(trainingInstance);

        // create the KNN classifier and add the training instances
        KNNClassifier knn = new KNNClassifier();
        knn.setTrainingInstances(trainingInstances);

        // create an instance to classify
        NumericInstance newInstance = new NumericInstance(null);
        features = new ArrayList<Double>();
        features.add(1d);
        features.add(2d);
        features.add(3d);
        newInstance.setFeatures(features);

        // classify
        knn.classify(newInstance);

        Assert.assertEquals(0.4743704726540487, newInstance.getMainCategoryEntry().getAbsoluteRelevance());
        Assert.assertEquals("A", newInstance.getMainCategoryEntry().getCategory().getName());
    }

    @Test
    public void testKNNClassifierLoadFromFile() {

        // create the KNN classifier and add the training instances
        KNNClassifier knn = new KNNClassifier();
        knn.trainFromCSV(KNNClassifierTest.class.getResource("/classifier/wineData.txt").getFile());

        // create an instance to classify
        // 13.82;1.75;2.42;14;111;3.88;3.74;.32;1.87;7.05;1.01;3.26;1190;1 => this is an actual instance from the
        // training data and should therefore also be classified as "1"
        NumericInstance newInstance = new NumericInstance(null);
        List<Double> features = new ArrayList<Double>();
        features.add(13.82);
        features.add(1.75);
        features.add(2.42);
        features.add(14d);
        features.add(111d);
        features.add(3.88);
        features.add(3.74);
        features.add(.32);
        features.add(1.87);
        features.add(7.05);
        features.add(1.01);
        features.add(3.26);
        features.add(1190d);
        newInstance.setFeatures(features);

        // classify
        knn.classify(newInstance);

        Assert.assertEquals(1.0000000001565293E9, newInstance.getMainCategoryEntry().getAbsoluteRelevance());
        Assert.assertEquals("1", newInstance.getMainCategoryEntry().getCategory().getName());
    }

    @Test
    public void testKNNClassifierLoadFromFileNormalize() {

        // create the KNN classifier and add the training instances
        KNNClassifier knn = new KNNClassifier();
        knn.trainFromCSV(KNNClassifierTest.class.getResource("/classifier/wineData.txt").getFile());
        knn.getTrainingInstances().normalize();

        // create an instance to classify
        // 13.82;1.75;2.42;14;111;3.88;3.74;.32;1.87;7.05;1.01;3.26;1190;1 => this is an actual instance from the
        // training data and should therefore also be classified as "1"
        NumericInstance newInstance = new NumericInstance(null);
        List<Double> features = new ArrayList<Double>();
        features.add(13.82);
        features.add(1.75);
        features.add(2.42);
        features.add(14d);
        features.add(111d);
        features.add(3.88);
        features.add(3.74);
        features.add(.32);
        features.add(1.87);
        features.add(7.05);
        features.add(1.01);
        features.add(3.26);
        features.add(1190d);
        newInstance.setFeatures(features);

        // classify
        knn.classify(newInstance);

        Assert.assertEquals(1.0000000079926668E9, newInstance.getMainCategoryEntry().getAbsoluteRelevance());
        Assert.assertEquals("1", newInstance.getMainCategoryEntry().getCategory().getName());
    }

}