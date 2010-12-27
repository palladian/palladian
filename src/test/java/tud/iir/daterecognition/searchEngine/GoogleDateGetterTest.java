package tud.iir.daterecognition.searchEngine;

import static org.junit.Assert.*;

import org.junit.Test;

import tud.iir.control.AllTests;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.searchengine.GoogleDateGetter;

public class GoogleDateGetterTest {

	@Test
	public void testGetGoogleDate() {
		if(AllTests.ALL_TESTS){
			String url = "http://www.spiegel.de/politik/deutschland/0,1518,731921,00.html";
			//url = "http://www.patrickswayze.net/";
			GoogleDateGetter gd = new GoogleDateGetter();
			ExtractedDate date = gd.getGoogleDate(url);
			
			assertEquals("2010-11-30", date.getNormalizedDateString());
		}
	}

}
