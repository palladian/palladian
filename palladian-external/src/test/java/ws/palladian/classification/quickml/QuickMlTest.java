package ws.palladian.classification.quickml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ws.palladian.classification.utils.ClassifierEvaluation.evaluate;
import static ws.palladian.helper.io.ResourceHelper.getResourceFile;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.helper.math.ConfusionMatrix;

public class QuickMlTest {

    @Test
    @Ignore // sporadically fails
    public void testDecisionTreeClassifier() {

        // sample data taken from https://github.com/sanity/quickdt
        List<Instance> instances = new ArrayList<>();

        instances.add(new InstanceBuilder().set("height", 55.).set("weight", 168.).set("gender", "male").create("overweight"));
        instances.add(new InstanceBuilder().set("height", 75.).set("weight", 168.).set("gender", "female").create("healthy"));
        instances.add(new InstanceBuilder().set("height", 74.).set("weight", 143.).set("gender", "male").create("underweight"));
        instances.add(new InstanceBuilder().set("height", 49.).set("weight", 144.).set("gender", "female").create("underweight"));
        instances.add(new InstanceBuilder().set("height", 83.).set("weight", 223.).set("gender", "male").create("healthy"));

        QuickMlLearner learner = QuickMlLearner.tree();
        QuickMlModel model = learner.train(instances);

        FeatureVector featureVector = new InstanceBuilder().set("height", 62.).set("weight", 201.).set("gender", "female").create();
        QuickMlClassifier classifier = new QuickMlClassifier();
        CategoryEntries prediction = classifier.classify(featureVector, model);

        assertEquals("healthy", prediction.getMostLikelyCategory());
        // assertEquals(0.4, prediction.getProbability("healthy"), 0);
        // assertEquals(0.4, prediction.getProbability("underweight"), 0);
        // assertEquals(0.2, prediction.getProbability("overweight"), 0);
    }

    @Test
    public void testWithAdultIncomeData() throws FileNotFoundException {
        List<Instance> instances = new CsvDatasetReader(getResourceFile("/adultData.txt"), false).readAll();
        ConfusionMatrix confusionMatrix = evaluate(QuickMlLearner.randomForest(), new QuickMlClassifier(), instances);
        double accuracy = confusionMatrix.getAccuracy();
        assertGreater(0.75, accuracy);
    }

    private void assertGreater(double expected, double actual) {
        assertTrue("value should be > " + expected + ", but was " + actual, actual > expected);
    }

    @Test
    public void testWithDiabetesData() throws FileNotFoundException {
        List<Instance> instances = new CsvDatasetReader(getResourceFile("/diabetesData.txt"), false)
                .readAll();
        ConfusionMatrix confusionMatrix = evaluate(QuickMlLearner.randomForest(), new QuickMlClassifier(), instances);
        double accuracy = confusionMatrix.getAccuracy();
        assertGreater(0.72, accuracy);
    }

}
