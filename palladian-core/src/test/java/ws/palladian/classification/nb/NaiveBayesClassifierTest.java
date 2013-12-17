package ws.palladian.classification.nb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ws.palladian.classification.utils.ClassifierEvaluation.evaluate;
import static ws.palladian.helper.io.ResourceHelper.getResourceFile;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Instance;
import ws.palladian.classification.InstanceBuilder;
import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * Tests for {@link NaiveBayesClassifier}.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class NaiveBayesClassifierTest {

    @Test
    public void testNaiveBayesWithPlayData() {
        List<Instance> instances = CollectionHelper.newArrayList();
        // @formatter:off
        instances.add(new InstanceBuilder().set("outlook", "sunny").set("temp", "hot").set("humidity", "high").set("windy", "false").create("no"));
        instances.add(new InstanceBuilder().set("outlook", "sunny").set("temp", "hot").set("humidity", "high").set("windy", "true").create("no"));
        instances.add(new InstanceBuilder().set("outlook", "overcast").set("temp", "hot").set("humidity", "high").set("windy", "false").create("yes"));
        instances.add(new InstanceBuilder().set("outlook", "rainy").set("temp", "mild").set("humidity", "high").set("windy", "false").create("yes"));
        instances.add(new InstanceBuilder().set("outlook", "rainy").set("temp", "cool").set("humidity", "normal").set("windy", "false").create("yes"));
        instances.add(new InstanceBuilder().set("outlook", "rainy").set("temp", "cool").set("humidity", "normal").set("windy", "true").create("no"));
        instances.add(new InstanceBuilder().set("outlook", "overcast").set("temp", "cool").set("humidity", "normal").set("windy", "true").create("yes"));
        instances.add(new InstanceBuilder().set("outlook", "sunny").set("temp", "mild").set("humidity", "high").set("windy", "false").create("no"));
        instances.add(new InstanceBuilder().set("outlook", "sunny").set("temp", "cool").set("humidity", "normal").set("windy", "false").create("yes"));
        instances.add(new InstanceBuilder().set("outlook", "rainy").set("temp", "mild").set("humidity", "normal").set("windy", "false").create("yes"));
        instances.add(new InstanceBuilder().set("outlook", "sunny").set("temp", "mild").set("humidity", "normal").set("windy", "true").create("yes"));
        instances.add(new InstanceBuilder().set("outlook", "overcast").set("temp", "mild").set("humidity", "high").set("windy", "true").create("yes"));
        instances.add(new InstanceBuilder().set("outlook", "overcast").set("temp", "hot").set("humidity", "normal").set("windy", "false").create("yes"));
        instances.add(new InstanceBuilder().set("outlook", "rainy").set("temp", "mild").set("humidity", "high").set("windy", "true").create("no"));

        NaiveBayesClassifier bayesClassifier = new NaiveBayesClassifier(1);
        NaiveBayesModel model = new NaiveBayesLearner().train(instances);
        assertEquals(2, model.getCategories().size());
        assertTrue(model.getCategories().contains("yes"));
        assertTrue(model.getCategories().contains("no"));
        
        FeatureVector featureVector = new InstanceBuilder().set("outlook", "sunny").set("temp", "cool").set("humidity", "high").set("windy", "true").create();
        CategoryEntries categoryEntries = bayesClassifier.classify(featureVector, model);
        assertEquals(0.262, categoryEntries.getProbability("yes"), 0.001);
        assertEquals(0.738, categoryEntries.getProbability("no"), 0.001);

        featureVector = new InstanceBuilder().set("outlook", "overcast").set("temp", "cool").set("humidity", "high").set("windy", "true").create();
        categoryEntries = bayesClassifier.classify(featureVector, model);
        assertEquals(0.703, categoryEntries.getProbability("yes"), 0.001);
        assertEquals(0.297, categoryEntries.getProbability("no"), 0.001);

        // missing values
        featureVector = new InstanceBuilder().set("temp", "cool").set("humidity", "high").set("windy", "true").create();
        categoryEntries = bayesClassifier.classify(featureVector, model);
        assertEquals(0.426, categoryEntries.getProbability("yes"), 0.001);
        assertEquals(0.574, categoryEntries.getProbability("no"), 0.001);
        // @formatter:on
    }

    @Test
    public void testNaiveBayesNumeric() {
        List<Instance> instances = CollectionHelper.newArrayList();
        instances.add(new InstanceBuilder().set("f", 3.0).create("Case"));
        instances.add(new InstanceBuilder().set("f", 6.0).create("Case"));
        instances.add(new InstanceBuilder().set("f", 20.0).create("Case"));
        instances.add(new InstanceBuilder().set("f", 18.0).create("Phone"));
        instances.add(new InstanceBuilder().set("f", 66.0).create("Phone"));
        instances.add(new InstanceBuilder().set("f", 290.0).create("Phone"));

        NaiveBayesModel model = new NaiveBayesLearner().train(instances);

        // create an instance to classify
        FeatureVector featureVector = new InstanceBuilder().set("f", 16.0).create();
        CategoryEntries categoryEntries = new NaiveBayesClassifier().classify(featureVector, model);

        assertEquals(0.944, categoryEntries.getProbability(categoryEntries.getMostLikelyCategory()), 0.01);
        assertEquals("Case", categoryEntries.getMostLikelyCategory());
    }

    @Test
    public void testNaiveBayesWithAdultIncomeData() throws FileNotFoundException {
        List<Trainable> instances = new CsvDatasetReader(getResourceFile("/classifier/adultData.txt"), false).readAll();
        ConfusionMatrix matrix = evaluate(new NaiveBayesLearner(), new NaiveBayesClassifier(), instances);
        assertTrue(matrix.getAccuracy() > 0.77);
    }

    @Test
    public void testNaiveBayesWithDiabetesData() throws FileNotFoundException {
        List<Trainable> instances = new CsvDatasetReader(getResourceFile("/classifier/diabetesData.txt"), false).readAll();
        ConfusionMatrix matrix = evaluate(new NaiveBayesLearner(), new NaiveBayesClassifier(), instances);
        assertTrue(matrix.getAccuracy() > 0.77);
    }

}
