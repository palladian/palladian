package tud.iir.daterecognition.searchengine;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import tud.iir.daterecognition.DateGetterHelper;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.GoogleDate;
import tud.iir.helper.HTMLHelper;
import tud.iir.web.Crawler;

/**
 * 
 * @author Martin Gregor
 * This class is responsible for finding a google-date by using google-search on  an URL.
 */
public class GoogleDateGetter {

	private String googleAPI ="http://www.google.de/search?q=";
	private Crawler crawler = new  Crawler();
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
					String divText = HTMLHelper.documentToReadableText(dateDiv);
					int index = divText.indexOf("...");
					if(index != -1){
						String dateString = divText.substring(0, index);
						date = DateGetterHelper.findDate(dateString);
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
