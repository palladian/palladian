/**
 * Created on: 09.06.2012 19:47:11
 */
package ws.palladian.extraction.pos;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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

    private final PipelineDocument<String> document;

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] { {"The quick brown fox jumps over the lazy dog."}, {"I like my cake."},
                {"Your gun is the best friend you have."}};
        return Arrays.asList(data);
    }

    public LingPipePosTaggerTest(String document) {
        super();

        this.document = new PipelineDocument<String>(document);
    }

    @Test
    public void test() throws FileNotFoundException, DocumentUnprocessableException {
        File modelFile = ResourceHelper.getResourceFile("/model/pos-en-general-brown.HiddenMarkovModel");
        PipelineProcessor tokenizer = new LingPipeTokenizer();
        PipelineProcessor objectOfClassUnderTest = new LingPipePosTagger(modelFile);

        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(tokenizer);
        pipeline.add(objectOfClassUnderTest);

        pipeline.process(document);
        System.out.println(document.getContent());
        TextAnnotationFeature featureVector = document.getFeatureVector().get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        for (Annotation<String> token : featureVector.getValue()) {
            System.out.print(" "
                    + token.getFeatureVector().get(LingPipePosTagger.PROVIDED_FEATURE_DESCRIPTOR).getValue());
        }
        System.out.println();
    }
}
