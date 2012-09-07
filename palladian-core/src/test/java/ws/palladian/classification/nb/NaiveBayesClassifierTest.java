package ws.palladian.classification.nb;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.InstanceBuilder;
import ws.palladian.classification.NominalInstance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * Tests for {@link NaiveBayesClassifier}.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @formatter:off
 */
public class NaiveBayesClassifierTest {
    
    @Test
    public void testNaiveBayesNominal() {
        List<NominalInstance> instances = CollectionHelper.newArrayList();
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
        
        NaiveBayesClassifier bayesClassifier = new NaiveBayesClassifier();
        NaiveBayesModel model = bayesClassifier.learn(instances);
        FeatureVector featureVector = new InstanceBuilder().set("outlook", "sunny").set("temp", "cool").set("humidity", "high").set("windy", "true").create();
        CategoryEntries categoryEntries = bayesClassifier.predict(featureVector, model);
        assertEquals(0.262, categoryEntries.getCategoryEntry("yes").getAbsoluteRelevance(), 0.001);
        assertEquals(0.738, categoryEntries.getCategoryEntry("no").getAbsoluteRelevance(), 0.001);

        featureVector = new InstanceBuilder().set("outlook", "overcase").set("temp", "cool").set("humidity", "high").set("windy", "true").create();
        categoryEntries = bayesClassifier.predict(featureVector, model);
        assertEquals(0.321, categoryEntries.getCategoryEntry("yes").getAbsoluteRelevance(), 0.001);
        assertEquals(0.679, categoryEntries.getCategoryEntry("no").getAbsoluteRelevance(), 0.001);

        // missing values
        featureVector = new InstanceBuilder().set("temp", "cool").set("humidity", "high").set("windy", "true").create();
        categoryEntries = bayesClassifier.predict(featureVector, model);
        assertEquals(0.426, categoryEntries.getCategoryEntry("yes").getAbsoluteRelevance(), 0.001);
        assertEquals(0.574, categoryEntries.getCategoryEntry("no").getAbsoluteRelevance(), 0.001);
    }
    
    @Test
    public void testNaiveBayesNumeric() {
        List<NominalInstance> instances = CollectionHelper.newArrayList();
        instances.add(new InstanceBuilder().set("f", 3.0).create("Case"));
        instances.add(new InstanceBuilder().set("f", 6.0).create("Case"));
        instances.add(new InstanceBuilder().set("f", 20.0).create("Case"));
        instances.add(new InstanceBuilder().set("f", 18.0).create("Phone"));
        instances.add(new InstanceBuilder().set("f", 66.0).create("Phone"));
        instances.add(new InstanceBuilder().set("f", 290.0).create("Phone"));
        
        NaiveBayesClassifier bayesClassifier = new NaiveBayesClassifier();
        NaiveBayesModel model = bayesClassifier.learn(instances);
        
        // create an instance to classify
        FeatureVector featureVector = new InstanceBuilder().set("f", 16.0).create();
        CategoryEntries categoryEntries = bayesClassifier.predict(featureVector, model);
        
        assertEquals(0.944, MathHelper.round(categoryEntries.getMostLikelyCategoryEntry().getRelevance(), 3), 0.01);
        assertEquals("Case", categoryEntries.getMostLikelyCategoryEntry().getCategory().getName());
    }

}