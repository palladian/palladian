package ws.palladian.classification.liblinear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ws.palladian.classification.utils.ClassificationUtils.readCsv;
import static ws.palladian.classification.utils.ClassifierEvaluation.evaluate;
import static ws.palladian.helper.io.ResourceHelper.getResourcePath;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.InstanceBuilder;
import ws.palladian.helper.collection.CollectionHelper;
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
        LibLinearModel model = new LibLinearLearner().train(data);
        // System.out.println(model);

        LibLinearClassifier classifier = new LibLinearClassifier();
        // assertEquals("1", liblinear.classify(data.get(0), model).getMostLikelyCategory());
        // assertEquals("2", liblinear.classify(data.get(1), model).getMostLikelyCategory());
        assertEquals("1", classifier.classify(data.get(2), model).getMostLikelyCategory());
        // assertEquals("2", liblinear.classify(data.get(3), model).getMostLikelyCategory());
        assertEquals("3", classifier.classify(data.get(4), model).getMostLikelyCategory());
    }

    @Test
    public void testWithAdultIncomeData() throws FileNotFoundException {
        List<Trainable> instances = readCsv(getResourcePath("/adultData.txt"), false);
        ConfusionMatrix confusionMatrix = evaluate(new LibLinearLearner(), new LibLinearClassifier(), instances);
        assertTrue(confusionMatrix.getAccuracy() > 0.79);
    }

    @Test
    public void testWithDiabetesData() throws FileNotFoundException {
        List<Trainable> instances = readCsv(getResourcePath("/diabetesData.txt"), true);
        ConfusionMatrix confusionMatrix = evaluate(new LibLinearLearner(), new LibLinearClassifier(), instances);
        assertTrue(confusionMatrix.getAccuracy() > 0.80);
    }

}
