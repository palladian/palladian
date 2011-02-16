package tud.iir.daterecognition.searchengine;

import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import tud.iir.daterecognition.DateGetterHelper;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.XPathHelper;
import tud.iir.web.Crawler;
import tud.iir.web.SourceRetriever;

public class AskDateGetter {
	private String googleAPI ="http://de.ask.com/web?q=";
	private Crawler crawler = new  Crawler();
	private String url;
	
	private Document getAskPage(){
		Document askPage = crawler.getWebDocument(googleAPI + url);	
		return askPage;
	}
	
	private ExtractedDate getDateOfAskPage(Document doc){
		NodeList aList = doc.getElementsByTagName("notrim");
		Node dateDiv = null;
		ExtractedDate date = null;
		
		for(int i=0; i <aList.getLength(); i++){
			Node div = XPathHelper.getChildNodeByID(doc, "r" + i + "_t");
			
			if(div != null){
				NamedNodeMap attr = div.getAttributes();
				Node href = attr.getNamedItem("href");
				if(href != null){
					if(href.getNodeValue().equalsIgnoreCase(url)){
						dateDiv = XPathHelper.getChildNodeByID(doc, "r" + i + "_a");
						break;
					}
				}	
			}
		}
		
		if(dateDiv != null){
			String divText = HTMLHelper.htmlToReadableText(dateDiv);
			int index = divText.indexOf("...");
			if(index != -1){
				String dateString = divText.substring(0, index);
				date = DateGetterHelper.findDate(dateString);
			}
		}
		return date;
	}
	
	public ExtractedDate getAskDate(String url){
		this.url=url;
		Document doc = getAskPage();
		ExtractedDate extractedDate = null;
		if(doc != null){
			extractedDate = getDateOfAskPage(doc);
		}
		return extractedDate;
	}
}
