package ws.palladian.extraction.feature;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.extraction.pos.BasePosTagger;
import ws.palladian.extraction.pos.LingPipePosTagger;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.LingPipeTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * Tests the correct behavior of the N-Gram creating {@code PipelineProcessor}s.
 * </p>
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @version 2.0
 * @since 0.1.7
 */
public class NGramCreatorTest {

    private PipelineDocument<String> document;
    private ProcessingPipeline pipeline;

    @Before
    public void setUp() {
        document = new TextDocument("the quick brown fox jumps over the lazy dog");
        pipeline = new ProcessingPipeline();
    }

    public void tearDown() {
        document = null;
        pipeline = null;
    }

    @Test
    public void testNGramCreator() throws DocumentUnprocessableException {
        pipeline.addWithDefaultConnection(new RegExTokenizer());
        pipeline.addWithDefaultConnection(new StopTokenRemover(Language.ENGLISH));
        pipeline.addWithDefaultConnection(new NGramCreator(2));
        pipeline.process(document);

        List<PositionAnnotation> annotations = document.getFeatureVector().getAll(PositionAnnotation.class,
                BaseTokenizer.PROVIDED_FEATURE);

        assertEquals(10, annotations.size());
        assertEquals("quick brown", annotations.get(6).getValue());
        assertEquals("brown fox", annotations.get(7).getValue());
        assertEquals("fox jumps", annotations.get(8).getValue());
        assertEquals("lazy dog", annotations.get(9).getValue());
    }

    /**
     * <p>
     * Test the NGramCreator that preserves annotations added to token.
     * </p>
     * 
     * @throws Exception
     */
    @Test
    public void testNGramCreatorPreserveAnnotations() throws Exception {
        pipeline.addWithDefaultConnection(new LingPipeTokenizer());
        pipeline.addWithDefaultConnection(new LingPipePosTagger(ResourceHelper
                .getResourceFile("/model/pos-en-general-brown.HiddenMarkovModel")));
        pipeline.addWithDefaultConnection(new NGramCreator(BasePosTagger.PROVIDED_FEATURE));
        pipeline.process(document);

        List<PositionAnnotation> annotations = document.getFeatureVector().getAll(PositionAnnotation.class,
                BaseTokenizer.PROVIDED_FEATURE);

        assertEquals(annotations.size(), 17);
        assertEquals("the quick", annotations.get(9).getValue());
        assertEquals("quick brown", annotations.get(10).getValue());
        assertEquals("brown fox", annotations.get(11).getValue());
        assertEquals("fox jumps", annotations.get(12).getValue());

        assertThat(
                annotations.get(9).getFeatureVector().getFeature(NominalFeature.class, BasePosTagger.PROVIDED_FEATURE)
                        .getValue(), is("ATJJ"));
        assertThat(
                annotations.get(10).getFeatureVector().getFeature(NominalFeature.class, BasePosTagger.PROVIDED_FEATURE)
                        .getValue(), is("JJJJ"));
        assertThat(
                annotations.get(11).getFeatureVector().getFeature(NominalFeature.class, BasePosTagger.PROVIDED_FEATURE)
                        .getValue(), is("JJNN"));
        assertThat(
                annotations.get(12).getFeatureVector().getFeature(NominalFeature.class, BasePosTagger.PROVIDED_FEATURE)
                        .getValue(), is("NNNNS"));
        assertThat(
                annotations.get(13).getFeatureVector().getFeature(NominalFeature.class, BasePosTagger.PROVIDED_FEATURE)
                        .getValue(), is("NNSIN"));
    }

}
