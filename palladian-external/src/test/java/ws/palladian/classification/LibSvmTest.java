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
import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.Instance;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.helper.math.ConfusionMatrix;

/**
 * <p>
 * Tests whether the Palladian wrapper for the Libsvm classifier works correctly or not.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 2.0
 * @since 0.2.0
 */
public class LibSvmTest {

    @Test
    @Ignore
    public void test() {
        List<Instance> instances = new ArrayList<Instance>();
        Instance instance1 = new InstanceBuilder().set("a", "a").set("b", 0.9).create("A");
        Instance instance2 = new InstanceBuilder().set("a", "b").set("b", 0.1).create("B");
        instances.add(instance1);
        instances.add(instance2);

        LibSvmLearner learner = new LibSvmLearner(new LinearKernel(1.0d));
        LibSvmModel model = learner.train(instances);
        assertThat(model, Matchers.is(Matchers.notNullValue()));
        assertEquals(2, model.getCategories().size());
        assertTrue(model.getCategories().contains("A"));
        assertTrue(model.getCategories().contains("B"));

        FeatureVector classificationVector = new InstanceBuilder().set("a", "a").set("b", 0.8).create();
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
            InstanceBuilder featureVectorBuilder = new InstanceBuilder();
            for (int i = 1; i < elements.length; i++) {
                String[] element = elements[i].split(":");
                String name = element[0];
                normalFeaturePathsSet.add(name);
                featureVectorBuilder.set(name, Double.parseDouble(element[1]));
            }
            ret.add(featureVectorBuilder.create(targetClass));
        }
        return ret;
    }

    @Test
    public void testWithAdultIncomeData() throws FileNotFoundException {
        List<Instance> instances = new CsvDatasetReader(getResourceFile("/adultData.txt"), false).readAll();
        LibSvmLearner learner = new LibSvmLearner(new RBFKernel(1., 1.));
        ConfusionMatrix confusionMatrix = evaluate(learner, new LibSvmClassifier(), instances);
        assertTrue(confusionMatrix.getAccuracy() > 0.81);
    }

    @Test
    public void testWithDiabetesData() throws FileNotFoundException {
        List<Instance> instances = new CsvDatasetReader(getResourceFile("/diabetesData.txt"), false).readAll();
        LibSvmLearner learner = new LibSvmLearner(new RBFKernel(1., 1.));
        ConfusionMatrix confusionMatrix = evaluate(learner, new LibSvmClassifier(), instances);
        assertTrue(confusionMatrix.getAccuracy() > 0.79);
    }

}
