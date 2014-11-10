package ws.palladian.classification.featureselection;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.nb.NaiveBayesClassifier;
import ws.palladian.classification.nb.NaiveBayesLearner;
import ws.palladian.classification.nb.NaiveBayesModel;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.core.Instance;
import ws.palladian.helper.NoProgress;
import ws.palladian.helper.io.ResourceHelper;

/**
 * @author Philipp Katz
 */
public class BackwardFeatureEliminationTest {

    @Test
    public void testElimination() throws FileNotFoundException {
        String testFile = ResourceHelper.getResourcePath("/classifier/diabetes2.csv");
        List<Instance> instances = ClassificationUtils.readCsv(testFile, true);

        NaiveBayesLearner learner = new NaiveBayesLearner();
        NaiveBayesClassifier classifier = new NaiveBayesClassifier();
        BackwardFeatureElimination<NaiveBayesModel> elimination = new BackwardFeatureElimination<NaiveBayesModel>(
                learner, classifier);
        FeatureRanking ranking = elimination.rankFeatures(instances, NoProgress.INSTANCE);
        String bestFeatureValue = ranking.getAll().get(0).getName();

        // this is not really a good test, as the BackwardFeatureElimination shuffles the dataset; the top ranked
        // features are as below, but I cannot exclude the case, the in one of 349834983 cases this might fail.
        assertTrue(Arrays.asList("plasma", "bmi", "bloodPressure", "triceps", "pedigree").contains(bestFeatureValue));
    }

}
