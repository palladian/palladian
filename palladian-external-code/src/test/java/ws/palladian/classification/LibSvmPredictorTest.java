/**
 * Created on: 18.12.2012 17:51:14
 */
package ws.palladian.classification;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Tests whether the Palladian wrapper for the Libsvm classifier works correctly or not.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.0
 */
public class LibSvmPredictorTest {

    @Test
    public void test() {
        List<Instance> instances = new ArrayList<Instance>();
        FeatureVector featureVector1 = new FeatureVector();
        featureVector1.add(new NominalFeature("a", "a"));
        featureVector1.add(new NumericFeature("b", 0.9));
        FeatureVector featureVector2 = new FeatureVector();
        featureVector2.add(new NominalFeature("a", "b"));
        featureVector2.add(new NumericFeature("b", 0.2));
        Instance instance1 = new Instance("A", featureVector1);
        Instance instance2 = new Instance("B", featureVector2);
        instances.add(instance1);
        instances.add(instance2);

        List<String> normalFeaturePaths = new ArrayList<String>();
        normalFeaturePaths.add("a");
        normalFeaturePaths.add("b");
        List<String> sparseFeaturePaths = new ArrayList<String>();

        LibSvmPredictor predictor = new LibSvmPredictor(normalFeaturePaths, sparseFeaturePaths);
        LibSvmModel model = predictor.train(instances);
        Assert.assertThat(model, Matchers.is(Matchers.notNullValue()));

        FeatureVector classificationVector = new FeatureVector();
        classificationVector.add(new NominalFeature("a", "a"));
        classificationVector.add(new NumericFeature("b", 0.8));
        CategoryEntries result = predictor.classify(classificationVector, model);
        Assert.assertThat(result.getMostLikelyCategoryEntry().getName(), Matchers.is("A"));
    }

}
