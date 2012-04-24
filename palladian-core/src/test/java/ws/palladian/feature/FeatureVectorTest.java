package ws.palladian.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import ws.palladian.model.features.Feature;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NominalFeature;
import ws.palladian.model.features.NumericFeature;

public class FeatureVectorTest {

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
        FeatureVector featureVector = new FeatureVector();
        NominalFeature f1 = new NominalFeature("nominalFeature1", "test");
        Feature<String> f2 = new Feature<String>("nominalFeature3", "test");
        NumericFeature f3 = new NumericFeature("numericFeature1", 2.);
        Feature<Double> f4 = new Feature<Double>("numericFeature2", 3.);
        featureVector.add(f1);
        featureVector.add(f2);
        featureVector.add(f3);
        featureVector.add(f4);
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

}
