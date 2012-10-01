package ws.palladian.extraction.date.searchengine;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.helper.date.ExtractedDate;

public class GoogleDateGetterTest {

    @Test
    @Ignore
    public void testGetGoogleDate() {
        String url = "http://www.spiegel.de/politik/deutschland/0,1518,731921,00.html";
        // url = "http://www.patrickswayze.net/";
        GoogleDateGetter gd = new GoogleDateGetter();
        ExtractedDate date = gd.getGoogleDate(url);

        assertEquals("2010-11-30", date.getNormalizedDateString());
    }

}
