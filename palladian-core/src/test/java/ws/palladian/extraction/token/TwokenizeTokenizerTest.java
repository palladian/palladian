package ws.palladian.extraction.token;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;

/**
 * <p>
 * Test for {@link TwokenizeTokenizer}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class TwokenizeTokenizerTest {

    private static final String TWEET = "I predict I won't win a single game I bet on. Got Cliff Lee today, so if he loses its on me RT @e_one: Texas (cont) http://tl.gd/6meogh";
    private static final String TOKENS[] = {"I", "predict", "I", "won't", "win", "a", "single", "game", "I", "bet",
            "on", ".", "Got", "Cliff", "Lee", "today", ",", "so", "if", "he", "loses", "its", "on", "me", "RT",
            "@e_one", ":", "Texas", "(", "cont", ")", "http://tl.gd/6meogh"};

    @Test
    public void testTwokenizeTokenizer() throws Exception {
        TwokenizeTokenizer tokenizer = new TwokenizeTokenizer();
        PipelineDocument<String> document = new PipelineDocument<String>(TWEET);
        tokenizer.processDocument(document);
        AnnotationFeature annotationFeature = document.getFeatureVector().get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> annotationList = annotationFeature.getValue();
        assertEquals(TOKENS.length, annotationList.size());
        for (int i = 0; i < TOKENS.length; i++) {
            assertEquals(TOKENS[i], annotationList.get(i).getValue());
        }
    }

}
