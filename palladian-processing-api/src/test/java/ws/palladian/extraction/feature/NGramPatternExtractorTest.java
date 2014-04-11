/**
 * Created on: 20.06.2012 21:05:02
 */
package ws.palladian.extraction.feature;

import java.io.FileNotFoundException;

import org.junit.Test;

import ws.palladian.extraction.patterns.NGramPatternExtractionStrategy;
import ws.palladian.extraction.patterns.SequentialPatternAnnotator;
import ws.palladian.extraction.pos.OpenNlpPosTagger;
import ws.palladian.extraction.sentence.PalladianSentenceDetector;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public class NGramPatternExtractorTest {

    private String testText = "The quick brown fox jumped over the lazy dog.";
    private final static String[] KEYWORDS = new String[] {"the"};

    @Test
    public void test() throws FileNotFoundException, DocumentUnprocessableException {
        ProcessingPipeline processingPipeline = new ProcessingPipeline();
        processingPipeline.add(new PalladianSentenceDetector());
        processingPipeline.add(new LowerCaser());
        processingPipeline.add(new RegExTokenizer());
        processingPipeline.add(new OpenNlpPosTagger(ResourceHelper.getResourceFile("/model/en-pos-maxent.bin")));
        processingPipeline.add(new SequentialPatternAnnotator(KEYWORDS, 3, 4, new NGramPatternExtractionStrategy()));

        PipelineDocument<String> document = new TextDocument(testText);

        processingPipeline.process(document);

        // for (Annotation annotation : document.getFeatureVector()
        // .get(AbstractSentenceDetector.PROVIDED_FEATURE_DESCRIPTOR).getValue()) {
        // SequentialPattern lsp = annotation.getFeatureVector().get(
        // SequentialPatternAnnotator.PROVIDED_FEATURE_DESCRIPTOR);
        // Assert.assertThat(lsp, Matchers.isIn(expectedPatterns));
        // }
    }

}
