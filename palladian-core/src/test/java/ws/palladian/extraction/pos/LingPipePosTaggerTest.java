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

import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.features.Annotation;

/**
 * <p>
 * Tests the correct working of the LingPipe POS Tagger implementation in Palladian.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
@RunWith(value = Parameterized.class)
public class LingPipePosTaggerTest {

    private static final String MODEL = "/model/pos-en-general-brown.HiddenMarkovModel";
    private final String text;
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

    public LingPipePosTaggerTest(String text, String[] expectedTags) {
        this.text = text;
        this.expectedTags = expectedTags;
    }

    @Test
    public void test() throws FileNotFoundException, DocumentUnprocessableException {
        File modelFile = ResourceHelper.getResourceFile(MODEL);
        LingPipePosTagger posTagger = new LingPipePosTagger(modelFile);
        List<Annotation> tokens = posTagger.getAnnotations(text);
        for (int i = 0; i < tokens.size(); i++) {
            Assert.assertThat(tokens.get(i).getTag(), Matchers.is(expectedTags[i]));
        }
    }

}
