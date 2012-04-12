package ws.palladian.classification.numeric;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.Instances;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.helper.io.ResourceHelper;

public class KNNClassifierTest {

    @Test
    public void testKNNClassifier() {

        // create some instances for the vector space
        Instances<UniversalInstance> trainingInstances = new Instances<UniversalInstance>();

        UniversalInstance trainingInstance = null;
        List<Double> features = null;

        // instance 1
        trainingInstance = new UniversalInstance(trainingInstances);
        features = new ArrayList<Double>();
        features.add(3d);
        features.add(4d);
        features.add(5d);
        trainingInstance.setNumericFeatures(features);
        trainingInstance.setClassNominal(true);
        trainingInstance.setInstanceCategory("A");
        trainingInstances.add(trainingInstance);

        // instance 2
        trainingInstance = new UniversalInstance(trainingInstances);
        features = new ArrayList<Double>();
        features.add(3d);
        features.add(6d);
        features.add(6d);
        trainingInstance.setNumericFeatures(features);
        trainingInstance.setClassNominal(true);
        trainingInstance.setInstanceCategory("A");
        trainingInstances.add(trainingInstance);

        // instance 3
        trainingInstance = new UniversalInstance(trainingInstances);
        features = new ArrayList<Double>();
        features.add(4d);
        features.add(4d);
        features.add(4d);
        trainingInstance.setNumericFeatures(features);
        trainingInstance.setClassNominal(true);
        trainingInstance.setInstanceCategory("B");
        trainingInstances.add(trainingInstance);

        // create the KNN classifier and add the training instances
        KnnClassifier knn = new KnnClassifier();
        knn.setTrainingInstances(trainingInstances);

        // create an instance to classify
        UniversalInstance newInstance = new UniversalInstance(null);
        features = new ArrayList<Double>();
        features.add(1d);
        features.add(2d);
        features.add(3d);
        newInstance.setNumericFeatures(features);

        // classify
        knn.classify(newInstance);

        assertEquals(0.4743704726540487, newInstance.getMainCategoryEntry().getAbsoluteRelevance(), 0);
        assertEquals("A", newInstance.getMainCategoryEntry().getCategory().getName());
    }

    @Test
    public void testKNNClassifierLoadFromFile() throws FileNotFoundException {

        // create the KNN classifier and add the training instances
        KnnClassifier knn = new KnnClassifier();
        knn.trainFromCSV(ResourceHelper.getResourcePath("/classifier/wineData.txt"));

        // create an instance to classify
        // 13.82;1.75;2.42;14;111;3.88;3.74;.32;1.87;7.05;1.01;3.26;1190;1 => this is an actual instance from the
        // training data and should therefore also be classified as "1"
        UniversalInstance newInstance = new UniversalInstance(null);
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
        newInstance.setNumericFeatures(features);

        // classify
        knn.classify(newInstance);

        assertEquals(1.0000000001339825E9, newInstance.getMainCategoryEntry().getAbsoluteRelevance(), 0);
        assertEquals("1", newInstance.getMainCategoryEntry().getCategory().getName());
    }

    @Test
    public void testKNNClassifierLoadFromFileNormalize() throws FileNotFoundException {

        // create the KNN classifier and add the training instances
        KnnClassifier knn = new KnnClassifier();
        knn.trainFromCSV(ResourceHelper.getResourcePath("/classifier/wineData.txt"));
        knn.getTrainingInstances().normalize();

        knn.setName("testKNN");
        knn.save("data/temp/");

        KnnClassifier loadedKnn = KnnClassifier.load("data/temp/testKNN.gz");

        // create an instance to classify
        // 13.82;1.75;2.42;14;111;3.88;3.74;.32;1.87;7.05;1.01;3.26;1190;1 => this is an actual instance from the
        // training data and should therefore also be classified as "1"
        UniversalInstance newInstance = new UniversalInstance(null);
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
        newInstance.setNumericFeatures(features);

        // classify
        loadedKnn.classify(newInstance);

        assertEquals(1.0000000054326154E9, newInstance.getMainCategoryEntry().getAbsoluteRelevance(), 0);
        assertEquals("1", newInstance.getMainCategoryEntry().getCategory().getName());
    }

}