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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.meta.Bagging;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.core.CategoryEntries;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.BasicFeatureVector;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.SequentialPattern;
import ws.palladian.processing.features.SparseFeature;

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
        List<Instance> trainingInstances = new ArrayList<Instance>();
        FeatureVector v1 = new BasicFeatureVector();
        v1.add(new NumericFeature("a", 2.3));
        v1.add(new NominalFeature("b", "value1"));
        List<SparseFeature<String>> v1ListFeatureList = new ArrayList<SparseFeature<String>>();
        v1ListFeatureList.add(new SparseFeature<String>("v1"));
        ListFeature<SparseFeature<String>> v1ListFeature = new ListFeature<SparseFeature<String>>("c",
                v1ListFeatureList);
        v1.add(v1ListFeature);

        Instance trainingInstance1 = new Instance("c1", v1);

        FeatureVector v2 = new BasicFeatureVector();
        v2.add(new NumericFeature("a", 1.1));
        v2.add(new NominalFeature("b", "value2"));
        ListFeature<SparseFeature<String>> v2ListFeature = new ListFeature<SparseFeature<String>>("c");
        v2ListFeature.add(new SparseFeature<String>("v1"));
        v2ListFeature.add(new SparseFeature<String>("v2"));
        v2.add(v2ListFeature);

        Instance trainingInstance2 = new Instance("c2", v2);

        trainingInstances.add(trainingInstance1);
        trainingInstances.add(trainingInstance2);
        WekaModel model = new WekaLearner(new NaiveBayes()).train(trainingInstances);

        assertEquals(2, model.getCategories().size());
        assertTrue(model.getCategories().contains("c1"));
        assertTrue(model.getCategories().contains("c2"));

        FeatureVector testVector = new BasicFeatureVector();
        testVector.add(new NumericFeature("a", 1.5));
        testVector.add(new NominalFeature("b", "value2"));
        ListFeature<SparseFeature<String>> testListFeature = new ListFeature<SparseFeature<String>>("c");
        testListFeature.add(new SparseFeature<String>("v1"));
        testListFeature.add(new SparseFeature<String>("v2"));
        testVector.add(testListFeature);
        CategoryEntries result = new WekaClassifier().classify(testVector, model);

        assertThat(result.getMostLikelyCategory(), isOneOf("c1", "c2"));
    }

    @Test
    public void testWithPositionalData() {
        // Feature Vector 1
        PositionAnnotation annotation1 = new PositionAnnotation("abc", 0);
        PositionAnnotation annotation2 = new PositionAnnotation("de", 4);
        ListFeature<PositionAnnotation> annotationListFeature = new ListFeature<PositionAnnotation>("token");
        annotationListFeature.add(annotation1);
        annotationListFeature.add(annotation2);

        ListFeature<SequentialPattern> pattern1ListFeature1 = new ListFeature<SequentialPattern>("tokenabcpattern");

        pattern1ListFeature1.add(new SequentialPattern(Arrays.asList(new String[] {"a"})));
        pattern1ListFeature1.add(new SequentialPattern(Arrays.asList(new String[] {"b"})));

        ListFeature<SequentialPattern> pattern2ListFeature1 = new ListFeature<SequentialPattern>("tokendepattern");
        pattern2ListFeature1.add(new SequentialPattern(Arrays.asList(new String[] {"d"})));

        FeatureVector featureVector1 = new BasicFeatureVector();
        featureVector1.add(annotationListFeature);
        featureVector1.add(pattern1ListFeature1);
        featureVector1.add(pattern2ListFeature1);

        // Feature Vector 2
        ListFeature<PositionAnnotation> annotationListFeature2 = new ListFeature<PositionAnnotation>("token");
        annotationListFeature2.add(new PositionAnnotation("de", 0));
        ListFeature<SequentialPattern> pattern1ListFeature2 = new ListFeature<SequentialPattern>("tokendepattern");
        pattern1ListFeature2.add(new SequentialPattern(Arrays.asList(new String[] {"d"})));

        FeatureVector featureVector2 = new BasicFeatureVector();
        featureVector2.add(annotationListFeature2);
        featureVector2.add(pattern1ListFeature2);

        List<Instance> instances = new ArrayList<Instance>();
        Instance instance1 = new Instance("c1", featureVector1);
        Instance instance2 = new Instance("c2", featureVector2);
        instances.add(instance1);
        instances.add(instance2);
        WekaModel model = new WekaLearner(new NaiveBayes()).train(instances);

        CategoryEntries result = new WekaClassifier().classify(instance1.getFeatureVector(), model);

        assertThat(result.getMostLikelyCategory(), is("c1"));
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
        List<Trainable> trainSet = new CsvDatasetReader(pathToTrainSet).readAll();
        List<Trainable> validationSet = new CsvDatasetReader(pathToValidationSet).readAll();

        WekaModel model = new WekaLearner(new Bagging()).train(trainSet);
        ConfusionMatrix evaluation = ClassifierEvaluation.evaluate(new WekaClassifier(), validationSet, model);
        assertThat(evaluation.getF(1.0, "false"), is(greaterThan(0.0)));
        assertThat(evaluation.getAccuracy(), is(greaterThan(0.0)));
    }

    @Test
    public void testWithAdultIncomeData() throws FileNotFoundException {
        List<Trainable> instances = new CsvDatasetReader(getResourceFile("/adultData.txt"), false).readAll();
        WekaLearner learner = new WekaLearner(new Bagging());
        ConfusionMatrix confusionMatrix = evaluate(learner, new WekaClassifier(), instances);
        assertTrue(confusionMatrix.getAccuracy() > 0.79);
    }

    @Test
    public void testWithDiabetesData() throws FileNotFoundException {
        List<Trainable> instances = new CsvDatasetReader(getResourceFile("/diabetesData.txt"), false).readAll();
        WekaLearner learner = new WekaLearner(new Bagging());
        ConfusionMatrix confusionMatrix = evaluate(learner, new WekaClassifier(), instances);
        assertTrue(confusionMatrix.getAccuracy() > 0.78);
    }
}
