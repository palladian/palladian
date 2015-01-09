package ws.palladian.classification;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ws.palladian.classification.utils.ClassifierEvaluation.evaluate;
import static ws.palladian.helper.io.ResourceHelper.getResourceFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.meta.Bagging;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.ConfusionMatrix;

/**
 * <p>
 * Tests whether the Weka predictor works correctly with different feature sets.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public class WekaTest {

    @Test
    public void test() {

        InstanceBuilder builder = new InstanceBuilder();
        Instance instance1 = builder.set("a", 2.3).set("b", "value1").set("v1", true).create("c1");

        builder = new InstanceBuilder();
        Instance instance = builder.set("a", 1.1).set("b", "value2").set("v1", true).set("v2", true).create("c2");

        List<Instance> trainingInstances = new ArrayList<>();
        trainingInstances.add(instance1);
        trainingInstances.add(instance);
        WekaModel model = new WekaLearner(new NaiveBayes()).train(trainingInstances);

        assertEquals(2, model.getCategories().size());
        assertTrue(model.getCategories().contains("c1"));
        assertTrue(model.getCategories().contains("c2"));

        builder = new InstanceBuilder();
        FeatureVector testVector = builder.set("a", 1.5).set("b", "value2").set("v1", true).set("v2", true).create();
        CategoryEntries result = new WekaClassifier().classify(testVector, model);

        assertThat(result.getMostLikelyCategory(), isOneOf("c1", "c2"));
    }

    /**
     * <p>
     * Tests whether a {@link NominalFeature} is processed correctly even if some of its values do not occur in the
     * training set, which results in an incomplete Weka schema.
     * </p>
     * 
     * @throws FileNotFoundException
     */
    @Test
    public void testNominalFeatureWithMissingValueInValidationSet() throws FileNotFoundException {
        File pathToTrainSet = getResourceFile("/wekadatasets/train_sample.csv");
        File pathToValidationSet = getResourceFile("/wekadatasets/validation_sample.csv");
        List<Instance> trainSet = new CsvDatasetReader(pathToTrainSet).readAll();
        List<Instance> validationSet = new CsvDatasetReader(pathToValidationSet).readAll();

        WekaModel model = new WekaLearner(new Bagging()).train(trainSet);
        ConfusionMatrix evaluation = ClassifierEvaluation.evaluate(new WekaClassifier(), validationSet, model);
        assertThat(evaluation.getF(1.0, "false"), is(greaterThan(0.0)));
        assertThat(evaluation.getAccuracy(), is(greaterThan(0.0)));
    }

    @Test
    public void testWithAdultIncomeData() throws FileNotFoundException {
        List<Instance> instances = new CsvDatasetReader(getResourceFile("/adultData.txt"), false).readAll();
        WekaLearner learner = new WekaLearner(new Bagging());
        ConfusionMatrix confusionMatrix = evaluate(learner, new WekaClassifier(), instances);
        // System.out.println(confusionMatrix.getAccuracy());
        assertTrue(confusionMatrix.getAccuracy() > 0.78);
    }

    @Test
    public void testWithDiabetesData() throws FileNotFoundException {
        List<Instance> instances = new CsvDatasetReader(getResourceFile("/diabetesData.txt"), false).readAll();
        WekaLearner learner = new WekaLearner(new Bagging());
        ConfusionMatrix confusionMatrix = evaluate(learner, new WekaClassifier(), instances);
        // System.out.println(confusionMatrix.getAccuracy());
        assertTrue(confusionMatrix.getAccuracy() > 0.77);
    }

}
