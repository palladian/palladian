package ws.palladian.classification.liblinear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ws.palladian.classification.utils.ClassificationUtils.readCsv;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.SolverType;
import ws.palladian.classification.InstanceBuilder;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.RegexFilter;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.processing.Trainable;

public class LibLinearTest {

    @Test
    public void testLiblinear() {
        // sample data taken from de.bwaldvogel.liblinear.Problem
        List<Trainable> data = CollectionHelper.newArrayList();
        data.add(new InstanceBuilder().set("a", 0).set("b", 0.1).set("c", 0.2).set("d", 0).set("e", 0).create("1"));
        data.add(new InstanceBuilder().set("a", 0).set("b", 0.1).set("c", 0.3).set("d", -1.2).set("e", 0).create("2"));
        data.add(new InstanceBuilder().set("a", 0.4).set("b", 0).set("c", 0).set("d", 0).set("e", 0).create("1"));
        data.add(new InstanceBuilder().set("a", 0).set("b", 0.1).set("c", 0).set("d", 1.4).set("e", 0.5).create("2"));
        data.add(new InstanceBuilder().set("a", -0.1).set("b", -0.2).set("c", 0.1).set("d", 1.1).set("e", 0.1)
                .create("3"));
        LibLinear liblinear = new LibLinear();
        LibLinearModel model = liblinear.train(data);
        // System.out.println(model);

        // assertEquals("1", liblinear.classify(data.get(0), model).getMostLikelyCategory());
        // assertEquals("2", liblinear.classify(data.get(1), model).getMostLikelyCategory());
        assertEquals("1", liblinear.classify(data.get(2), model).getMostLikelyCategory());
        // assertEquals("2", liblinear.classify(data.get(3), model).getMostLikelyCategory());
        assertEquals("3", liblinear.classify(data.get(4), model).getMostLikelyCategory());
    }

    @Test
    public void testWithDataset() throws FileNotFoundException {
        List<Trainable> instances = readCsv(ResourceHelper.getResourcePath("/adultData.txt"), false);
        ConfusionMatrix confusionMatrix = testWithSplit(instances);
        assertTrue(confusionMatrix.getAccuracy() > 0.779);
    }

    @Test
    @Ignore
    public void testWithMyDataset() throws FileNotFoundException {
        List<Trainable> instances = readCsv("/Users/pk/Code/newsseecr/newsseecr/trainingData.csv", true);
        // instances = ClassificationUtils.filterFeatures(instances, new RegexFilter("content|normalizedHausdorff|normalizedMidpoint|geoJaccard|hierarchicalGeoJaccard"));
        instances = ClassificationUtils.filterFeatures(instances, new RegexFilter("content"));
        ConfusionMatrix confusionMatrix = testWithSplit(instances);
        assertTrue(confusionMatrix.getAccuracy() > 0.998);
        assertTrue(confusionMatrix.getF(1., "true") > 0.766);
        System.out.println(confusionMatrix);
    }

    private ConfusionMatrix testWithSplit(List<Trainable> instances) {
        List<Trainable> train = instances.subList(0, instances.size() / 2);
        List<Trainable> test = instances.subList(instances.size() / 2, instances.size() - 1);

        LibLinear classifier = new LibLinear(new Parameter(SolverType.L2R_LR, 1.0, 0.01), 1);
        LibLinearModel model = classifier.train(train);

        return ClassifierEvaluation.evaluate(classifier, test, model);
    }

}
