/**
 * Created on: 17.10.2012 19:46:44
 */
package ws.palladian.classification.universal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.processing.Trainable;

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

        List<Trainable> instances = ClassificationUtils.readCsv(
                ResourceHelper.getResourcePath("/classifier/saheart.csv"), true, ",");

        List<Trainable> trainingSet = new ArrayList<Trainable>(instances.subList(0, (int)(instances.size() * 0.6)));
        instances.removeAll(trainingSet);

        UniversalClassifier objectOfClassUnderTest = new UniversalClassifier();
        UniversalClassifierModel model = objectOfClassUnderTest.train(trainingSet);

        assertEquals(2, model.getCategories().size());
        assertTrue(model.getCategories().contains("0"));
        assertTrue(model.getCategories().contains("1"));

        ConfusionMatrix matrix = ClassifierEvaluation.evaluate(objectOfClassUnderTest, instances, model);
        // Precision: 0.5645161290322581
        // Recall: 0.6140350877192983
        // F1: 0.5882352941176471
        assertTrue(matrix.getPrecision("1") > 0.56);
        assertTrue(matrix.getRecall("1") > 0.61);
        assertTrue(matrix.getF(1.0, "1") > 0.58);

    }
}
