package ws.palladian.daterecognition.searchEngine;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.searchengine.HakiaDateGetter;

public class HakiaDateGetterTEst {
    private static final Logger LOGGER = Logger.getLogger(HakiaDateGetterTEst.class);

	@Test
	@Ignore
	public void testGetHakiaDate() {
		String url = "http://www.afriquejet.com/news/international-news/final-of-the-2010-twenty20-cricket-world-cup-2010051849532.html";
		HakiaDateGetter dg = new HakiaDateGetter();
		ExtractedDate date = dg.getHakiaDate(url);
		LOGGER.info(date.getNormalizedDateString());
	}

}
