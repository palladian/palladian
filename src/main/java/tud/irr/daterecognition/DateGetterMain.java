package tud.irr.daterecognition;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.w3c.dom.Document;

import tud.iir.helper.DateHelper;
import tud.iir.web.Crawler;
import tud.iir.web.CrawlerCallback;

public class DateGetterMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String url1 = "http://blog.zeit.de/newsblog/2010/06/30/live-die-wahl-des-bundesprasidenten/";
		String url2 ="http://www.zeit.de/politik/deutschland/2010-06/neuer-bundespraesident";
		DateGetter dg = new DateGetter(url1);
		Calendar c = new GregorianCalendar();
		long[][] dateArray = dg.getDate();
		
		for(int i=0; i<dateArray.length; i++){
			c.setTimeInMillis(dateArray[i][0]);
			System.out.print("Date: " + c.getTime().toString());
			System.out.println(" - Bewertung: " + dateArray[i][1]);
		}
		
		dg = new DateGetter(url2);
		c = new GregorianCalendar();
		dateArray = dg.getDate();
		
		for(int i=0; i<dateArray.length; i++){
			c.setTimeInMillis(dateArray[i][0]);
			System.out.print("Date: " + c.getTime().toString());
			System.out.println(" - Bewertung: " + dateArray[i][1]);
		}
		

		
		

	}

}
