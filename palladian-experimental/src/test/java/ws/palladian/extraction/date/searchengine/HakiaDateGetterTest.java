package ws.palladian.extraction.date.searchengine;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.date.ExtractedDate;

public class HakiaDateGetterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(HakiaDateGetterTest.class);

    @Test
    @Ignore
    public void testGetHakiaDate() {
        String url = "http://www.afriquejet.com/news/international-news/final-of-the-2010-twenty20-cricket-world-cup-2010051849532.html";
        HakiaDateGetter dg = new HakiaDateGetter();
        ExtractedDate date = dg.getHakiaDate(url);
        LOGGER.info(date.getNormalizedDateString());
    }

}
