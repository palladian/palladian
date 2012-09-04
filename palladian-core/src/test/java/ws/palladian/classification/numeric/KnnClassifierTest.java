package ws.palladian.classification.numeric;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.ClassificationUtils;
import ws.palladian.classification.Instances;
import ws.palladian.classification.NominalInstance;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Tests whether the numerical Knn Classifier works correctly or not.
 * </p>
 * @author Klemens Muthmann
 *
 */
public class KnnClassifierTest {

    @Test
    public void testKNNClassifier() {

        // create some instances for the vector space
        List<NominalInstance> trainingInstances = new ArrayList<NominalInstance>(3);

//        UniversalInstance trainingInstance = null;
//        List<Double> features = null;

        // instance 1
//        trainingInstance = new UniversalInstance(trainingInstances);
//        features = new ArrayList<Double>();
//        features.add(3d);
//        features.add(4d);
//        features.add(5d);
//        trainingInstance.setNumericFeatures(features);
//        trainingInstance.setClassNominal(true);
//        trainingInstance.setInstanceCategory("A");
//        trainingInstances.add(trainingInstance);
        
        NominalInstance trainingInstance1 = new NominalInstance();
        trainingInstance1.target = "A";
        FeatureVector vector1 = new FeatureVector();
        vector1.add(new NumericFeature("f1", 3d));
        vector1.add(new NumericFeature("f2", 4d));
        vector1.add(new NumericFeature("f3", 5d));
        trainingInstance1.featureVector = vector1;
        trainingInstances.add(trainingInstance1);

        // instance 2
//        trainingInstance = new UniversalInstance(trainingInstances);
//        features = new ArrayList<Double>();
//        features.add(3d);
//        features.add(6d);
//        features.add(6d);
//        trainingInstance.setNumericFeatures(features);
//        trainingInstance.setClassNominal(true);
//        trainingInstance.setInstanceCategory("A");
//        trainingInstances.add(trainingInstance);
        
        NominalInstance trainingInstance2 = new NominalInstance();
        trainingInstance2.target = "A";
        FeatureVector vector2 = new FeatureVector();
        vector2.add(new NumericFeature("f1", 3d));
        vector2.add(new NumericFeature("f2", 6d));
        vector2.add(new NumericFeature("f3", 6d));
        trainingInstance2.featureVector = vector2;
        trainingInstances.add(trainingInstance2);

        // instance 3
//        trainingInstance = new UniversalInstance(trainingInstances);
//        features = new ArrayList<Double>();
//        features.add(4d);
//        features.add(4d);
//        features.add(4d);
//        trainingInstance.setNumericFeatures(features);
//        trainingInstance.setClassNominal(true);
//        trainingInstance.setInstanceCategory("B");
//        trainingInstances.add(trainingInstance);
        
        NominalInstance trainingInstance3 = new NominalInstance();
        trainingInstance3.target = "B";
        FeatureVector vector3 = new FeatureVector();
        vector3.add(new NumericFeature("f1", 4d));
        vector3.add(new NumericFeature("f2", 4d));
        vector3.add(new NumericFeature("f3", 4d));
        trainingInstance3.featureVector = vector3;
        trainingInstances.add(trainingInstance3);

        // create the KNN classifier and add the training instances
        KnnClassifier knn = new KnnClassifier();
        KnnModel model = knn.learn(trainingInstances);

        // create an instance to classify
//        UniversalInstance newInstance = new UniversalInstance(null);
//        features = new ArrayList<Double>();
//        features.add(1d);
//        features.add(2d);
//        features.add(3d);
//        newInstance.setNumericFeatures(features);
        
        FeatureVector newInstance = new FeatureVector();
        newInstance.add(new NumericFeature("f1", 1d));
        newInstance.add(new NumericFeature("f2", 2d));
        newInstance.add(new NumericFeature("f3", 3d));

        // classify
        CategoryEntries result = knn.predict(newInstance,model);

        assertEquals(0.4743704726540487, ClassificationUtils.getSingleBestCategoryEntry(result).getAbsoluteRelevance(), 0);
        assertEquals("A", ClassificationUtils.getSingleBestCategoryEntry(result).getCategory().getName());
    }

//    @Test
//    public void testKNNClassifierLoadFromFile() throws FileNotFoundException {
//
//        // create the KNN classifier and add the training instances
//        KnnClassifier knn = new KnnClassifier(3);
//        knn.trainFromCSV(ResourceHelper.getResourcePath("/classifier/wineData.txt"));
//
//        // create an instance to classify
//        // 13.82;1.75;2.42;14;111;3.88;3.74;.32;1.87;7.05;1.01;3.26;1190;1 => this is an actual instance from the
//        // training data and should therefore also be classified as "1"
//        UniversalInstance newInstance = new UniversalInstance(null);
//        List<Double> features = new ArrayList<Double>();
//        features.add(13.82);
//        features.add(1.75);
//        features.add(2.42);
//        features.add(14d);
//        features.add(111d);
//        features.add(3.88);
//        features.add(3.74);
//        features.add(.32);
//        features.add(1.87);
//        features.add(7.05);
//        features.add(1.01);
//        features.add(3.26);
//        features.add(1190d);
//        newInstance.setNumericFeatures(features);
//
//        // classify
//        CategoryEntries result = knn.predict(newInstance);
//
//        assertEquals(1.0000000001339825E9, newInstance.getMainCategoryEntry().getAbsoluteRelevance(), 0);
//        assertEquals("1", newInstance.getMainCategoryEntry().getCategory().getName());
//    }
//
//    @Test
//    public void testKNNClassifierLoadFromFileNormalize() throws FileNotFoundException {
//
//        // create the KNN classifier and add the training instances
//        KnnClassifier knn = new KnnClassifier(3);
//        knn.trainFromCSV(ResourceHelper.getResourcePath("/classifier/wineData.txt"));
//        knn.getTrainingInstances().normalize();
//
////        knn.setName("testKNN");
//        knn.save("data/temp/");
//
//        KnnClassifier loadedKnn = KnnClassifier.load("data/temp/testKNN.gz");
//
//        // create an instance to classify
//        // 13.82;1.75;2.42;14;111;3.88;3.74;.32;1.87;7.05;1.01;3.26;1190;1 => this is an actual instance from the
//        // training data and should therefore also be classified as "1"
////        UniversalInstance newInstance = new UniversalInstance(null);
//        FeatureVector vector = new FeatureVector();
//        vector.add(new NumericFeature("f1", 13.82));
//        vector.add(new NumericFeature("f2", 1.75));
//        vector.add(new NumericFeature("f3", 2.42));
//        vector.add(new NumericFeature("f4", 14d));
//        vector.add(new NumericFeature("f5", 111d));
//        vector.add(new NumericFeature("f6", 3.88));
//        vector.add(new NumericFeature("f7", 3.74));
//        vector.add(new NumericFeature("f8", .32));
//        vector.add(new NumericFeature("f9", 1.87));
//        vector.add(new NumericFeature("f10", 7.05));
//        vector.add(new NumericFeature("f11", 1.01));
//        vector.add(new NumericFeature("f12", 3.26));
//        vector.add(new NumericFeature("f13", 1190d));
////        List<Double> features = new ArrayList<Double>();
////        features.add(13.82);
////        features.add(1.75);
////        features.add(2.42);
////        features.add(14d);
////        features.add(111d);
////        features.add(3.88);
////        features.add(3.74);
////        features.add(.32);
////        features.add(1.87);
////        features.add(7.05);
////        features.add(1.01);
////        features.add(3.26);
////        features.add(1190d);
////        newInstance.setNumericFeatures(features);
//
//        // classify
//        CategoryEntries result = loadedKnn.predict(vector, model);
//
//        assertEquals(1.0000000054326154E9, newInstance.getMainCategoryEntry().getAbsoluteRelevance(), 0);
//        assertEquals("1", newInstance.getMainCategoryEntry().getCategory().getName());
//    }

}