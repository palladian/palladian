package ws.palladian.classification.zeror;

import static org.junit.Assert.assertEquals;
import static ws.palladian.classification.utils.ClassifierEvaluation.evaluate;
import static ws.palladian.helper.io.ResourceHelper.getResourceFile;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.processing.Trainable;

public class ZeroRTest {

    @Test
    public void testNaiveBayesWithDiabetesData() throws FileNotFoundException {
        List<Trainable> instances = new CsvDatasetReader(getResourceFile("/classifier/diabetesData.txt"), false)
                .readAll();
        ConfusionMatrix matrix = evaluate(new ZeroRLearner(), new ZeroRClassifier(), instances);
        assertEquals(0.67, matrix.getAccuracy(), 0.01);
        assertEquals(1, matrix.getRecall("class0"), 0);
        assertEquals(0, matrix.getRecall("class1"), 0);
    }

}
