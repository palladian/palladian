package tud.iir.daterecognition.searchEngine;

import static org.junit.Assert.*;

import org.junit.Test;

import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.searchengine.HakiaDateGetter;

public class HakiaDateGetterTEst {

	@Test
	public void testGetHakiaDate() {
		String url = "http://www.afriquejet.com/news/international-news/final-of-the-2010-twenty20-cricket-world-cup-2010051849532.html";
		HakiaDateGetter dg = new HakiaDateGetter();
		ExtractedDate date = dg.getHakiaDate(url);
		System.out.println(date.getNormalizedDateString());
	}

}
