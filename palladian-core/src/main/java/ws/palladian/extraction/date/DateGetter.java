package ws.palladian.extraction.date;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;

import ws.palladian.extraction.date.getter.ContentDateGetter;
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

    private boolean techHtmlContent = true;

	private final ContentDateGetter cdg = new ContentDateGetter();

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
	public List<ExtractedDate> getDate() {

		List<ExtractedDate> dates = new ArrayList<ExtractedDate>();
		DocumentRetriever crawler = new DocumentRetriever();

        if (url != null && techHtmlContent) {
			Document document = this.document;
			if (document == null) {
				document = crawler.getWebDocument(url);
			}
			if (document != null) {
				cdg.setDocument(document);
				dates.addAll(cdg.getDates());
			}
		}
        dates.removeAll(Collections.singletonList(null));
		return dates;

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
        techHtmlContent = value;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

}
