/**
 * Created on: 05.02.2011 08:21:01
 */
package ws.palladian.extraction;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

/**
 * <p>
 * Tests if all implementations of the natural language parser work correctly.
 * </p>
 * 
 * @author Klemens Muthmann
 * 
 */
public class ParserTest {
    /**
     * <p>
     * Testdata for the object of the class under test.
     * </p>
     */
    private String fixture;

    /**
     * <p>
     * Initializes the test fixture with data from <tt>src/test/resources/texts</tt>.
     * </p>
     * 
     * @throws Exception If some exception occurs during initialization the test is stopped and fails.
     */
    @Before
    public void setUp() throws Exception {
        fixture = FileHelper.readFileToString(ResourceHelper.getResourceFile("/texts/contribution01.txt"));
    }

    // TODO add some small models for testing purposes. This test will only work with a valid model but existing examples are way too large.
    /**
     * <p>
     * Tests the implementation of the natural language parser based on the OpenNLP toolsuite.
     * </p>
     * 
     * @throws Exception If some exception occurs the test fails.
     */
    @Test
    @Ignore
    public void testOpenNLPParser() throws Exception {
//        OpenNlpParser objectOfClassUnderTest = new OpenNlpParser();
//        objectOfClassUnderTest.loadDefaultModel();
//        objectOfClassUnderTest.parse(fixture);
//        Parse result = objectOfClassUnderTest.getParse();
//        System.out.println(result);
//
    }
}
