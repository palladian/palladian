/**
 * Created on: 17.10.2012 19:46:44
 */
package ws.palladian.classification.universal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ws.palladian.classification.universal.UniversalClassifier.ClassifierSetting.BAYES;
import static ws.palladian.classification.universal.UniversalClassifier.ClassifierSetting.KNN;
import static ws.palladian.helper.io.ResourceHelper.getResourceFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.universal.UniversalClassifier.ClassifierSetting;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.core.Instance;
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
    // @Ignore // FIXME; see issue #282, #281
    public void test() throws FileNotFoundException {

        File datasetFile = getResourceFile("/classifier/saheart.csv");
        List<Instance> instances = new CsvDatasetReader(datasetFile, true, ",").readAll();

        List<Instance> trainingSet = new ArrayList<Instance>(instances.subList(0, (int)(instances.size() * 0.6)));
        instances.removeAll(trainingSet);

        UniversalClassifier objectOfClassUnderTest = new UniversalClassifier(FeatureSettingBuilder.words().create(),BAYES, KNN);
        UniversalClassifierModel model = objectOfClassUnderTest.train(trainingSet);

        assertEquals(2, model.getCategories().size());
        assertTrue(model.getCategories().contains("0"));
        assertTrue(model.getCategories().contains("1"));

        ConfusionMatrix matrix = ClassifierEvaluation.evaluate(objectOfClassUnderTest, instances, model);
        // Precision: 0.5645161290322581
        // Recall: 0.6140350877192983
        // F1: 0.5882352941176471
        System.out.println(matrix);
        assertTrue(matrix.getPrecision("1") > 0.57);
        assertTrue(matrix.getRecall("1") > 0.56);
        assertTrue(matrix.getF(1.0, "1") > 0.56);

    }
}
