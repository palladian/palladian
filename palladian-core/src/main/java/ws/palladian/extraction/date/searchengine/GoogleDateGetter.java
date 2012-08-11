package ws.palladian.extraction.date.searchengine;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * This class is responsible for finding a google-date by using google-search on  an URL.
 * @author Martin Gregor
 */
public class GoogleDateGetter {

	private String googleAPI ="http://www.google.de/search?q=";
	private DocumentRetriever crawler = new  DocumentRetriever();
	private String url;
	
	private Document getGooglePage(){
		Document googlePage = crawler.getWebDocument(googleAPI + url);	
		return googlePage;
	}
	
	private ExtractedDate getDateOfGooglePage(Document doc){
		NodeList aList = doc.getElementsByTagName("a");
		Node dateDiv = null;
		ExtractedDate date = null;
		
		for(int i=0; i <aList.getLength(); i++){
			Node a = aList.item(i);
			NamedNodeMap attr = a.getAttributes();
			Node href = attr.getNamedItem("href");
			if(href != null){
				if(href.getNodeValue().equalsIgnoreCase(url)){
					dateDiv = a.getParentNode().getParentNode().getNextSibling();
					break;
				}
			}	
		}
		
		if(dateDiv != null){
			NamedNodeMap attr =dateDiv.getAttributes();
			Node classAttr = attr.getNamedItem("class");
			if(classAttr != null){
				if(classAttr.getNodeValue().equalsIgnoreCase("s")){
					String divText = HtmlHelper.documentToReadableText(dateDiv);
					int index = divText.indexOf("...");
					if(index != -1){
						String dateString = divText.substring(0, index);
						date = DateParser.findDate(dateString);
					}
				}
			}
		}
		
		return date;
	}
	
	public ExtractedDate getGoogleDate(String url){
		this.url=url;
		Document doc = getGooglePage();
		ExtractedDate extractedDate = getDateOfGooglePage(doc);
		return extractedDate;
	}
	
}
