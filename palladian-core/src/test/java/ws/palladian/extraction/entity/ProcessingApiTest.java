/**
 * 
 */
package ws.palladian.extraction.entity;

import static org.junit.Assert.assertThat;

import java.io.File;

import org.hamcrest.Matchers;
import org.junit.Test;

import ws.palladian.extraction.entity.tagger.PalladianNer;
import ws.palladian.extraction.entity.tagger.PalladianNer.LanguageMode;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.TextAnnotationFeature;

/**
 * <p>
 * Tests whether the processing API works correct for the Named Entity Recognition.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 2.0
 * @since 0.1.7
 */
public final class ProcessingApiTest {

    /**
     * <p>
     * An example text to check Named Entity Recognition on.
     * </p>
     */
    private String FIXTURE = "John J. Smith and the Nexus One location iphone 4 mention Seattle in the text John J. Smith lives in Seattle.";

    /**
     * <p>
     * Tests the Palladian NER with the processing API on {@link #FIXTURE}
     * </p>
     * 
     * @throws Exception If something goes wrong.
     */
    @Test
    public void testProcessing() throws Exception {
        PalladianNer nerProcessor = new PalladianNer(LanguageMode.English);

        // Training NER model
        File trainingFile = ResourceHelper.getResourceFile("/ner/training.txt");
        File modelFile = File.createTempFile("tudnerEn", ".model.gz");
        nerProcessor.train(trainingFile, modelFile);

        // Loading trained NER model
        nerProcessor.loadModel(modelFile.getCanonicalPath());

        // Processing example Document
        TextDocument document = new TextDocument(FIXTURE);
        nerProcessor.processDocument(document);

        // Checking results
        TextAnnotationFeature extractedEntities = document
                .getFeature(NamedEntityRecognizer.PROVIDED_FEATURE_DESCRIPTOR);

        assertThat(extractedEntities.getValue().size(), Matchers.is(5));
        for (Annotation<String> nerAnnotation : extractedEntities.getValue()) {
            assertThat(nerAnnotation, Matchers.notNullValue());
        }
    }
}
