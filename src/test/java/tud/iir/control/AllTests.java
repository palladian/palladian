package tud.iir.control;

import tud.iir.classification.page.ClassifierTest;
import tud.iir.extraction.FactExtractionTest;
import tud.iir.extraction.ListDiscoveryTest;
import tud.iir.helper.DBStoreTest;
import tud.iir.helper.XPathTest;
import tud.iir.multimedia.ImageHandlerTest;
import tud.iir.normalization.NormalizationTest;
import tud.iir.persistence.DictionaryFileIndexTest;
import tud.iir.web.CrawlerTest;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * All tests.
 * 
 * @author David Urbansky
 */
public class AllTests {

    // if true, also tests that require network access (online) are run
    public static final boolean ALL_TESTS = false;

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for test");
        // $JUnit-BEGIN$
        suite.addTestSuite(ClassifierTest.class);
        suite.addTestSuite(DictionaryFileIndexTest.class);
        suite.addTestSuite(CrawlerTest.class);
        suite.addTestSuite(ImageHandlerTest.class);
        suite.addTestSuite(FactExtractionTest.class);
        suite.addTestSuite(ListDiscoveryTest.class);
        suite.addTestSuite(NormalizationTest.class);
        suite.addTestSuite(XPathTest.class);
        suite.addTestSuite(DBStoreTest.class);

        // $JUnit-END$
        return suite;
    }

    public static void main(String args[]) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}