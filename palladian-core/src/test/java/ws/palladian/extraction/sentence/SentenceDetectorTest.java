package ws.palladian.extraction.sentence;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.core.Annotation;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

public class SentenceDetectorTest {

    private static final String fixture = "I require some help from a SAP SD & FI consultant.\nThere is a main plant (godown) which services all the offices in the state Maharashtra. There are 2 offices in Maharashtra, one in Mumbai & other in Pune.\nThese two offices have got plants of their own as a business area is required for the sales office for accounting purpose. The sales employee is mapped to the sales office.\nWe want to sell from these offices, but dispatch should go from the godown (main plant), however; the accounting of COGS & Sales should happen in the Sales office so that profitability for the sales office is known.\nPresently, in order to cater to this, we have created a plant for the sales office . In order to fit the above requirement, we do an additional movement of stocks from main godown to Sales office plant & then do the sales. The godown keeper also does the delivery / PGI from the sales office plant.\nHow can we eliminate this sales office plant & additional stock movement process and also achieve our requirement of capturing profitability for the sales office.";
    private static String fixture2;
    private static Set<String> expectedResult;

    static {
        expectedResult = new HashSet<String>();
        expectedResult.add("I require some help from a SAP SD & FI consultant.");
        expectedResult.add("There is a main plant (godown) which services all the offices in the state Maharashtra.");
        expectedResult.add("There are 2 offices in Maharashtra, one in Mumbai & other in Pune.");
        expectedResult
                .add("These two offices have got plants of their own as a business area is required for the sales office for accounting purpose.");
        expectedResult.add("The sales employee is mapped to the sales office.");
        expectedResult
                .add("We want to sell from these offices, but dispatch should go from the godown (main plant), however; the accounting of COGS & Sales should happen in the Sales office so that profitability for the sales office is known.");
        expectedResult.add("Presently, in order to cater to this, we have created a plant for the sales office .");
        expectedResult
                .add("In order to fit the above requirement, we do an additional movement of stocks from main godown to Sales office plant & then do the sales.");
        expectedResult.add("The godown keeper also does the delivery / PGI from the sales office plant.");
        expectedResult
                .add("How can we eliminate this sales office plant & additional stock movement process and also achieve our requirement of capturing profitability for the sales office.");
    }

    @BeforeClass
    public static void setUp() throws Exception {
        fixture2 = FileHelper.readFileToString(ResourceHelper.getResourceFile("/texts/contribution02.txt"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        fixture2 = null;
    }

    @Test
    public void testLingPipeSentenceChunker() throws Exception {
        AbstractSentenceDetector sentenceDetector = new LingPipeSentenceDetector();
        List<Annotation> sentences = sentenceDetector.getAnnotations(fixture);
        for (Annotation sentence : sentences) {
            assertTrue(expectedResult.contains(sentence.getValue()));
        }
    }

    @Test
    public void testOpenNLPSentenceChunker() throws Exception {
        AbstractSentenceDetector sentenceDetector = new OpenNlpSentenceDetector(
                ResourceHelper.getResourceFile("/model/en-sent.bin"));
        List<Annotation> sentences = sentenceDetector.getAnnotations(fixture);
        for (Annotation sentence : sentences) {
            assertTrue(expectedResult.contains(sentence.getValue()));
        }
    }

    @Test
    public void testPalladianSentenceChunker() throws FileNotFoundException {
        AbstractSentenceDetector sentenceDetector = new PalladianSentenceDetector();
        List<Annotation> sentences = sentenceDetector.getAnnotations(fixture2);
        assertThat(sentences.size(), Matchers.is(269));
        assertThat(sentences.get(sentences.size() - 1).getValue(), is("DBConnection disconnect\nINFO: disconnected"));
    }

    @Test
    public void testPalladianSentenceChunkerWithMaskAtEndOfText() {
        AbstractSentenceDetector sentenceDetector = new PalladianSentenceDetector();
        List<Annotation> sentences = sentenceDetector
                .getAnnotations("Web Dynpro is just in ramp up now. You can't use Web Dynpro in production environments.\n\nYou can develop BSP and J2EE Applications with 6.20. You connect to your R/3 System through RFC. This applications can also be used in 4.7.");
        assertThat(sentences.size(), is(5));
        assertThat(sentences.get(sentences.size() - 1).getValue(), is("This applications can also be used in 4.7."));
    }

    @Test
    public void testPalladianSentenceChunkerWithLineBreakAtEndOfText() throws IOException {
        AbstractSentenceDetector sentenceDetector = new PalladianSentenceDetector();
        String text = FileHelper.readFileToString(ResourceHelper.getResourceFile("/texts/contribution03.txt"));
        List<Annotation> sentences = sentenceDetector.getAnnotations(text);
        assertThat(sentences.size(), is(81));
        assertThat(sentences.get(sentences.size() - 1).getValue(), is("Return code: 4"));
    }
}
