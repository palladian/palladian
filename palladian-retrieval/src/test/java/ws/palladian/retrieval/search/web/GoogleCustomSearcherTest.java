package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.List;

import org.json.JSONException;
import org.junit.Test;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

public class GoogleCustomSearcherTest {
    
    @Test
    public void testParsing() throws FileNotFoundException, JSONException {
        String jsonString = FileHelper.readFileToString(ResourceHelper.getResourceFile("/apiresponse/googleCustomSearchResponse.json"));
        List<WebResult> results = GoogleCustomSearcher.parse(jsonString);
        assertEquals(10, results.size());
        
        assertEquals("Palladian architecture - Wikipedia, the free encyclopedia", results.get(0).getTitle());
        assertEquals("http://en.wikipedia.org/wiki/Palladian_architecture", results.get(0).getUrl());
        assertEquals("Palladian architecture is a European style of architecture derived from the   designs of the Venetian architect Andrea Palladio (1508–1580). The term \"  Palladian\" ...", results.get(0).getSummary());
        
        long resultCount = GoogleCustomSearcher.parseResultCount(jsonString);
        assertEquals(147000, resultCount);
    }

}
