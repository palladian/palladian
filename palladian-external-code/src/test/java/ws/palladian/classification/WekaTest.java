/**
 * Created on: 24.11.2012 16:12:03
 */
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
import java.util.List;

import org.junit.Test;

import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.meta.Bagging;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.FeatureVectorBuilder;
import ws.palladian.core.Instance;
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

        FeatureVectorBuilder builder = new FeatureVectorBuilder();
        Instance instance1 = builder.set("a", 2.3).set("b", "value1").set("v1", true).create("c1");

        builder = new FeatureVectorBuilder();
        Instance instance = builder.set("a", 1.1).set("b", "value2").set("v1", true).set("v2", true).create("c2");

        List<Instance> trainingInstances = CollectionHelper.newArrayList();
        trainingInstances.add(instance1);
        trainingInstances.add(instance);
        WekaModel model = new WekaLearner(new NaiveBayes()).train(trainingInstances);

        assertEquals(2, model.getCategories().size());
        assertTrue(model.getCategories().contains("c1"));
        assertTrue(model.getCategories().contains("c2"));

        builder = new FeatureVectorBuilder();
        FeatureVector testVector = builder.set("a", 1.5).set("b", "value2").set("v1", true).set("v2", true).create();
        CategoryEntries result = new WekaClassifier().classify(testVector, model);

        assertThat(result.getMostLikelyCategory(), isOneOf("c1", "c2"));
    }

//    @Test
//    public void testWithPositionalData() {
//        // Feature Vector 1
//        PositionAnnotation annotation1 = new PositionAnnotation("abc", 0);
//        PositionAnnotation annotation2 = new PositionAnnotation("de", 4);
//        ListFeature<PositionAnnotation> annotationListFeature = new ListFeature<PositionAnnotation>("token");
//        annotationListFeature.add(annotation1);
//        annotationListFeature.add(annotation2);
//
//        ListFeature<SequentialPattern> pattern1ListFeature1 = new ListFeature<SequentialPattern>("tokenabcpattern");
//
//        pattern1ListFeature1.add(new SequentialPattern(Arrays.asList(new String[] {"a"})));
//        pattern1ListFeature1.add(new SequentialPattern(Arrays.asList(new String[] {"b"})));
//
//        ListFeature<SequentialPattern> pattern2ListFeature1 = new ListFeature<SequentialPattern>("tokendepattern");
//        pattern2ListFeature1.add(new SequentialPattern(Arrays.asList(new String[] {"d"})));
//
//        FeatureVector featureVector1 = new BasicFeatureVector();
//        featureVector1.add(annotationListFeature);
//        featureVector1.add(pattern1ListFeature1);
//        featureVector1.add(pattern2ListFeature1);
//
//        // Feature Vector 2
//        ListFeature<PositionAnnotation> annotationListFeature2 = new ListFeature<PositionAnnotation>("token");
//        annotationListFeature2.add(new PositionAnnotation("de", 0));
//        ListFeature<SequentialPattern> pattern1ListFeature2 = new ListFeature<SequentialPattern>("tokendepattern");
//        pattern1ListFeature2.add(new SequentialPattern(Arrays.asList(new String[] {"d"})));
//
//        FeatureVector featureVector2 = new BasicFeatureVector();
//        featureVector2.add(annotationListFeature2);
//        featureVector2.add(pattern1ListFeature2);
//
//        List<Instance> instances = new ArrayList<Instance>();
//        Instance instance1 = new Instance("c1", featureVector1);
//        Instance instance2 = new Instance("c2", featureVector2);
//        instances.add(instance1);
//        instances.add(instance2);
//        WekaModel model = new WekaLearner(new NaiveBayes()).train(instances);
//
//        CategoryEntries result = new WekaClassifier().classify(instance1.getFeatureVector(), model);
//
//        assertThat(result.getMostLikelyCategory(), is("c1"));
//    }

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
        assertTrue(confusionMatrix.getAccuracy() > 0.79);
    }

    @Test
    public void testWithDiabetesData() throws FileNotFoundException {
        List<Instance> instances = new CsvDatasetReader(getResourceFile("/diabetesData.txt"), false).readAll();
        WekaLearner learner = new WekaLearner(new Bagging());
        ConfusionMatrix confusionMatrix = evaluate(learner, new WekaClassifier(), instances);
        assertTrue(confusionMatrix.getAccuracy() > 0.78);
    }
}
