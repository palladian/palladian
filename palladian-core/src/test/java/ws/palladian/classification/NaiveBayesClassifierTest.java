package ws.palladian.classification;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.classification.nb.NaiveBayesClassifier;
import ws.palladian.classification.nb.NaiveBayesModel;
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
        
        CategoryEntries categoryEntries = bayesClassifier.predict(new InstanceBuilder().set("outlook", "sunny").set("temp", "cool").set("humidity", "high").set("windy", "true").create(), model);
        System.out.println(categoryEntries);
        // assertEquals(0.205, categoryEntries.getCategoryEntry("yes").getAbsoluteRelevance(), 0.001);
        // assertEquals(0.795, categoryEntries.getCategoryEntry("no").getAbsoluteRelevance(), 0.001);
        assertEquals(0.259, categoryEntries.getCategoryEntry("yes").getAbsoluteRelevance(), 0.001);
        assertEquals(0.741, categoryEntries.getCategoryEntry("no").getAbsoluteRelevance(), 0.001);
        
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