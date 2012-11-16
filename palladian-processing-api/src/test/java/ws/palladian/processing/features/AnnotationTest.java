package ws.palladian.processing.features;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.processing.TextDocument;

public class AnnotationTest {

    @Test
    public void testGetFeature() {

        TextDocument document = new TextDocument("The quick brown fox.");
        PositionAnnotation annotation = new PositionAnnotation("term", document, 0, 3, "The");
        assertEquals("The", annotation.getValue());

        NominalFeature stopwordFeature = new NominalFeature("stopword", "true");
        NumericFeature lengthFeature = new NumericFeature("length", 3.);
        annotation.getFeatureVector().add(stopwordFeature);
        annotation.getFeatureVector().add(lengthFeature);

        NominalFeature retrievedFeature = annotation.getFeatureVector().getFeature(NominalFeature.class, "stopword");
        assertEquals(stopwordFeature, retrievedFeature);

        NumericFeature retrievedFeature2 = annotation.getFeatureVector().getFeature(NumericFeature.class, "length");
        assertEquals(lengthFeature, retrievedFeature2);
    }

}
