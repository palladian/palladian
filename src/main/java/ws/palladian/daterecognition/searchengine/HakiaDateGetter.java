package ws.palladian.daterecognition.searchengine;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.lowagie.text.html.WebColors;

import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.helper.HTMLHelper;
import ws.palladian.helper.RegExp;
import ws.palladian.web.Crawler;
import ws.palladian.web.SourceRetriever;
import ws.palladian.web.SourceRetrieverManager;
import ws.palladian.web.WebResult;

public class HakiaDateGetter {
	private String hakiaAPI = "http://hakia.com/search?q=";
	private String url;
	private String title = null;
	private Crawler crawler = new Crawler();
	
	private void setTitle(){
		Document doc = crawler.getWebDocument(url);
		if(doc != null){
			NodeList titleList = doc.getElementsByTagName("title");
			String title = "";
			if(titleList.getLength()>0){
				title=titleList.item(0).getTextContent();
				title = title.replaceAll("\\s", "%20");
			}
			this.title = title;
		}
	}
	
	
	private ExtractedDate getDateFromHakia(){
		ExtractedDate date = null;
		SourceRetriever sr = new SourceRetriever();
		sr.setResultCount(100);
		List<WebResult> wr = sr.getWebResults(title, SourceRetrieverManager.HAKIA_NEWS, false);
		
		for(int i=0; i<wr.size(); i++){
			WebResult result = wr.get(i);
			String tempUrl  = result.getUrl();
			String requestURL= crawler.getRedirectUrl(tempUrl);
			if(requestURL != null && requestURL.equalsIgnoreCase(url)){
				date = DateGetterHelper.getDateFromString(result.getDate(), RegExp.DATE_USA_MM_D_Y_T_SEPARATOR);
				break;
			}
			
		}
		return date;
	}
	
	public ExtractedDate getHakiaDate(String url){
		this.url = url;
		setTitle();
		ExtractedDate date = null;
		if(title != null){
			date = getDateFromHakia();
		}
		return date;
	}
	
	
	
}
