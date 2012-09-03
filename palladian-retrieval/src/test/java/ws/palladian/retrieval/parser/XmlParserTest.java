package ws.palladian.retrieval.parser;

import java.io.FileNotFoundException;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.helper.io.ResourceHelper;

public class XmlParserTest {
    
    private XmlParser xmlParser;

    @Before
    public void setUp() {
        xmlParser = new XmlParser();
    }
    
    /**
     * <p>Test for parsing faulty and ill-formed XML.</p>
     * @throws FileNotFoundException
     * @throws ParserException
     */
    @Test
    public void testParseInvalidXml() throws FileNotFoundException, ParserException {
        xmlParser.parse(ResourceHelper.getResourceFile("/xmlDocuments/invalid-chars.xml"));
        xmlParser.parse(ResourceHelper.getResourceFile("/feeds/sourceforge02.xml"));
        xmlParser.parse(ResourceHelper.getResourceFile("/feeds/feed061.xml"));
    }

}
