/**
 * Created on: 17.10.2012 19:46:44
 */
package ws.palladian.classification.universal;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.Instance;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.helper.math.ConfusionMatrix;

/**
 * <p>
 * Tests whether the {@link UniversalClassifier} works correctly on some arbitrary data.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public class UniversalClassifierTest {

    /**
     * <p>
     * Tests the functionality not the quality of the classification result.
     * </p>
     * 
     * @throws FileNotFoundException If the example dataset was not found.
     */
    @Test
    public void test() throws FileNotFoundException {

        List<Instance> instances = ClassificationUtils.createInstances(
                ResourceHelper.getResourcePath("/classifier/saheart.csv"), true, ",");

        List<Instance> trainingSet = ClassificationUtils.drawRandomSubset(instances, 60);
        instances.removeAll(trainingSet);

        UniversalClassifier objectOfClassUnderTest = new UniversalClassifier();
        UniversalClassifierModel model = objectOfClassUnderTest.train(trainingSet);

        ConfusionMatrix matrix = ClassifierEvaluation.evaluate(objectOfClassUnderTest, model, instances);
        System.out.println("Precision: " + matrix.getPrecision("1"));
        System.out.println("Recall: " + matrix.getRecall("1"));
        System.out.println("F1: " + matrix.getF("1", 1.0));
    }
}
