package ws.palladian.extraction.pos;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.PositionAnnotation;

public class OpenNlpPosTaggerTest {

    private final TextDocument document = new TextDocument("The quick brown fox jumps over the lazy dog.");
    private File modelFile;

    @Before
    public void setUp() throws FileNotFoundException {
        modelFile = ResourceHelper.getResourceFile("/model/en-pos-maxent.bin");
    }

    @Test
    public void testOpenNlpPosTagger() throws DocumentUnprocessableException {
        ProcessingPipeline processingPipeline = new ProcessingPipeline();
        processingPipeline.add(new RegExTokenizer());
        processingPipeline.add(new OpenNlpPosTagger(modelFile));
        processingPipeline.process(document);

        List<PositionAnnotation> annotations = RegExTokenizer.getTokenAnnotations(document);

        assertEquals(10, annotations.size());
        assertEquals("DT",
                annotations.get(0).getFeatureVector().getFeature(NominalFeature.class, BasePosTagger.PROVIDED_FEATURE)
                        .getValue());
        assertEquals("JJ",
                annotations.get(1).getFeatureVector().getFeature(NominalFeature.class, BasePosTagger.PROVIDED_FEATURE)
                        .getValue());
        assertEquals("JJ",
                annotations.get(2).getFeatureVector().getFeature(NominalFeature.class, BasePosTagger.PROVIDED_FEATURE)
                        .getValue());
        assertEquals("NN",
                annotations.get(3).getFeatureVector().getFeature(NominalFeature.class, BasePosTagger.PROVIDED_FEATURE)
                        .getValue());
        assertEquals("NNS",
                annotations.get(4).getFeatureVector().getFeature(NominalFeature.class, BasePosTagger.PROVIDED_FEATURE)
                        .getValue());
        assertEquals("IN",
                annotations.get(5).getFeatureVector().getFeature(NominalFeature.class, BasePosTagger.PROVIDED_FEATURE)
                        .getValue());
        assertEquals("DT",
                annotations.get(6).getFeatureVector().getFeature(NominalFeature.class, BasePosTagger.PROVIDED_FEATURE)
                        .getValue());
        assertEquals("JJ",
                annotations.get(7).getFeatureVector().getFeature(NominalFeature.class, BasePosTagger.PROVIDED_FEATURE)
                        .getValue());
        assertEquals("NN",
                annotations.get(8).getFeatureVector().getFeature(NominalFeature.class, BasePosTagger.PROVIDED_FEATURE)
                        .getValue());
        assertEquals(".",
                annotations.get(9).getFeatureVector().getFeature(NominalFeature.class, BasePosTagger.PROVIDED_FEATURE)
                        .getValue());
    }

}
