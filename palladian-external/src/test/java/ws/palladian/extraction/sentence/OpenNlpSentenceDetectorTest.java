package ws.palladian.extraction.sentence;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Iterator;

import org.junit.Test;

import ws.palladian.core.Token;
import ws.palladian.helper.io.ResourceHelper;

public class OpenNlpSentenceDetectorTest {

    private static final String SENTENCE_MODEL_RESOURCE = "/model/en-sent.bin";
    
    private static final String TEXT = "I require some help from a SAP SD & FI consultant.\nThere is a main plant (godown) which services all the offices in the state Maharashtra. There are 2 offices in Maharashtra, one in Mumbai & other in Pune.\nThese two offices have got plants of their own as a business area is required for the sales office for accounting purpose. The sales employee is mapped to the sales office.\nWe want to sell from these offices, but dispatch should go from the godown (main plant), however; the accounting of COGS & Sales should happen in the Sales office so that profitability for the sales office is known.\nPresently, in order to cater to this, we have created a plant for the sales office . In order to fit the above requirement, we do an additional movement of stocks from main godown to Sales office plant & then do the sales. The godown keeper also does the delivery / PGI from the sales office plant.\nHow can we eliminate this sales office plant & additional stock movement process and also achieve our requirement of capturing profitability for the sales office.";

    @Test
    public void testOpenNLPSentenceChunker() throws Exception {
        File sentenceModel = ResourceHelper.getResourceFile(SENTENCE_MODEL_RESOURCE);
        SentenceDetector sentenceDetector = new OpenNlpSentenceDetector(sentenceModel);
        Iterator<Token> sentences = sentenceDetector.iterateTokens(TEXT);
        assertEquals(0, sentences.next().getStartPosition());
        assertEquals(51, sentences.next().getStartPosition());
        assertEquals(139, sentences.next().getStartPosition());
        assertEquals(206, sentences.next().getStartPosition());
        assertEquals(329, sentences.next().getStartPosition());
        assertEquals(379, sentences.next().getStartPosition());
        assertEquals(595, sentences.next().getStartPosition());
        assertEquals(680, sentences.next().getStartPosition());
        assertEquals(818, sentences.next().getStartPosition());
        assertEquals(894, sentences.next().getStartPosition());
    }
}
