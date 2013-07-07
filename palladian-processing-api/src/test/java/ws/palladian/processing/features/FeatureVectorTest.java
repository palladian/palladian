package ws.palladian.processing.features;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class FeatureVectorTest {

    @Test
    public void testGetFeature() {
        FeatureVector featureVector = new FeatureVector();
        featureVector.add(new NominalFeature("testFeature", "test"));

        NominalFeature retrievedFeature = featureVector.get(NominalFeature.class, "testFeature");
        assertEquals(NominalFeature.class, retrievedFeature.getClass());
        assertEquals("test", retrievedFeature.getValue());
    }

    @Test
    public void testGetFeaturesByType() {
        FeatureVector featureVector = new FeatureVector();
        NominalFeature f1 = new NominalFeature("nominalFeature1", "test");
        NominalFeature f2 = new NominalFeature("nominalFeature3", "test");
        NumericFeature f3 = new NumericFeature("numericFeature1", 2.);
        NumericFeature f4 = new NumericFeature("numericFeature2", 3.);
        featureVector.add(f1);
        featureVector.add(f2);
        featureVector.add(f3);
        featureVector.add(f4);

        assertEquals(4, featureVector.size());

        List<NominalFeature> nominalFeatures = featureVector.getAll(NominalFeature.class);
        assertEquals(2, nominalFeatures.size());
        assertTrue(nominalFeatures.contains(f1));
        assertTrue(nominalFeatures.contains(f2));

        List<NumericFeature> numericFeatures = featureVector.getAll(NumericFeature.class);
        assertEquals(2, numericFeatures.size());
        assertTrue(numericFeatures.contains(f3));
        assertTrue(numericFeatures.contains(f4));

        // try to retrieve a NumericFeature, which is actually a NominalFeature
        assertNull(featureVector.get(NominalFeature.class, "numericFeature1"));
    }

    @Test
    public void testCopyFeatureVector() {
        FeatureVector original = new FeatureVector();
        NominalFeature f1 = new NominalFeature("nominalFeature", "test");
        NumericFeature f2 = new NumericFeature("numericFeature", 7);
        NumericFeature f3 = new NumericFeature("numericFeature2", 8);
        original.add(f1);
        original.add(f2);
        original.add(f3);

        FeatureVector newFeatureVector = new FeatureVector(original);
        List<NominalFeature> nominalFeatures = newFeatureVector.getAll(NominalFeature.class);
        List<NumericFeature> numericFeatures = newFeatureVector.getAll(NumericFeature.class);
        assertEquals(3, newFeatureVector.size());
        assertEquals(1, nominalFeatures.size());
        assertEquals(2, numericFeatures.size());
        assertTrue(nominalFeatures.contains(f1));
        assertTrue(numericFeatures.contains(f2));
        assertTrue(numericFeatures.contains(f3));
    }

    @Test
    public void testEquality() {
        FeatureVector featureVector1 = new FeatureVector();
        FeatureVector featureVector2 = new FeatureVector();
        featureVector1.add(new NominalFeature("nominalFeature1", "test"));
        featureVector1.add(new NominalFeature("nominalFeature3", "test"));
        featureVector1.add(new NumericFeature("numericFeature1", 2.));
        featureVector1.add(new NumericFeature("numericFeature2", 3.));
        featureVector2.add(new NominalFeature("nominalFeature1", "test"));
        featureVector2.add(new NominalFeature("nominalFeature3", "test"));
        featureVector2.add(new NumericFeature("numericFeature1", 2.));
        featureVector2.add(new NumericFeature("numericFeature2", 3.));
        assertTrue(featureVector1.equals(featureVector2));
    }

}
