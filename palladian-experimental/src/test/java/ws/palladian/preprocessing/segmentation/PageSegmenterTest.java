package ws.palladian.preprocessing.segmentation;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * <p>
 * Test cases for the PageSegmenter.
 * </p>
 * 
 * @author Silvio Rabe
 * @author Philipp Katz
 */
public class PageSegmenterTest {

    private final DocumentParser htmlParser = ParserFactory.createHtmlParser();

    @Test
    public void testSegmentation() throws ParserConfigurationException, IOException, ParserException {

        PageSegmenter seg = new PageSegmenter();
        seg.setDocument(htmlParser.parse(ResourceHelper.getResourceFile("/pageSegmenter/forum_temp1.html")));

        List<Document> simList = new ArrayList<Document>();
        simList.add(htmlParser.parse(ResourceHelper.getResourceFile("/pageSegmenter/forum_temp1_aehnlich1.html")));
        simList.add(htmlParser.parse(ResourceHelper.getResourceFile("/pageSegmenter/forum_temp1_aehnlich2.html")));
        simList.add(htmlParser.parse(ResourceHelper.getResourceFile("/pageSegmenter/forum_temp1_aehnlich3.html")));
        simList.add(htmlParser.parse(ResourceHelper.getResourceFile("/pageSegmenter/forum_temp1_aehnlich4.html")));
        simList.add(htmlParser.parse(ResourceHelper.getResourceFile("/pageSegmenter/forum_temp1_aehnlich5.html")));
        seg.setSimilarFiles(simList);

        seg.startPageSegmentation();

        //
        // rem: I had to change those values after incorporating the fixed NekoHTML parser
        // 2012-04-24, Philipp
        //
        assertEquals(418, seg.getAllSegments().size());
        assertEquals(259, seg.getSpecificSegments(Segment.Color.RED).size());
        assertEquals(18, seg.getSpecificSegments(Segment.Color.LIGHTRED).size());
        assertEquals(16, seg.getSpecificSegments(Segment.Color.REDYELLOW).size());
        assertEquals(30, seg.getSpecificSegments(Segment.Color.YELLOW).size());
        assertEquals(18, seg.getSpecificSegments(Segment.Color.GREENYELLOW).size());
        assertEquals(2, seg.getSpecificSegments(Segment.Color.LIGHTGREEN).size());
        assertEquals(75, seg.getSpecificSegments(Segment.Color.GREEN).size());

        assertEquals(77, seg.getSpecificSegments(0.0, 0.3).size());
        assertEquals(48, seg.getSpecificSegments(0.3, 0.6).size());
        assertEquals(125, seg.getSpecificSegments(0.0, 0.6).size());
        assertEquals(211, seg.getSpecificSegments(0.95, 1.0).size());

        assertEquals(75, seg.makeMutual(seg.getSpecificSegments(Segment.Color.RED), 1).size());
        assertEquals(32, seg.makeMutual(seg.getSpecificSegments(Segment.Color.YELLOW), 1).size());
        assertEquals(150, seg.makeMutual(seg.getSpecificSegments(0.7, 0.8), 1).size());

    }

}
