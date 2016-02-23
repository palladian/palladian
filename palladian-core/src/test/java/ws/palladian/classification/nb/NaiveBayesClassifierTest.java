package ws.palladian.classification.nb;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ws.palladian.classification.utils.ClassifierEvaluation.evaluate;
import static ws.palladian.helper.io.ResourceHelper.getResourceFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;
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
        List<Instance> instances = new ArrayList<>();
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
		assertEquals(4, model.getLearnedFeatures().size());
		assertEquals(new HashSet<>(asList("outlook", "temp", "humidity", "windy")), model.getLearnedFeatures());
        
        FeatureVector featureVector = new InstanceBuilder().set("outlook", "sunny").set("temp", "cool").set("humidity", "high").set("windy", "true").create();
        CategoryEntries categoryEntries = bayesClassifier.classify(featureVector, model);
        assertEquals("no", categoryEntries.getMostLikelyCategory());

        featureVector = new InstanceBuilder().set("outlook", "overcast").set("temp", "cool").set("humidity", "high").set("windy", "true").create();
        categoryEntries = bayesClassifier.classify(featureVector, model);
        assertEquals("yes", categoryEntries.getMostLikelyCategory());

        // missing values
        featureVector = new InstanceBuilder().set("temp", "cool").set("humidity", "high").set("windy", "true").create();
        categoryEntries = bayesClassifier.classify(featureVector, model);
        assertEquals("no", categoryEntries.getMostLikelyCategory());
        
        // features which were not trained (should simply be ignored)
        featureVector = new InstanceBuilder().set("outlook", "overcast").set("temp", "cool").set("humidity", "high").set("windy", "true").set("a", "a").set("b", "b").set("c", "c").create();
        categoryEntries = bayesClassifier.classify(featureVector, model);
        assertEquals("yes", categoryEntries.getMostLikelyCategory());
        
        // @formatter:on
    }

    @Test
    public void testNaiveBayesNumeric() {
        List<Instance> instances = new ArrayList<>();
        instances.add(new InstanceBuilder().set("f", 3.0).create("Case"));
        instances.add(new InstanceBuilder().set("f", 6.0).create("Case"));
        instances.add(new InstanceBuilder().set("f", 20.0).create("Case"));
        instances.add(new InstanceBuilder().set("f", 18.0).create("Phone"));
        instances.add(new InstanceBuilder().set("f", 66.0).create("Phone"));
        instances.add(new InstanceBuilder().set("f", 290.0).create("Phone"));

        NaiveBayesModel model = new NaiveBayesLearner().train(instances);
        
        assertEquals(1, model.getLearnedFeatures().size());
        assertEquals(new HashSet<>(asList("f")), model.getLearnedFeatures());

        // create an instance to classify
        FeatureVector featureVector = new InstanceBuilder().set("f", 16.0).create();
        CategoryEntries categoryEntries = new NaiveBayesClassifier().classify(featureVector, model);

        assertEquals("Case", categoryEntries.getMostLikelyCategory());
    }

    @Test
    public void testNaiveBayesWithAdultIncomeData() throws FileNotFoundException {
        List<Instance> instances = new CsvDatasetReader(getResourceFile("/classifier/adultData.txt"), false).readAll();
        ConfusionMatrix matrix = evaluate(new NaiveBayesLearner(), new NaiveBayesClassifier(), instances);
        assertTrue(matrix.getAccuracy() > 0.77);
    }

    @Test
    public void testNaiveBayesWithDiabetesData() throws FileNotFoundException {
        List<Instance> instances = new CsvDatasetReader(getResourceFile("/classifier/diabetesData.txt"), false).readAll();
        ConfusionMatrix matrix = evaluate(new NaiveBayesLearner(), new NaiveBayesClassifier(), instances);
        assertTrue(matrix.getAccuracy() > 0.77);
    }
    
    @Test
    public void testSerialization() throws IOException {
        List<Instance> instances = new CsvDatasetReader(getResourceFile("/classifier/diabetesData.txt"), false).readAll();
        NaiveBayesLearner learner = new NaiveBayesLearner();
        NaiveBayesModel model = learner.train(instances);
        String tempFile = new File(FileHelper.getTempDir(), "naiveBayes.model").getPath();
        FileHelper.serialize(model, tempFile);
        
        NaiveBayesModel deserialized = FileHelper.deserialize(tempFile);
        assertNotNull(deserialized);
    }

}
