/**
 * Created on: 24.11.2012 16:12:03
 */
package ws.palladian.classification;

import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;

import weka.classifiers.bayes.NaiveBayes;
import ws.palladian.extraction.patterns.SequentialPattern;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.PositionAnnotation;

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

        assertThat(result.getMostLikelyCategory(), Matchers.isOneOf("c1", "c2"));
    }

    @Test
    public void testWithPositionalData() {
        PositionAnnotation annotation1 = new PositionAnnotation("token", 0, 3, "abc");
        PositionAnnotation annotation2 = new PositionAnnotation("token", 4, 6, "de");
        annotation1.getFeatureVector().add(new SequentialPattern("pattern", Arrays.asList(new String[] {"a"})));
        annotation1.getFeatureVector().add(new SequentialPattern("pattern", Arrays.asList(new String[] {"b"})));

        annotation2.getFeatureVector().add(new SequentialPattern("pattern", Arrays.asList(new String[] {"d"})));
        FeatureVector featureVector1 = new FeatureVector();
        featureVector1.add(annotation1);
        featureVector1.add(annotation2);

        PositionAnnotation annotation3 = new PositionAnnotation("token", 0, 2, "de");
        annotation3.getFeatureVector().add(new SequentialPattern("pattern", Arrays.asList(new String[] {"d"})));
        FeatureVector featureVector2 = new FeatureVector();
        featureVector2.add(annotation3);

        List<String> normalFeatures = new ArrayList<String>();
        List<String> sparseFeatures = new ArrayList<String>();
        sparseFeatures.add("token/pattern");

        WekaPredictor objectOfClassUnderTest = new WekaPredictor(new NaiveBayes(), normalFeatures, sparseFeatures);

        List<Instance> instances = new ArrayList<Instance>();
        Instance instance1 = new Instance("c1", featureVector1);
        Instance instance2 = new Instance("c2", featureVector2);
        instances.add(instance1);
        instances.add(instance2);
        WekaModel model = objectOfClassUnderTest.train(instances);

        CategoryEntries result = objectOfClassUnderTest.classify(instance1.getFeatureVector(), model);

        assertThat(result.getMostLikelyCategory(), Matchers.is("c1"));
    }
}