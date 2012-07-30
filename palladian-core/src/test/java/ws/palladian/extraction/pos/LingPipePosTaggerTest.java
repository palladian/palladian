/**
 * Created on: 09.06.2012 19:47:11
 */
package ws.palladian.extraction.pos;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ws.palladian.extraction.TagAnnotations;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.LingPipeTokenizer;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.TextAnnotationFeature;

/**
 * <p>
 * Tests the correct working of the LinPipe POS Tagger implementation in Palladian.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
@RunWith(value = Parameterized.class)
public class LingPipePosTaggerTest {

    /**
     * <p>
     * 
     * </p>
     */
    private static final String MODEL = "/model/pos-en-general-brown.HiddenMarkovModel";
    private final PipelineDocument<String> document;
    private final String[] expectedTags;

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] {
                {"The quick brown fox jumps over the lazy dog.",
                        new String[] {"AT", "JJ", "JJ", "NN", "NNS", "IN", "AT", "JJ", "NN", "."}},
                {"I like my cake.", new String[] {"PPSS", "VB", "PP$", "NN", "."}},
                {"Your gun is the best friend you have.",
                        new String[] {"PP$", "NN", "BEZ", "AT", "JJT", "NN", "PPSS", "HV", "."}},
                {
                        "I'm here to say that we're about to do that.",
                        new String[] {"PPSS", "'", "BEM", "RB", "TO", "VB", "CS", "PPSS", "'", "QL", "RB", "TO", "DO",
                                "DT", "."}}};
        return Arrays.asList(data);
    }

    public LingPipePosTaggerTest(String document, String[] expectedTags) {
        super();

        this.document = new PipelineDocument<String>(document);
        this.expectedTags = expectedTags;
    }

    @Test
    public void test() throws FileNotFoundException, DocumentUnprocessableException {
        File modelFile = ResourceHelper.getResourceFile(MODEL);
        PipelineProcessor tokenizer = new LingPipeTokenizer();
        PipelineProcessor objectOfClassUnderTest = new LingPipePosTagger(modelFile);

        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(tokenizer);
        pipeline.add(objectOfClassUnderTest);

        pipeline.process(document);
        TextAnnotationFeature featureVector = document.getFeatureVector()
                .get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation<String>> tokens = featureVector.getValue();
        for (int i = 0; i < tokens.size(); i++) {
            Assert.assertThat(tokens.get(i).getFeature(LingPipePosTagger.PROVIDED_FEATURE_DESCRIPTOR).getValue(),
                    Matchers.is(expectedTags[i]));
        }
    }

    @Test
    public void testSimple() throws FileNotFoundException {
        File modelFile = ResourceHelper.getResourceFile(MODEL);
        BasePosTagger tagger = new LingPipePosTagger(modelFile);
        TagAnnotations tagResult = tagger.tag(document.getContent());
        Assert.assertEquals(expectedTags.length, tagResult.size());
    }
}
