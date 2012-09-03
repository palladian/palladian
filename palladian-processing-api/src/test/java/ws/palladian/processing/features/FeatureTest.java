package ws.palladian.processing.features;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class FeatureTest {
    
    @Test
    public void testEquals() {
        NominalFeature nom1 = new NominalFeature("testFeature1", "foo");
        NominalFeature nom2 = new NominalFeature("testFeature1", "foo");
        NominalFeature nom3 = new NominalFeature("testFeature2", "foo");
        NominalFeature nom4 = new NominalFeature("testFeature1", "bar");
        
        assertEquals(nom1, nom2);
        assertFalse(nom1.equals(nom3));
        assertFalse(nom1.equals(nom4));
        
        NumericFeature num1 = new NumericFeature("testFeature2", 1.);
        NumericFeature num2 = new NumericFeature("testFeature2", 1.);
        assertEquals(num1, num2);
    }

}
