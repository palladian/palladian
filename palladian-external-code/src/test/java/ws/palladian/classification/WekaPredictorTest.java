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
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.SequentialPattern;
import ws.palladian.processing.features.SparseFeature;

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
        WekaPredictor objectOfClassUnderTest = new WekaPredictor(new NaiveBayes());

        List<Instance> trainingInstances = new ArrayList<Instance>();
        FeatureVector v1 = new FeatureVector();
        v1.add(new NumericFeature("a", 2.3));
        v1.add(new NominalFeature("b", "value1"));
        List<SparseFeature<String>> v1ListFeatureList = new ArrayList<SparseFeature<String>>();
        v1ListFeatureList.add(new SparseFeature<String>("v1"));
        ListFeature<SparseFeature<String>> v1ListFeature = new ListFeature<SparseFeature<String>>("c",
                v1ListFeatureList);
        v1.add(v1ListFeature);

        Instance trainingInstance1 = new Instance("c1", v1);

        FeatureVector v2 = new FeatureVector();
        v2.add(new NumericFeature("a", 1.1));
        v2.add(new NominalFeature("b", "value2"));
        List<SparseFeature<String>> v2ListFeatureList = new ArrayList<SparseFeature<String>>();
        v2ListFeatureList.add(new SparseFeature<String>("v1"));
        v2ListFeatureList.add(new SparseFeature<String>("v2"));
        ListFeature<SparseFeature<String>> v2ListFeature = new ListFeature<SparseFeature<String>>("c",
                v2ListFeatureList);
        v2.add(v2ListFeature);

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
        PositionAnnotation annotation1 = new PositionAnnotation("abc", 0, 3);
        PositionAnnotation annotation2 = new PositionAnnotation("de", 4, 6);
        ListFeature<PositionAnnotation> annotationListFeature = new ListFeature<PositionAnnotation>("token");
        annotationListFeature.add(annotation1);
        annotationListFeature.add(annotation2);

        ListFeature<SequentialPattern> patternListFeature1 = new ListFeature<SequentialPattern>("annotation1Pattern");

        patternListFeature1.add(new SequentialPattern("pattern", Arrays.asList(new String[] {"a"})));
        patternListFeature1.add(new SequentialPattern("pattern", Arrays.asList(new String[] {"b"})));

        ListFeature<SequentialPattern> patternListFeature2 = new ListFeature<SequentialPattern>("annotation2Pattern");
        patternListFeature2.add(new SequentialPattern("pattern", Arrays.asList(new String[] {"d"})));

        FeatureVector featureVector1 = new FeatureVector();
        featureVector1.add(annotationListFeature);
        featureVector1.add(patternListFeature1);
        featureVector1.add(patternListFeature2);

        PositionAnnotation annotation3 = new PositionAnnotation("token", 0, 2, "de");
        annotation3.getFeatureVector().add(new SequentialPattern("pattern", Arrays.asList(new String[] {"d"})));
        FeatureVector featureVector2 = new FeatureVector();
        featureVector2.add(annotation3);

        WekaPredictor objectOfClassUnderTest = new WekaPredictor(new NaiveBayes());

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