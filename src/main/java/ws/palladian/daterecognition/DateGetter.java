package ws.palladian.daterecognition;

import java.util.ArrayList;
import java.util.Collection;

import org.w3c.dom.Document;

import ws.palladian.daterecognition.dates.AbstractDate;
import ws.palladian.daterecognition.dates.MetaDate;
import ws.palladian.daterecognition.technique.ArchiveDateGetter;
import ws.palladian.daterecognition.technique.ContentDateGetter;
import ws.palladian.daterecognition.technique.HTTPDateGetter;
import ws.palladian.daterecognition.technique.HeadDateGetter;
import ws.palladian.daterecognition.technique.MetaDateGetter;
import ws.palladian.daterecognition.technique.ReferenceDateGetter;
import ws.palladian.daterecognition.technique.StructureDateGetter;
import ws.palladian.daterecognition.technique.URLDateGetter;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * This class is responsible for rating dates. <br>
 * Therefore it coordinates each technique-rater-class. <br>
 * 
 * @author Martin Gregor (mail@m-gregor.de)
 * @param <T>
 * 
 * 
 */
public class DateGetter {

	private boolean tech_HTML_content = true;

	private ContentDateGetter cdg = new ContentDateGetter();

	/** URL that will be called */
	private String url;

	private Document document;

	public DateGetter() {
		super();
	}

	/**
	 * Constructor creates a new DateGetter with a given URL.
	 * 
	 * @param url
	 *            URL that will be analyzed
	 */
	public DateGetter(final String url) {
		this.url = url;
	}

	/**
	 * Constructor creates a new DateGetter with a given document.
	 * 
	 * @param url
	 *            URL that will be analyzed
	 */
	public DateGetter(final Document document) {
		this.document = document;
	}

	/**
	 * Constructor creates a new DateGetter with a given URL and document.
	 * 
	 * @param url
	 *            URL that will be analyzed
	 */
	public DateGetter(final String url, Document document) {
		this.url = url;
		this.document = document;
	}

	/**
	 * Analyzes a webpage by different techniques to find dates. The techniques
	 * are found in DateGetterHelper. <br>
	 * Type of the found dates are ExtractedDate.
	 * 
	 * @param <T>
	 * 
	 * @return A array of ExtractedDates.
	 */
	@SuppressWarnings("unchecked")
	public <T> ArrayList<T> getDate() {

		ArrayList<T> dates = new ArrayList<T>();
		DocumentRetriever crawler = new DocumentRetriever();

		if (url != null && tech_HTML_content) {
			Document document = this.document;
			if (document == null) {
				document = crawler.getWebDocument(url);
			}
			if (document != null) {
				cdg.setDocument(document);
				dates.addAll((Collection<? extends T>) cdg.getDates());
			}
		}
		dates = CollectionHelper.removeNullElements(dates);
		return dates;

	}

	/**
	 * Getter for global variable URL.
	 * 
	 * @return URL.
	 */
	public String getURL() {
		return this.url;
	}

	/**
	 * Setter for global variable URL.
	 * 
	 * @return URL.
	 */
	public void setURL(final String url) {
		this.url = url;
	}

	/**
	 * Activate or disable HTML-content-technique.
	 * 
	 * @param value
	 */
	public void setTechHTMLContent(boolean value) {
		tech_HTML_content = value;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public Document getDocument() {
		return this.document;
	}

}
