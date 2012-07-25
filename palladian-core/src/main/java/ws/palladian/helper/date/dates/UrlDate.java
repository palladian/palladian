package ws.palladian.helper.date.dates;

import ws.palladian.helper.date.ExtractedDate;

/**
 * Represents a date found in an url-string.
 * 
 * @author Martin Greogr
 * 
 */
public final class UrlDate extends ExtractedDate {
    
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
