package ws.palladian.classification.dt;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Instance;
import ws.palladian.classification.InstanceBuilder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.BasicFeatureVectorImpl;

public class DecisionTreeClassifierTest {

    @Test
    public void testDecisionTreeClassifier() {

        // sample data taken from https://github.com/sanity/quickdt
        List<Instance> instances = CollectionHelper.newArrayList();

        instances.add(new InstanceBuilder().set("height", 55.).set("weight", 168.).set("gender", "male").create("overweight"));
        instances.add(new InstanceBuilder().set("height", 75.).set("weight", 168.).set("gender", "female").create("healthy"));
        instances.add(new InstanceBuilder().set("height", 74.).set("weight", 143.).set("gender", "male").create("underweight"));
        instances.add(new InstanceBuilder().set("height", 49.).set("weight", 144.).set("gender", "female").create("underweight"));
        instances.add(new InstanceBuilder().set("height", 83.).set("weight", 223.).set("gender", "male").create("healthy"));

        DecisionTreeClassifier classifier = new DecisionTreeClassifier();
        DecisionTreeModel model = classifier.train(instances);


        BasicFeatureVectorImpl featureVector = new InstanceBuilder().set("height", 62.).set("weight", 201.).set("gender", "female").create();
        CategoryEntries prediction = classifier.classify(featureVector, model);

        assertEquals(1., prediction.getProbability(prediction.getMostLikelyCategory()), 0);
        assertEquals("underweight", prediction.getMostLikelyCategory());
    }

}
