package ws.palladian.extraction.token;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.core.Annotation;

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

    private static final String TWEET2 = "Funny! But I wonder why? Hmmm ~~&gt; RT @MarketWatch: Diamond feared Barclays nationalization in 2008 http://t.co/EbRcgLYf";

    private TwokenizeTokenizer tokenizer;

    @Before
    public void setUp() {
        tokenizer = new TwokenizeTokenizer();
    }

    @Test
    public void testTwokenizeTokenizer() {
        List<Annotation> annotations = tokenizer.getAnnotations(TWEET);
        assertEquals(TOKENS.length, annotations.size());
        for (int i = 0; i < TOKENS.length; i++) {
            assertEquals(TOKENS[i], annotations.get(i).getValue());
        }
    }

    @Test
    public void testTwokenizeProblem() {
        // see comment in TwokenizeTokenizer class, line 35
        tokenizer.getAnnotations(TWEET2);
    }

}
