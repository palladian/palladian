package tud.irr.daterecognition;

import org.w3c.dom.Document;

import tud.iir.helper.DateHelper;
import tud.iir.web.Crawler;

public class DateGetter {
	
	String url;
	Document document;
	
	
	public DateGetter(String url){
		this.url =url;
	}
	
	public long[][] getDate(){
		
		long[][] returnValues=new long[1][2];
		
		Crawler c = new Crawler();
		this.document = c.getWebDocument(this.url, false);
		c.getHeaders(this.url);
		
		String[] urlDate = DateGetterHelper.getURLDate(url);
		
		returnValues[0][0] = DateHelper.getTimestamp(urlDate[0]);
		if(urlDate[1].equals("false"))
			returnValues[0][1]=100;
		else
			returnValues[0][1]=80;
		
		return  returnValues;
		
		
		
	}
}
