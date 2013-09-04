package ws.palladian.classification.numeric;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Instance;
import ws.palladian.classification.InstanceBuilder;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.BasicFeatureVectorImpl;

/**
 * <p>
 * Tests for the numerical KNN classifier.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 */

public class KnnClassifierTest {

    /**
     * <p>
     * Tests the typical in memory usage of the Knn classifier. It is trained with three instances and tried out on one
     * {@link BasicFeatureVectorImpl}. In the end the top class and its absolute relevance need to be correct.
     * </p>
     */

    @Test
    public void testKnnClassifier() {
        // create some instances for the vector space
        List<Instance> trainingInstances = CollectionHelper.newArrayList();
        trainingInstances.add(new InstanceBuilder().set("f1", 3d).set("f2", 4d).set("f3", 5d).create("A"));
        trainingInstances.add(new InstanceBuilder().set("f1", 3d).set("f2", 6d).set("f3", 6d).create("A"));
        trainingInstances.add(new InstanceBuilder().set("f1", 4d).set("f2", 4d).set("f3", 4d).create("B"));

        // create the KNN classifier and add the training instances
        KnnClassifier knn = new KnnClassifier();
        KnnModel model = knn.train(trainingInstances);
        BasicFeatureVectorImpl featureVector = new InstanceBuilder().set("f1", 1d).set("f2", 2d).set("f3", 3d).create();

        // classify
        CategoryEntries result = knn.classify(featureVector, model);
        assertEquals(0.474, result.getProbability(result.getMostLikelyCategory()), 0.001);
        assertEquals("A", result.getMostLikelyCategory());
    }

    /**
     * <p>
     * Tests whether the {@link KnnClassifier} works correctly on a larger dataset loaded directly from a CSV file.
     * </p>
     */
    @Test
    public void testKnnClassifierLoadFromFile() throws Exception {
        // create the KNN classifier and add the training instances
        KnnClassifier knn = new KnnClassifier(3);
        List<Trainable> instances = ClassificationUtils.readCsv(
                ResourceHelper.getResourcePath("/classifier/wineData.txt"), false);
        KnnModel model = knn.train(instances);

        // create an instance to classify
        // 13.82;1.75;2.42;14;111;3.88;3.74;.32;1.87;7.05;1.01;3.26;1190;1 =>
        // this is an actual instance from the
        // training data and should therefore also be classified as "1"
        InstanceBuilder instanceBuilder = new InstanceBuilder();
        instanceBuilder.set("0", 13.82);
        instanceBuilder.set("1", 1.75);
        instanceBuilder.set("2", 2.42);
        instanceBuilder.set("3", 14d);
        instanceBuilder.set("4", 111d);
        instanceBuilder.set("5", 3.88);
        instanceBuilder.set("6", 3.74);
        instanceBuilder.set("7", .32);
        instanceBuilder.set("8", 1.87);
        instanceBuilder.set("9", 7.05);
        instanceBuilder.set("10", 1.01);
        instanceBuilder.set("11", 3.26);
        instanceBuilder.set("12", 1190d);
        BasicFeatureVectorImpl featureVector = instanceBuilder.create();

        // classify
        CategoryEntries result = knn.classify(featureVector, model);
        assertEquals(1.0000000001339825E9, result.getProbability(result.getMostLikelyCategory()), 0);
        assertEquals("1", result.getMostLikelyCategory());
    }

    @Test
    public void testKnnClassifierLoadFromFileNormalize() throws Exception {
        // create the KNN classifier and add the training instances
        KnnClassifier knn = new KnnClassifier(3);
        String testDataPath = ResourceHelper.getResourcePath("/classifier/wineData.txt");
        KnnModel model = knn.train(ClassificationUtils.readCsv(testDataPath, false));
        model.normalize();
        String tempDir = System.getProperty("java.io.tmpdir");
        FileHelper.serialize(model, tempDir + "/testKNN.gz");
        KnnModel loadedModel = FileHelper.deserialize(tempDir + "/testKNN.gz");

        // create an instance to classify
        // 13.82;1.75;2.42;14;111;3.88;3.74;.32;1.87;7.05;1.01;3.26;1190;1 =>
        // this is an actual instance from the
        // training data and should therefore also be classified as "1"
        InstanceBuilder instanceBuilder = new InstanceBuilder();
        instanceBuilder.set("0", 13.82);
        instanceBuilder.set("1", 1.75);
        instanceBuilder.set("2", 2.42);
        instanceBuilder.set("3", 14d);
        instanceBuilder.set("4", 111d);
        instanceBuilder.set("5", 3.88);
        instanceBuilder.set("6", 3.74);
        instanceBuilder.set("7", .32);
        instanceBuilder.set("8", 1.87);
        instanceBuilder.set("9", 7.05);
        instanceBuilder.set("10", 1.01);
        instanceBuilder.set("11", 3.26);
        instanceBuilder.set("12", 1190d);

        // classify
        CategoryEntries result = knn.classify(instanceBuilder.create(), loadedModel);
        assertEquals(1.0000000054326154E9, result.getProbability(result.getMostLikelyCategory()), 0);
        assertEquals("1", result.getMostLikelyCategory());
    }

}
