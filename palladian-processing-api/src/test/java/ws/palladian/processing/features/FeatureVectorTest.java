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

        List<Annotation<String>> annotations = new ArrayList<Annotation<String>>();

        PipelineDocument<String> document = new PipelineDocument<String>("tet");

        Feature tf1 = new NominalFeature("pos", "testTerm1");
        PositionAnnotation wordAnnotation1 = new PositionAnnotation(document, 0, 1);
        wordAnnotation1.addFeature(tf1);

        PositionAnnotation wordAnnotation2 = new PositionAnnotation(document, 0, 1);
        Feature tf2 = new NominalFeature("pos", "testTerm2");
        wordAnnotation2.addFeature(tf2);

        annotations.add(wordAnnotation1);
        annotations.add(wordAnnotation2);

        FeatureVector featureVector = new FeatureVector();

        Feature f1 = new NominalFeature("term", "testTerm");
        TextAnnotationFeature af1 = new TextAnnotationFeature("terms", annotations);

        featureVector.add(f1);
        featureVector.add(af1);

        document.setFeatureVector(featureVector);

        NominalFeature feature = featureVector.getFeature(NominalFeature.class, "term");

        assertEquals("testTerm", feature.getValue());

        List<? extends Feature<String>> features = featureVector.getFeatures(NominalFeature.class, "terms/pos");
        for (Feature<String> featureEntry : features) {
            assertTrue(featureEntry.getValue().contains("testTerm"));
        }

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
