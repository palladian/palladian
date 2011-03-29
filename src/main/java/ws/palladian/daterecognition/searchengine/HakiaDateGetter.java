package ws.palladian.daterecognition.searchengine;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.helper.RegExp;
import ws.palladian.web.DocumentRetriever;
import ws.palladian.web.SourceRetriever;
import ws.palladian.web.SourceRetrieverManager;
import ws.palladian.web.WebResult;

public class HakiaDateGetter {
	//private String hakiaAPI = "http://hakia.com/search?q=";
	private String url;
	private String title = null;
	private DocumentRetriever crawler = new DocumentRetriever();
	
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
