package ws.palladian.control;

import junit.framework.Test;
import junit.framework.TestSuite;
import ws.palladian.classification.page.ClassifierTest;
import ws.palladian.preprocessing.normalization.NormalizationTest;
import ws.palladian.retrieval.CrawlerTest;

/**
 * All tests performed by Hudson.
 * 
 * @author David Urbansky
 */
public class HudsonTests {

    // if true, also tests that require network access (online) are run
    public static final boolean ALL_TESTS = false;

    public static Test suite() {
        TestSuite suite = new TestSuite("Hudson tests.");
        // $JUnit-BEGIN$
        suite.addTestSuite(ClassifierTest.class);
        suite.addTestSuite(CrawlerTest.class);
        suite.addTestSuite(NormalizationTest.class);

        // $JUnit-END$
        return suite;
    }

    public static void main(String args[]) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
