package ws.palladian.extraction.date.searchengine;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import ws.palladian.extraction.date.DateGetterHelper;
import ws.palladian.extraction.date.dates.ExtractedDate;
import ws.palladian.helper.RegExp;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.search.WebSearcher;
import ws.palladian.retrieval.search.WebSearcherManager;
import ws.palladian.retrieval.search.WebResult;

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
		WebSearcher sr = new WebSearcher();
		sr.setResultCount(100);
		List<WebResult> wr = sr.getWebResults(title, WebSearcherManager.HAKIA_NEWS, false);
		
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
