package ws.palladian.classification.liblinear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ws.palladian.classification.utils.ClassifierEvaluation.evaluate;
import static ws.palladian.helper.io.ResourceHelper.getResourceFile;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.SolverType;
import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.classification.utils.NoNormalizer;
import ws.palladian.classification.utils.ZScoreNormalizer;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.Instance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.ConfusionMatrix;

public class LibLinearTest {

    @Test
    public void testLiblinear() {
        // sample data taken from de.bwaldvogel.liblinear.Problem
        List<Instance> data = createSampleData();
        LibLinearModel model = new LibLinearLearner().train(data);
        // System.out.println(model);

        LibLinearClassifier classifier = new LibLinearClassifier();
        // assertEquals("1", liblinear.classify(data.get(0), model).getMostLikelyCategory());
        // assertEquals("2", liblinear.classify(data.get(1), model).getMostLikelyCategory());
        assertEquals("1", classifier.classify(data.get(2).getVector(), model).getMostLikelyCategory());
        // assertEquals("2", liblinear.classify(data.get(3), model).getMostLikelyCategory());
        assertEquals("3", classifier.classify(data.get(4).getVector(), model).getMostLikelyCategory());
    }
    
    @Test
    public void testLiblinearNonProbabilistic() {
        List<Instance> data = createSampleData();
        Parameter parameter = new Parameter(SolverType.L1R_L2LOSS_SVC, 1.0, 0.01);
        LibLinearLearner learner = new LibLinearLearner(parameter, 1., new ZScoreNormalizer());
        LibLinearModel model = learner.train(data);
        CategoryEntries result = new LibLinearClassifier().classify(data.get(2).getVector(), model);
        assertEquals("1", result.getMostLikelyCategory());
        assertEquals(1., result.getProbability("1"), 0.);
    }

    private List<Instance> createSampleData() {
        List<Instance> data = CollectionHelper.newArrayList();
        data.add(new InstanceBuilder().set("a", 0).set("b", 0.1).set("c", 0.2).set("d", 0).set("e", 0).create("1"));
        data.add(new InstanceBuilder().set("a", 0).set("b", 0.1).set("c", 0.3).set("d", -1.2).set("e", 0).create("2"));
        data.add(new InstanceBuilder().set("a", 0.4).set("b", 0).set("c", 0).set("d", 0).set("e", 0).create("1"));
        data.add(new InstanceBuilder().set("a", 0).set("b", 0.1).set("c", 0).set("d", 1.4).set("e", 0.5).create("2"));
        data.add(new InstanceBuilder().set("a", -0.1).set("b", -0.2).set("c", 0.1).set("d", 1.1).set("e", 0.1)
                .create("3"));
        return data;
    }

    @Test
    public void testUntrainedFeature() {
        LibLinearModel model = new LibLinearLearner().train(createSampleData());
        FeatureVector featureVector = new InstanceBuilder().set("a", 0.4).set("b", 0).set("c", 0).set("e", 0)
                .set("f", 0).create();
        assertEquals("1", new LibLinearClassifier().classify(featureVector, model).getMostLikelyCategory());
    }

    @Test
    public void testWithAdultIncomeData() throws FileNotFoundException {
        List<Instance> instances = new CsvDatasetReader(getResourceFile("/adultData.txt"), false).readAll();
        ConfusionMatrix confusionMatrix = evaluate(new LibLinearLearner(), new LibLinearClassifier(), instances);
        assertTrue(confusionMatrix.getAccuracy() > 0.82);
    }

    @Test
    public void testWithDiabetesData() throws FileNotFoundException {
        List<Instance> instances = new CsvDatasetReader(getResourceFile("/diabetesData.txt"), false).readAll();
        ConfusionMatrix confusionMatrix = evaluate(new LibLinearLearner(), new LibLinearClassifier(), instances);
        assertTrue(confusionMatrix.getAccuracy() > 0.80);
    }
    
    /**
     * We do not support LibLinear's support vector regression, an exception must be thrown in case one tries to use it.
     * (the reason is, that the current Learner/Classifier/Instance API is not intended for regression. In the future,
     * we might add necessary extensions, and this exception can be removed.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedOperation() {
        Parameter parameter = new Parameter(SolverType.L2R_L2LOSS_SVR, 1.0, 0.01);
        new LibLinearLearner(parameter, 1., new NoNormalizer());
    }

}
