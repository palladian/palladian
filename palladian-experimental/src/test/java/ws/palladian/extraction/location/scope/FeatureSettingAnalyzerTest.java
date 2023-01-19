package ws.palladian.extraction.location.scope;

import org.junit.Test;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FeatureSettingAnalyzerTest {

    private final String text = "The quick brown fox jumps over the lazy dog";

    @SuppressWarnings("resource")
    @Test
    public void testFeatureSettingAnalyzer() {
        FeatureSetting featureSetting = FeatureSettingBuilder.chars(2, 5).create();
        List<String> tokens = new FeatureSettingAnalyzer(featureSetting).analyze(text);
        assertCharLength(2, 5, tokens);
        assertAllLowercase(tokens);

        featureSetting = FeatureSettingBuilder.chars(1).create();
        tokens = new FeatureSettingAnalyzer(featureSetting).analyze(text);
        assertCharLength(1, 1, tokens);
        assertAllLowercase(tokens);

        featureSetting = FeatureSettingBuilder.chars(1).maxTerms(10).create();
        tokens = new FeatureSettingAnalyzer(featureSetting).analyze(text);
        assertCharLength(1, 1, tokens);
        assertAllLowercase(tokens);
        assertEquals(10, tokens.size());

        featureSetting = FeatureSettingBuilder.words().create();
        tokens = new FeatureSettingAnalyzer(featureSetting).analyze(text);
        assertTokenCount(1, 1, tokens);
        assertAllLowercase(tokens);
        assertEquals(9, tokens.size());

        featureSetting = FeatureSettingBuilder.words(1, 5).create();
        tokens = new FeatureSettingAnalyzer(featureSetting).analyze(text);
        assertTokenCount(1, 5, tokens);
        assertAllLowercase(tokens);
        assertEquals(35, tokens.size());

        featureSetting = FeatureSettingBuilder.words().termLength(5, 20).create();
        tokens = new FeatureSettingAnalyzer(featureSetting).analyze(text);
        assertTokenCount(1, 1, tokens);
        assertAllLowercase(tokens);
        assertEquals(3, tokens.size());
        assertCharLength(5, 20, tokens);
    }

    private static void assertAllLowercase(List<String> tokens) {
        for (String token : tokens) {
            assertTrue("tokens must be lowercase", token.equals(token.toLowerCase()));
        }
    }

    private static void assertCharLength(int min, int max, List<String> tokens) {
        for (String token : tokens) {
            assertTrue("token length must be >= " + min + " and <= " + max + ", but was " + token.length(), token.length() >= min && token.length() <= max);
        }
    }

    private static void assertTokenCount(int min, int max, List<String> tokens) {
        for (String token : tokens) {
            int numTokens = token.split("\\s").length;
            assertTrue("token count must be >= " + min + " and <= " + max + ", but was " + numTokens, numTokens >= min && numTokens <= max);
        }
    }

}
