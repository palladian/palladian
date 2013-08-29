package ws.palladian.classification.featureselection;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.nb.NaiveBayesClassifier;
import ws.palladian.classification.nb.NaiveBayesModel;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.processing.Trainable;

/**
 * @author Philipp Katz
 */
public class BackwardFeatureEliminationTest {

    @Test
    public void testElimination() throws FileNotFoundException {
        String testFile = ResourceHelper.getResourcePath("/classifier/diabetes2.csv");
        List<Trainable> instances = ClassificationUtils.readCsv(testFile, true);

        NaiveBayesClassifier classifier = new NaiveBayesClassifier();
        BackwardFeatureElimination<NaiveBayesModel> elimination = new BackwardFeatureElimination<NaiveBayesModel>(
                classifier, classifier);
        FeatureRanking ranking = elimination.rankFeatures(instances);
        String bestFeatureValue = ranking.getAll().get(0).getValue();

        // this is not really a good test, as the BackwardFeatureElimination shuffles the dataset; the top ranked
        // features are as below, but I cannot exclude the case, the in one of 349834983 cases this might fail.
        assertTrue(Arrays.asList("plasma", "bmi", "bloodPressure", "triceps", "pedigree").contains(bestFeatureValue));
    }

}
