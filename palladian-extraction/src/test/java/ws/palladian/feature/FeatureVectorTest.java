package ws.palladian.feature;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NominalFeature;

public class FeatureVectorTest {

    @Test
    public void testFeatureVector() {
        FeatureVector featureVector = new FeatureVector();
        FeatureDescriptor<NominalFeature> featureDescriptor = FeatureDescriptorBuilder.build("myNominalFeature",
                NominalFeature.class);

        NominalFeature nominalFeature = new NominalFeature(featureDescriptor, "test");
        featureVector.add(nominalFeature);

        NominalFeature retrievedFeature = featureVector.get(featureDescriptor);
        
        assertEquals("test", retrievedFeature.getValue());
        assertEquals(NominalFeature.class, retrievedFeature.getClass());

    }

}
