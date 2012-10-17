/**
 * Created on: 17.10.2012 19:46:44
 */
package ws.palladian.classification;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.universal.AbstractWeightingStrategy;
import ws.palladian.classification.universal.InstanceWeightingStrategy;
import ws.palladian.classification.universal.UniversalClassifier;
import ws.palladian.classification.universal.UniversalClassifierModel;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.helper.io.ResourceHelper;

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
        AbstractWeightingStrategy weightingStrategy = new InstanceWeightingStrategy();
        UniversalClassifier objectOfClassUnderTest = new UniversalClassifier(weightingStrategy);

        List<Instance> instances = ClassificationUtils.createInstances(
                ResourceHelper.getResourcePath("/classifier/saheart.csv"), true, ",");

        List<Instance> trainingSet = ClassificationUtils.drawRandomSubset(instances, 60);
        List<Instance> testSet = instances;
        testSet.removeAll(trainingSet);
        UniversalClassifierModel model = objectOfClassUnderTest.train(instances);

        int truePositives = 0;
        int trueNegatives = 0;
        int falsePositives = 0;
        int falseNegatives = 0;

        for (Instance testInstance : testSet) {
            CategoryEntries result = objectOfClassUnderTest.classify(testInstance.getFeatureVector(), model);
            if (result.getMostLikelyCategoryEntry().getName().equals("1") && testInstance.getTargetClass().equals("1")) {
                truePositives++;
            } else if (result.getMostLikelyCategoryEntry().getName().equals("0")
                    && testInstance.getTargetClass().equals("0")) {
                trueNegatives++;
            } else if (result.getMostLikelyCategoryEntry().getName().equals("1")
                    && testInstance.getTargetClass().equals("0")) {
                falsePositives++;
            } else if (result.getMostLikelyCategoryEntry().getName().equals("0")
                    && testInstance.getTargetClass().equals("1")) {
                falseNegatives++;
            }
        }

        double precision = Double.valueOf(truePositives) / (truePositives + falsePositives);
        System.out.println("Precision: " + precision);
        double recall = Double.valueOf(truePositives) / (truePositives + falseNegatives);
        System.out.println("Recall: " + recall);
        System.out.println("F1: " + Double.valueOf(2 * precision * recall) / (precision + recall));
    }
}
