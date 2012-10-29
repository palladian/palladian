/**
 * Created on: 17.10.2012 19:46:44
 */
package ws.palladian.classification;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

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

        List<Instance> instances = ClassificationUtils.createInstances(
                ResourceHelper.getResourcePath("/classifier/saheart.csv"), true, ",");

        List<Instance> trainingSet = ClassificationUtils.drawRandomSubset(instances, 60);
        instances.removeAll(trainingSet);
        
        UniversalClassifier objectOfClassUnderTest = new UniversalClassifier();
        UniversalClassifierModel model = objectOfClassUnderTest.train(trainingSet);
        
        for (int i = 0; i < 3; i++) {

            int truePositives = 0;
            int trueNegatives = 0;
            int falsePositives = 0;
            int falseNegatives = 0;

            for (Instance testInstance : instances) {
                CategoryEntries result = objectOfClassUnderTest.classify(testInstance.getFeatureVector(), model);
                if (result.getMostLikelyCategoryEntry().getName().equals("1")
                        && testInstance.getTargetClass().equals("1")) {
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

            System.out.println("TP:" + truePositives);
            System.out.println("TN:" + trueNegatives);
            System.out.println("FP:" + falsePositives);
            System.out.println("FN:" + falseNegatives);
            System.out.println("total:" + instances.size());
            System.out.println("correct:" + (truePositives + trueNegatives));
            System.out.println(model);

        }
    }
}
