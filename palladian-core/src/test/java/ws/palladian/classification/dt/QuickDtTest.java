package ws.palladian.classification.dt;

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

public class QuickDtTest {

//    @Test
//    public void testDirectly() {
//        final Set<quickdt.Instance> instances = CollectionHelper.newHashSet();
//        // A male weighing 168lb that is 55 inches tall, they are overweight
//        instances.add(HashMapAttributes.create("height", 55, "weight", 168, "gender", "male").classification(
//                "overweight"));
//        instances.add(HashMapAttributes.create("height", 75, "weight", 168, "gender", "female").classification(
//                "healthy"));
//        instances.add(HashMapAttributes.create("height", 74, "weight", 143, "gender", "male").classification(
//                "underweight"));
//        instances.add(HashMapAttributes.create("height", 49, "weight", 144, "gender", "female").classification(
//                "underweight"));
//        instances
//                .add(HashMapAttributes.create("height", 83, "weight", 223, "gender", "male").classification("healthy"));
//
//        TreeBuilder treeBuilder = new TreeBuilder();
//        Tree tree = treeBuilder.buildPredictiveModel(instances);
//        Attributes attributes = HashMapAttributes.create("height", 62, "weight", 201, "gender", "female");
//        Serializable classification = tree.getClassificationByMaxProb(attributes);
//        double probability = tree.getProbability(attributes, classification);
//        System.out.println("classification: " + classification);
//        System.out.println("probability " + probability);
//    }

    @Test
    public void testDecisionTreeClassifier() {

        // sample data taken from https://github.com/sanity/quickdt
        List<Instance> instances = CollectionHelper.newArrayList();

        instances.add(new InstanceBuilder().set("height", 55.).set("weight", 168.).set("gender", "male").create("overweight"));
        instances.add(new InstanceBuilder().set("height", 75.).set("weight", 168.).set("gender", "female").create("healthy"));
        instances.add(new InstanceBuilder().set("height", 74.).set("weight", 143.).set("gender", "male").create("underweight"));
        instances.add(new InstanceBuilder().set("height", 49.).set("weight", 144.).set("gender", "female").create("underweight"));
        instances.add(new InstanceBuilder().set("height", 83.).set("weight", 223.).set("gender", "male").create("healthy"));

        QuickDtLearner learner = QuickDtLearner.tree();
        QuickDtModel model = learner.train(instances);

        FeatureVector featureVector = new InstanceBuilder().set("height", 62.).set("weight", 201.).set("gender", "female").create();
        QuickDtClassifier classifier = new QuickDtClassifier();
        CategoryEntries prediction = classifier.classify(featureVector, model);

        assertEquals("healthy", prediction.getMostLikelyCategory());
        assertEquals(0.4, prediction.getProbability("healthy"), 0);
        assertEquals(0.4, prediction.getProbability("underweight"), 0);
        assertEquals(0.2, prediction.getProbability("overweight"), 0);
    }

    @Test
    public void testWithAdultIncomeData() throws FileNotFoundException {
        List<Trainable> instances = new CsvDatasetReader(getResourceFile("/classifier/adultData.txt"), false).readAll();
        ConfusionMatrix confusionMatrix = evaluate(QuickDtLearner.randomForest(), new QuickDtClassifier(), instances);
        double accuracy = confusionMatrix.getAccuracy();
        assertGreater(0.75, accuracy);
    }

    private void assertGreater(double expected, double actual) {
        assertTrue("value should be > " + expected + ", but was " + actual, actual > expected);
    }

    @Test
    public void testWithDiabetesData() throws FileNotFoundException {
        List<Trainable> instances = new CsvDatasetReader(getResourceFile("/classifier/diabetesData.txt"), false)
                .readAll();
        ConfusionMatrix confusionMatrix = evaluate(QuickDtLearner.randomForest(), new QuickDtClassifier(), instances);
        double accuracy = confusionMatrix.getAccuracy();
        assertGreater(0.72, accuracy);
    }

}
