package ws.palladian.classification.numeric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Instance;
import ws.palladian.classification.InstanceBuilder;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.classification.utils.MinMaxNormalizer;
import ws.palladian.classification.utils.NoNormalizer;
import ws.palladian.classification.utils.ZScoreNormalizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * Tests for the numerical KNN classifier.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public class KnnClassifierTest {

    /**
     * <p>
     * Tests the typical in memory usage of the Knn classifier. It is trained with three instances and tried out on one
     * {@link FeatureVector}. In the end the top class and its absolute relevance need to be correct.
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
        KnnLearner knnLearner = new KnnLearner(new NoNormalizer());
        KnnModel model = knnLearner.train(trainingInstances);
        FeatureVector featureVector = new InstanceBuilder().set("f1", 1d).set("f2", 2d).set("f3", 3d).create();

        assertEquals(2, model.getCategories().size());
        assertTrue(model.getCategories().contains("A"));
        assertTrue(model.getCategories().contains("B"));

        // classify
        CategoryEntries result = new KnnClassifier(3).classify(featureVector, model);
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
        KnnLearner knnLearner = new KnnLearner(new NoNormalizer());
        List<Trainable> instances = ClassificationUtils.readCsv(
                ResourceHelper.getResourcePath("/classifier/wineData.txt"), false);
        KnnModel model = knnLearner.train(instances);
        assertEquals(3, model.getCategories().size());

        // classify
        CategoryEntries result = new KnnClassifier(3).classify(createTestInstance(), model);
        assertEquals(1.0000000001339825E9, result.getProbability(result.getMostLikelyCategory()), 0);
        assertEquals("1", result.getMostLikelyCategory());
    }

    @Test
    public void testKnnClassifierLoadFromFileNormalize() throws Exception {
        // create the KNN classifier and add the training instances
        KnnLearner knnLearner = new KnnLearner();
        String testDataPath = ResourceHelper.getResourcePath("/classifier/wineData.txt");
        KnnModel model = knnLearner.train(ClassificationUtils.readCsv(testDataPath, false));
        File tempDir = FileHelper.getTempDir();
        String tempFile = new File(tempDir, "/testKNN.gz").getPath();
        FileHelper.serialize(model, tempFile);
        KnnModel loadedModel = FileHelper.deserialize(tempFile);

        // classify
        CategoryEntries result = new KnnClassifier(3).classify(createTestInstance(), loadedModel);
        assertEquals(1.0000000054326154E9, result.getProbability(result.getMostLikelyCategory()), 0);
        assertEquals("1", result.getMostLikelyCategory());
    }

    @Test
    public void testWithAdultIncomeData() throws FileNotFoundException {
        List<Trainable> instances = ClassificationUtils.readCsv(
                ResourceHelper.getResourcePath("/classifier/adultData.txt"), false);
        KnnLearner learner = new KnnLearner(new NoNormalizer());
        ConfusionMatrix confusionMatrix = ClassifierEvaluation.evaluate(learner, new KnnClassifier(3), instances);
        assertTrue(confusionMatrix.getAccuracy() > 0.69);

        learner = new KnnLearner(new MinMaxNormalizer());
        confusionMatrix = ClassifierEvaluation.evaluate(learner, new KnnClassifier(3), instances);
        assertTrue(confusionMatrix.getAccuracy() > 0.69);

        learner = new KnnLearner(new ZScoreNormalizer());
        confusionMatrix = ClassifierEvaluation.evaluate(learner, new KnnClassifier(3), instances);
        assertTrue(confusionMatrix.getAccuracy() > 0.71);
    }

    private Classifiable createTestInstance() {
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
        return instanceBuilder.create();
    }

}
