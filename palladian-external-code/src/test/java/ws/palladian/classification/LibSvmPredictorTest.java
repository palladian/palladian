/**
 * Created on: 18.12.2012 17:51:14
 */
package ws.palladian.classification;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ws.palladian.classification.utils.ClassifierEvaluation.evaluate;
import static ws.palladian.helper.io.ResourceHelper.getResourceFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Test;

import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.BasicFeatureVector;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Tests whether the Palladian wrapper for the Libsvm classifier works correctly or not.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 1.0
 * @since 0.2.0
 */
public class LibSvmPredictorTest {

    @Test
    public void test() {
        List<Instance> instances = new ArrayList<Instance>();
        FeatureVector featureVector1 = new BasicFeatureVector();
        featureVector1.add(new NominalFeature("a", "a"));
        featureVector1.add(new NumericFeature("b", 0.9));
        FeatureVector featureVector2 = new BasicFeatureVector();
        featureVector2.add(new NominalFeature("a", "b"));
        featureVector2.add(new NumericFeature("b", 0.1));
        Instance instance1 = new Instance("A", featureVector1);
        Instance instance2 = new Instance("B", featureVector2);
        instances.add(instance1);
        instances.add(instance2);

        LibSvmLearner learner = new LibSvmLearner(new LinearKernel(1.0d));
        LibSvmModel model = learner.train(instances);
        assertThat(model, Matchers.is(Matchers.notNullValue()));
        assertEquals(2, model.getCategories().size());
        assertTrue(model.getCategories().contains("A"));
        assertTrue(model.getCategories().contains("B"));

        FeatureVector classificationVector = new BasicFeatureVector();
        classificationVector.add(new NominalFeature("a", "a"));
        classificationVector.add(new NumericFeature("b", 0.8));
        CategoryEntries result = new LibSvmClassifier().classify(classificationVector, model);
        assertThat(result.getMostLikelyCategory(), Matchers.is("A"));
    }

    /**
     * <p>
     * A test on a dataset from the LibSvm webpage using the same set of parameters. Should achieve a quite high
     * accuracy.
     * </p>
     * 
     * @throws FileNotFoundException If the training data can not be found.
     */
    @Test
    public void testRealDataSet() throws FileNotFoundException {
        List<Instance> instances = readInstances("/train.1");

        LibSvmLearner learner = new LibSvmLearner(new RBFKernel(2.0d, 2.0d));
        LibSvmModel model = learner.train(instances);

        List<Instance> test = readInstances("/test.1");
        ConfusionMatrix confusionMatrix = ClassifierEvaluation.evaluate(new LibSvmClassifier(), test, model);

        assertThat(confusionMatrix.getAverageAccuracy(false), is(greaterThan(0.954)));
        assertThat(confusionMatrix.getAverageRecall(false), is(greaterThan(0.954)));
        assertThat(confusionMatrix.getAveragePrecision(false), is(greaterThan(0.954)));
        assertThat(confusionMatrix.getAverageF(0.5, false), is(greaterThan(0.954)));
    }

    private List<Instance> readInstances(String resource) throws FileNotFoundException {
        File contentFile = ResourceHelper.getResourceFile(resource);
        List<String> lines = FileHelper.readFileToArray(contentFile);
        List<Instance> ret = new ArrayList<Instance>(lines.size());
        Set<String> normalFeaturePathsSet = new HashSet<String>();
        for (String line : lines) {
            String[] elements = line.split("\\s");
            String targetClass = elements[0];
            FeatureVector featureVector = new BasicFeatureVector();
            for (int i = 1; i < elements.length; i++) {
                String[] element = elements[i].split(":");
                String name = element[0];
                normalFeaturePathsSet.add(name);
                Number value = Double.valueOf(element[1]);
                featureVector.add(new NumericFeature(name, value));
            }
            ret.add(new Instance(targetClass, featureVector));
        }
        return ret;
    }

    @Test
    public void testWithAdultIncomeData() throws FileNotFoundException {
        List<Trainable> instances = new CsvDatasetReader(getResourceFile("/adultData.txt"), false).readAll();
        LibSvmLearner learner = new LibSvmLearner(new RBFKernel(1., 1.));
        ConfusionMatrix confusionMatrix = evaluate(learner, new LibSvmClassifier(), instances);
        assertTrue(confusionMatrix.getAccuracy() > 0.78);
    }

    @Test
    public void testWithDiabetesData() throws FileNotFoundException {
        List<Trainable> instances = new CsvDatasetReader(getResourceFile("/diabetesData.txt"), false).readAll();
        LibSvmLearner learner = new LibSvmLearner(new RBFKernel(1., 1.));
        ConfusionMatrix confusionMatrix = evaluate(learner, new LibSvmClassifier(), instances);
        assertTrue(confusionMatrix.getAccuracy() > 0.80);
    }

}
