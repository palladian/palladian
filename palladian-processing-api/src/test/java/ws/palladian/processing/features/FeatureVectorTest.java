package ws.palladian.processing.features;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.processing.PipelineDocument;

public class FeatureVectorTest {

    private FeatureVector featureVector;
    private NominalFeature f1;
    private Feature<String> f2;
    private NumericFeature f3;
    private Feature<Double> f4;

    @Before
    public void setUp() {
        featureVector = new FeatureVector();
        f1 = new NominalFeature("nominalFeature1", "test");
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
    public void testRetrieveFeaturesByPath() {

        PipelineDocument<String> document = new PipelineDocument<String>("hello world");
        
        
        PositionAnnotation annotation1 = new PositionAnnotation(document, 0, 5);
        PositionAnnotation annotation2 = new PositionAnnotation(document, 6, 11);
        
        // features for terms
        NominalFeature feature1 = new NominalFeature("feature1", "value1");
        annotation1.addFeature(feature1);
        
        NominalFeature feature2 = new NominalFeature("feature1", "value2");
        annotation2.addFeature(feature2);
        
        TextAnnotationFeature annotationFeature = new TextAnnotationFeature("terms");
        annotationFeature.add(annotation1);
        annotationFeature.add(annotation2);

        // feature for document
        NominalFeature documentFeature = new NominalFeature("term", "testTerm");

        // add features
        document.addFeature(documentFeature);
        document.addFeature(annotationFeature);

        NominalFeature feature = document.getFeatureVector().get(NominalFeature.class, "term");

        assertEquals("testTerm", feature.getValue());
        
        System.out.println(document.getFeatureVector());

        List<? extends Feature<String>> features = document.getFeatureVector().getFeatures(NominalFeature.class, "terms/feature1");
        assertEquals(2, features.size());
        assertEquals("value1", features.get(0).getValue());
        assertEquals("value2", features.get(1).getValue());

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

    // @Test
    // public void testCopyFeatureVector() {Class<? extends Feature<T>> class1
    // FeatureVector newFeatureVector = new FeatureVector(featureVector);
    // assertEquals(4, newFeatureVector.size());
    // }

}
