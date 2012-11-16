package ws.palladian.processing.features;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import ws.palladian.processing.TextDocument;

public class FeatureVectorTest {

    @Test
    public void testRetrieveFeaturesByDescriptor() {
        FeatureVector featureVector = new FeatureVector();
        
        NominalFeature nominalFeature = new NominalFeature("testFeature", "test");
        featureVector.add(nominalFeature);

        NominalFeature retrievedFeature = featureVector.getFeature(NominalFeature.class, "testFeature");

        assertEquals("test", retrievedFeature.getValue());
        assertEquals(NominalFeature.class, retrievedFeature.getClass());
    }

    @Test
    public void testRetrieveFeaturesByPath() {

        TextDocument document = new TextDocument("hello world");
        
        PositionAnnotation annotation1 = new PositionAnnotation("term", document, 0, 5, "hello");
        PositionAnnotation annotation2 = new PositionAnnotation("term", document, 6, 11, "world");
        
        // features for terms
//        NominalFeature feature1 = new NominalFeature("feature1", "value1");
//        annotation1.getFeatureVector().add(feature1);
        
//        NominalFeature feature2 = new NominalFeature("feature1", "value2");
//        annotation2.getFeatureVector().add(feature2);
        
//        TextAnnotationFeature annotationFeature = new TextAnnotationFeature("terms");
//        annotationFeature.add(annotation1);
//        annotationFeature.add(annotation2);

        // feature for document
//        NominalFeature documentFeature = new NominalFeature("term", "testTerm");

        // add features
//        document.addFeature(documentFeature);
//        document.addFeature(annotationFeature);
        document.getFeatureVector().add(annotation1);
        document.getFeatureVector().add(annotation2);

//        NominalFeature feature = document.getFeatureVector().getFeature(NominalFeature.class, "term");
//
//        assertEquals("testTerm", feature.getValue());
//        
//        System.out.println(document.getFeatureVector());

        List<PositionAnnotation> features = document.getFeatureVector().getAll(PositionAnnotation.class, "term");
        assertEquals(2, features.size());
        assertEquals("hello", features.get(0).getValue());
        assertEquals("world", features.get(1).getValue());

    }

    @Test
    public void testRetrieveFeaturesByType() {
        FeatureVector featureVector = new FeatureVector();
        NominalFeature f1 = new NominalFeature("nominalFeature1", "test");
        Feature<String> f2 = new NominalFeature("nominalFeature3", "test");
        NumericFeature f3 = new NumericFeature("numericFeature1", 2.);
        Feature<Double> f4 = new NumericFeature("numericFeature2", 3.);
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
        assertNull(featureVector.getFeature(NominalFeature.class, "numericFeature1"));
    }

    // @Test
    // public void testCopyFeatureVector() {Class<? extends Feature<T>> class1
    // FeatureVector newFeatureVector = new FeatureVector(featureVector);
    // assertEquals(4, newFeatureVector.size());
    // }

}
