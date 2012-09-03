package ws.palladian.extraction.date.dates;

import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.date.ExtractedDateImpl;

/**
 * Represents a date found in an url-string.
 * 
 * @author Martin Greogr
 * 
 */
public final class UrlDate extends ExtractedDateImpl {
    
    private final String url; 

    public UrlDate(ExtractedDate date, String url) {
        super(date);
        this.url = url;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }
    
    
}
