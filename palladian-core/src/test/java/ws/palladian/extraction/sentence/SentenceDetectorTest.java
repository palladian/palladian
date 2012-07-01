package ws.palladian.extraction.sentence;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.processing.features.Annotation;

public class SentenceDetectorTest {

    private static final String fixture = "I require some help from a SAP SD & FI consultant.\nThere is a main plant (godown) which services all the offices in the state Maharashtra. There are 2 offices in Maharashtra, one in Mumbai & other in Pune.\nThese two offices have got plants of their own as a business area is required for the sales office for accounting purpose. The sales employee is mapped to the sales office.\nWe want to sell from these offices, but dispatch should go from the godown (main plant), however; the accounting of COGS & Sales should happen in the Sales office so that profitability for the sales office is known.\nPresently, in order to cater to this, we have created a plant for the sales office . In order to fit the above requirement, we do an additional movement of stocks from main godown to Sales office plant & then do the sales. The godown keeper also does the delivery / PGI from the sales office plant.\nHow can we eliminate this sales office plant & additional stock movement process and also achieve our requirement of capturing profitability for the sales office.";
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

    private AbstractSentenceDetector objectOfClassUnderTest;

    @Before
    public void setUp() throws Exception {
    }

    public void tearDown() throws Exception {
        objectOfClassUnderTest = null;
        System.gc();
    }

    @Test
    public void testLingPipeSentenceChunker() throws Exception {
        objectOfClassUnderTest = new LingPipeSentenceDetector();
        objectOfClassUnderTest.detect(fixture);
        Annotation[] sentences = objectOfClassUnderTest.getSentences();
        for (Annotation sentence : sentences) {
            assertTrue(expectedResult.contains(sentence.getValue()));
        }
    }

    @Test
    public void testOpenNLPSentenceChunker() throws Exception {
        objectOfClassUnderTest = new OpenNlpSentenceDetector(ResourceHelper.getResourceFile("/model/en-sent.bin"));
        objectOfClassUnderTest.detect(fixture);
        Annotation[] sentences = objectOfClassUnderTest.getSentences();
        for (Annotation sentence : sentences) {
            assertTrue(expectedResult.contains(sentence.getValue()));
        }
    }
}
