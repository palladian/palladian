package tud.iir.preprocessing.segmentation;

import java.io.IOException;
import java.util.ArrayList;
import javax.xml.parsers.ParserConfigurationException;

import tud.iir.preprocessing.segmentation.PageSegmenter;
import tud.iir.preprocessing.segmentation.Segment;
import tud.iir.web.Crawler;
import junit.framework.TestCase;

import org.w3c.dom.Document;


/**
 * Test cases for the PageSegmenter.
 * 
 * @author Silvio Rabe
 */
public class PageSegmenterTest extends TestCase {

    public PageSegmenterTest(String name) {
        super(name);
    }

    public void testSegmentation() throws ParserConfigurationException, IOException {
    	
        PageSegmenter seg = new PageSegmenter();
        seg.setDocument("data/test/pageSegmenter/forum_temp1.html");
        
        Crawler c = new Crawler();
        
        ArrayList<Document> simList = new ArrayList<Document>();
        simList.add(c.getWebDocument("data/test/pageSegmenter/forum_temp1_aehnlich1.html"));
        simList.add(c.getWebDocument("data/test/pageSegmenter/forum_temp1_aehnlich2.html"));
        simList.add(c.getWebDocument("data/test/pageSegmenter/forum_temp1_aehnlich3.html"));
        simList.add(c.getWebDocument("data/test/pageSegmenter/forum_temp1_aehnlich4.html"));
        simList.add(c.getWebDocument("data/test/pageSegmenter/forum_temp1_aehnlich5.html"));
        seg.setSimilarFiles(simList);
    	
        seg.startPageSegmentation();

        assertEquals("", Crawler.getDomain(""));

        assertEquals(407, seg.getAllSegments().size());
        assertEquals(276, seg.getSpecificSegments(Segment.Color.RED).size());
        assertEquals(12, +seg.getSpecificSegments(Segment.Color.LIGHTRED).size());
        assertEquals(19, seg.getSpecificSegments(Segment.Color.REDYELLOW).size());
        assertEquals(17, seg.getSpecificSegments(Segment.Color.YELLOW).size());
        assertEquals(16, seg.getSpecificSegments(Segment.Color.GREENYELLOW).size());
        assertEquals(2, seg.getSpecificSegments(Segment.Color.LIGHTGREEN).size());
        assertEquals(65, seg.getSpecificSegments(Segment.Color.GREEN).size());
        
        assertEquals(67, seg.getSpecificSegments(0.0, 0.3).size());
        assertEquals(33, seg.getSpecificSegments(0.3, 0.6).size());
        assertEquals(100, seg.getSpecificSegments(0.0, 0.6).size());
        assertEquals(219, seg.getSpecificSegments(0.95, 1.0).size());
    	
        assertEquals(169, seg.makeMutual(seg.getSpecificSegments(Segment.Color.RED),1).size());
        assertEquals(2, seg.makeMutual(seg.getSpecificSegments(Segment.Color.YELLOW),1).size());
        assertEquals(163, seg.makeMutual(seg.getSpecificSegments(0.7, 0.8),1).size());


    }




}
