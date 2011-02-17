package ws.palladian.control;

import junit.framework.Test;
import junit.framework.TestSuite;
import ws.palladian.classification.page.ClassifierTest;
import ws.palladian.extraction.PageAnalyzerTest;
import ws.palladian.helper.DBStoreTest;
import ws.palladian.persistence.DictionaryFileIndexTest;
import ws.palladian.preprocessing.multimedia.ImageHandlerTest;
import ws.palladian.preprocessing.normalization.NormalizationTest;
import ws.palladian.web.CrawlerTest;

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
        suite.addTestSuite(NormalizationTest.class);
        suite.addTestSuite(PageAnalyzerTest.class);
        suite.addTestSuite(DBStoreTest.class);

        // $JUnit-END$
        return suite;
    }

    public static void main(String args[]) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}