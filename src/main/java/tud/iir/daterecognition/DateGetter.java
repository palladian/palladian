package tud.iir.daterecognition;

import java.util.LinkedList;

import tud.iir.web.Crawler;

/**
 * DateGetter provides methods for getting dates from URL and rate them.
 * 
 * @author Martin Gregor (mail@m-gregor.de)
 * 
 * 
 */
public class DateGetter {

    /** URL that will be called */
    private String url;

    /**
     * Constructor creates a new DateGetter with a given URL.
     * 
     * @param url URL that will be analyzed
     */
    public DateGetter(final String url) {
        this.url = url;
    }

    /**
     * Analyzes a webpage by different techniques to find dates. The techniques are found in DateGetterHelper. <br>
     * Type of the found dates are ExtractedDate.
     * 
     * @return A array of ExtractedDates.
     */
    public ExtractedDate[] getDate() {

        final LinkedList<ExtractedDate> dates = new LinkedList<ExtractedDate>();
        final Crawler crawler = new Crawler();
        // final Document document = crawler.getWebDocument(this.url, false);

        crawler.getHeaders(this.url);
        final ExtractedDate urlDate = DateGetterHelper.getURLDate(url);
        if (urlDate != null) {
            dates.add(urlDate);
        }

        return dates.toArray(new ExtractedDate[dates.size()]);

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
    public void stURL(final String url) {
        this.url = url;
    }
}
