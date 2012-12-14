package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

public class VimeoSearcherTest {
    
    @Test
    public void testParseJson()throws Exception {
        String jsonString = FileHelper.readFileToString(ResourceHelper.getResourceFile("/apiresponse/vimeo.json"));
        List<WebVideoResult> videoResults = VimeoSearcher.parse(jsonString);
        assertEquals(50, videoResults.size());
    }

}
