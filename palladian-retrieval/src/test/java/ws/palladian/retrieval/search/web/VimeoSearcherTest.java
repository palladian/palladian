package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.search.videos.VimeoSearcher;
import ws.palladian.retrieval.search.videos.WebVideoResult;

public class VimeoSearcherTest {

    @Test
    public void testParseJson() throws Exception {
        String jsonString = FileHelper.readFileToString(ResourceHelper.getResourceFile("/apiresponse/vimeo.json"));
        List<WebVideoResult> videoResults = VimeoSearcher.parseVideoResult(jsonString);
        assertEquals(50, videoResults.size());

        WebVideoResult result1 = videoResults.get(0);
        assertEquals("Matta - Release The Freq", result1.getTitle());
        assertEquals(1299589789000l, result1.getPublished().getTime());
        assertEquals("https://vimeo.com/20800127", result1.getUrl());
        assertEquals(246, (long)result1.getRunTime());
        
        long resultCount = VimeoSearcher.parseResultCount(jsonString);
        assertEquals(94609, resultCount);
    }

}
