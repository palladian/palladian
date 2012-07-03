package ws.palladian.processing.features;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.FeatureDescriptorBuilder;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

public class FeatureVectorTest {

    private FeatureVector featureVector;
    private NominalFeature f1;
    private Feature<String> f2;
    private NumericFeature f3;
    private Feature<Double> f4;

    @Before
    public void setUp() {
        featureVector = new FeatureVector();
        f1 = new NominalFeature("nominalFeature1", "test", "value1", "value2");
        f2 = new Feature<String>("nominalFeature3", "test");
        f3 = new NumericFeature("numericFeature1", 2.);
        f4 = new Feature<Double>("numericFeature2", 3.);
        featureVector.add(f1);
        featureVector.add(f2);
        featureVector.add(f3);
        featureVector.add(f4);
    }

    @Test
    public void testRetrieveFeaturesByDescriptor() {
        FeatureVector featureVector = new FeatureVector();
        FeatureDescriptor<NominalFeature> featureDescriptor = FeatureDescriptorBuilder.build("myNominalFeature",
                NominalFeature.class);

        NominalFeature nominalFeature = new NominalFeature(featureDescriptor, "test");
        featureVector.add(nominalFeature);

        NominalFeature retrievedFeature = featureVector.get(featureDescriptor);

        assertEquals("test", retrievedFeature.getValue());
        assertEquals(NominalFeature.class, retrievedFeature.getClass());
    }

    @Test
    public void testRetrieveFeaturesByType() {
        assertEquals(4, featureVector.size());
        List<Feature<String>> stringFeatures = featureVector.getAll(String.class);
        assertEquals(2, stringFeatures.size());
        assertTrue(stringFeatures.contains(f1));
        assertTrue(stringFeatures.contains(f2));
        List<Feature<Number>> numericFeatures = featureVector.getAll(Number.class);
        assertEquals(2, numericFeatures.size());
        assertTrue(numericFeatures.contains(f3));
        assertTrue(numericFeatures.contains(f4));
    }

    @Test
    public void testCopyFeatureVector() {
        FeatureVector newFeatureVector = new FeatureVector(featureVector);
        assertEquals(4, newFeatureVector.size());
    }

}
