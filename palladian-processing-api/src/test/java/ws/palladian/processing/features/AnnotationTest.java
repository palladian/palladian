package ws.palladian.processing.features;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.processing.TextDocument;

public class AnnotationTest {
    
    @Test
    public void testGetFeature() {
        
        FeatureDescriptor<NominalFeature> stopwordFeatureDescriptor = FeatureDescriptorBuilder.build("stopword", NominalFeature.class);
        FeatureDescriptor<NumericFeature> lengthFeatureDescriptor = FeatureDescriptorBuilder.build("length", NumericFeature.class);
        
        TextDocument document = new TextDocument("The quick brown fox.");
        Annotation<String> annotation = new PositionAnnotation(document, 0, 3);
        assertEquals("The", annotation.getValue());
        
        NominalFeature stopwordFeature = new NominalFeature(stopwordFeatureDescriptor, "true");
        NumericFeature lengthFeature = new NumericFeature(lengthFeatureDescriptor, 3.);
        annotation.addFeature(stopwordFeature);
        annotation.addFeature(lengthFeature);
        
        NominalFeature retrievedFeature = annotation.getFeature(stopwordFeatureDescriptor);
        assertEquals(stopwordFeature, retrievedFeature);
        
        NumericFeature retrievedFeature2 = annotation.getFeature(lengthFeatureDescriptor);
        assertEquals(lengthFeature, retrievedFeature2);
    }

}
