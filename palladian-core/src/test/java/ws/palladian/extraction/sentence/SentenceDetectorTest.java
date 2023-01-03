package ws.palladian.extraction.sentence;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ws.palladian.core.Token;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SentenceDetectorTest {
    private static String fixture;

    @BeforeClass
    public static void setUp() throws Exception {
        fixture = FileHelper.readFileToString(ResourceHelper.getResourceFile("/texts/contribution02.txt"));
    }

    @AfterClass
    public static void tearDown() {
        fixture = null;
    }

    @Test
    public void testPalladianSentenceChunker() {
        SentenceDetector sentenceDetector = new PalladianSentenceDetector(Language.ENGLISH);
        List<Token> sentences = CollectionHelper.newArrayList(sentenceDetector.iterateTokens(fixture));
//        assertThat(sentences.size(), Matchers.is(269)); <- .java recognized as domain
        assertThat(sentences.get(sentences.size() - 1).getValue(), is("DBConnection disconnect\nINFO: disconnected"));
    }

    @Test
    public void testPalladianSentenceChunkerWithMaskAtEndOfText() {
        SentenceDetector sentenceDetector = new PalladianSentenceDetector(Language.ENGLISH);
        List<Token> sentences = CollectionHelper
                .newArrayList(sentenceDetector
                        .iterateTokens("Web Dynpro is just in ramp up now. You can't use Web Dynpro in production environments.\n\nYou can develop BSP and J2EE Applications with 6.20. You connect to your R/3 System through RFC. This applications can also be used in 4.7."));
        assertThat(sentences.size(), is(5));
        assertThat(sentences.get(sentences.size() - 1).getValue(), is("This applications can also be used in 4.7."));
    }

    @Test
    public void testPalladianSentenceChunkerWithLineBreakAtEndOfText() throws IOException {
        SentenceDetector sentenceDetector = new PalladianSentenceDetector(Language.ENGLISH);
        String text = FileHelper.readFileToString(ResourceHelper.getResourceFile("/texts/contribution03.txt"));
        List<Token> sentences = CollectionHelper.newArrayList(sentenceDetector.iterateTokens(text));
        assertThat(sentences.size(), is(75));
        assertThat(sentences.get(sentences.size() - 1).getValue(), is("Return code: 4"));
    }
}
