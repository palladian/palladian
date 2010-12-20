package tud.iir.extraction;

import java.util.List;

import junit.framework.TestCase;
import tud.iir.extraction.qa.QAExtractor;
import tud.iir.knowledge.QA;

/**
 * Test cases for the xPath handling.
 * 
 * @author David Urbansky
 */
public class QATest extends TestCase {

    public QATest(String name) {
        super(name);
    }

    public void testFAQExtraction() {
        List<QA> qas = null;
        qas = QAExtractor.getInstance().extractFAQ("data/test/webPages/faq/abb.html");
        assertEquals(18, qas.size()); // TODO 20 would be better

        qas = QAExtractor.getInstance().extractFAQ("data/test/webPages/faq/sony.html");
        assertEquals(20, qas.size());

        // qas = QAExtractor.getInstance().extractFAQ("data/test/webPages/faq/otto.html");
        // assertEquals(6, qas.size());
    }

}