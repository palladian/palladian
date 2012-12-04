package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.InputSource;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.ParserException;

public class WikimetaNerTest {
    
    private static String ORIGINAL_TEXT;
    private static File XML_FILE;

    @BeforeClass
    public static void loadData() throws FileNotFoundException {
        ORIGINAL_TEXT = FileHelper.readFileToString(ResourceHelper.getResourceFile("NewsSampleText.txt"));
        XML_FILE = ResourceHelper.getResourceFile("WikimetaResponse.xml");
    }

    @Test
    public void testParseApiResponse() throws FileNotFoundException, ParserException {
        WikimetaNer wikimetaNer = new WikimetaNer();
        InputSource inputSource = new InputSource(new FileInputStream(XML_FILE));
        Annotations annotations = wikimetaNer.parseXml(inputSource, ORIGINAL_TEXT);

        assertEquals(134, annotations.size());
        assertEquals("eastern United States", annotations.get(2).getEntity());

        assertEquals("LOC", annotations.get(2).getMostLikelyTagName());

        assertEquals(101, annotations.get(2).getOffset());
        assertEquals(21, annotations.get(2).getLength());

    }
    
    @Test
    public void testTagging() throws FileNotFoundException, ParserException {
        WikimetaNer wikimetaNer = new WikimetaNer();
        InputSource inputSource = new InputSource(new FileInputStream(XML_FILE));
        Annotations annotations = wikimetaNer.parseXml(inputSource, ORIGINAL_TEXT);
        // make sure this stupid alignment error doesn't show up, it should throw an exception though
        // (see NamedEntityRecognizer, line 282)
        wikimetaNer.tagText(ORIGINAL_TEXT, annotations);
    }

}
