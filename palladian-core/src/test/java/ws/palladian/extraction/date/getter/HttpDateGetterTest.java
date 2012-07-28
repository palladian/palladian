package ws.palladian.extraction.date.getter;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;

public class HttpDateGetterTest {
    
    @Test
    public void testGetHttpHeaderDate() throws FileNotFoundException {
        HttpResult httpResult = HttpRetriever.loadSerializedHttpResult(ResourceHelper.getResourceFile("/httpResults/testPage01.httpResult"));
        
        HttpDateGetter httpDateGetter = new HttpDateGetter();
        List<MetaDate> dates = httpDateGetter.getHttpHeaderDate(httpResult);
        
        assertEquals("2012-07-22 14:35:38", dates.get(0).getNormalizedDateString());
        assertEquals("2012-07-22 13:59:10", dates.get(1).getNormalizedDateString());

    }

}
