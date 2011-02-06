/**
 * Created on: 05.02.2011 08:21:01
 */
package tud.iir.preprocessing.nlp;

import opennlp.tools.parser.Parse;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

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
        fixture = IOUtils.toString(ParserTest.class.getResource("/texts/contribution01.txt").openStream());
    }

    /**
     * <p>
     * Tests the implementation of the natural language parser based on the OpenNLP toolsuite.
     * </p>
     * 
     * @throws Exception If some exception occurs the test fails.
     */
    @Test
    public void testOpenNLPParser() throws Exception {
        OpenNLPParser objectOfClassUnderTest = new OpenNLPParser();
        objectOfClassUnderTest.loadDefaultModel();
        objectOfClassUnderTest.parse(fixture);
        Parse result = objectOfClassUnderTest.getParse();
        System.out.println(result);

    }
}
