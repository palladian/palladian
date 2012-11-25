/**
 * Created on: 24.11.2012 16:12:03
 */
package ws.palladian.classification;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import weka.classifiers.bayes.NaiveBayes;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since
 */
public class WekaPredictorTest {

    @Test
    public void test() {
        List<String> normalFeatures = new ArrayList<String>();
        normalFeatures.add("a");
        normalFeatures.add("b");
        List<String> sparseFeatures = new ArrayList<String>();
        sparseFeatures.add("c");

        WekaPredictor objectOfClassUnderTest = new WekaPredictor(new NaiveBayes(), normalFeatures, sparseFeatures);

        List<Instance> trainingInstances = new ArrayList<Instance>();
        FeatureVector v1 = new FeatureVector();
        v1.add(new NumericFeature("a", 2.3));
        v1.add(new NominalFeature("b", "value1"));
        v1.add(new NominalFeature("c", "v1"));
        Instance trainingInstance1 = new Instance("c1", v1);
        FeatureVector v2 = new FeatureVector();
        v2.add(new NumericFeature("a", 1.1));
        v2.add(new NominalFeature("b", "value2"));
        v2.add(new NominalFeature("c", "v1"));
        v2.add(new NominalFeature("c", "v2"));
        Instance trainingInstance2 = new Instance("c2", v2);
        trainingInstances.add(trainingInstance1);
        trainingInstances.add(trainingInstance2);
        WekaModel model = objectOfClassUnderTest.train(trainingInstances);

        FeatureVector testVector = new FeatureVector();
        testVector.add(new NumericFeature("a", 1.5));
        testVector.add(new NominalFeature("b", "value2"));
        testVector.add(new NominalFeature("c", "v1"));
        testVector.add(new NominalFeature("c", "v2"));
        CategoryEntries result = objectOfClassUnderTest.classify(testVector, model);

        Assert.assertThat(result.getMostLikelyCategoryEntry().getName(), Matchers.isOneOf("c1", "c2"));
    }
}
