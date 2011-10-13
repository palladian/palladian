package ws.palladian.extraction.date.searchengine;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.control.AllTests;
import ws.palladian.extraction.date.dates.ExtractedDate;
import ws.palladian.extraction.date.searchengine.HakiaDateGetter;

public class HakiaDateGetterTEst {
    private static final Logger LOGGER = Logger.getLogger(HakiaDateGetterTEst.class);

    @Ignore
	@Test
	public void testGetHakiaDate() {
        if (AllTests.ALL_TESTS) {
            String url = "http://www.afriquejet.com/news/international-news/final-of-the-2010-twenty20-cricket-world-cup-2010051849532.html";
            HakiaDateGetter dg = new HakiaDateGetter();
            ExtractedDate date = dg.getHakiaDate(url);
            LOGGER.info(date.getNormalizedDateString());
        }
	}

}
