package ws.palladian.extraction.date;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import ws.palladian.extraction.date.getter.ContentDateGetter;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * <p>
 * This class is responsible for rating dates.
 * </p>
 * <p>
 * Therefore it coordinates each technique-rater-class.
 * </p>
 * 
 * @author Martin Gregor (mail@m-gregor.de)
 * 
 * 
 */
public class DateGetter {

	private final ContentDateGetter contentDateGetter = new ContentDateGetter();

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
	public DateGetter(String url) {
		this.url = url;
	}

	/**
	 * Constructor creates a new DateGetter with a given URL and document.
	 * 
	 * @param url
	 *            URL that will be analyzed
	 */
	public DateGetter(String url, Document document) {
		this.url = url;
		this.document = document;
	}

	/**
	 * Analyzes a webpage by different techniques to find dates.
	 * 
	 * @param <T>
	 * 
	 * @return A array of ExtractedDates.
	 */
	public List<ExtractedDate> getDate() {

		List<ExtractedDate> dates = new ArrayList<ExtractedDate>();
		DocumentRetriever crawler = new DocumentRetriever();

        if (url != null) {
			Document document = this.document;
			if (document == null) {
				document = crawler.getWebDocument(url);
			}
			if (document != null) {
				contentDateGetter.setDocument(document);
				dates.addAll(contentDateGetter.getDates());
			}
		}
        CollectionHelper.removeNulls(dates);
		return dates;

	}

	/**
	 * Setter for global variable URL.
	 * 
	 * @return URL.
	 */
	public void setURL(String url) {
		this.url = url;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

}
