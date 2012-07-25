package ws.palladian.extraction.date.searchengine;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ws.palladian.helper.date.dates.DateParser;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.DocumentRetriever;

public class AskDateGetter {
	private String googleAPI ="http://de.ask.com/web?q=";
	private DocumentRetriever crawler = new  DocumentRetriever();
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
			Node div = XPathHelper.getNodeByID(doc, "r" + i + "_t");
			
			if(div != null){
				NamedNodeMap attr = div.getAttributes();
				Node href = attr.getNamedItem("href");
				if(href != null){
					if(href.getNodeValue().equalsIgnoreCase(url)){
						dateDiv = XPathHelper.getNodeByID(doc, "r" + i + "_a");
						break;
					}
				}	
			}
		}
		
		if(dateDiv != null){
			String divText = HtmlHelper.documentToReadableText(dateDiv);
			int index = divText.indexOf("...");
			if(index != -1){
				String dateString = divText.substring(0, index);
				date = DateParser.findDate(dateString);
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
