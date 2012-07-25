package ws.palladian.extraction.date.getter;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.helper.DateFormat;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.dates.DateParser;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.date.dates.UrlDate;

/**
 * 
 * This class searches for dates in a url-tring.<br>
 * Therefore it uses other regular expression then other techniques.
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public class UrlDateGetter extends TechniqueDateGetter<UrlDate> {

    @Override
    public List<UrlDate> getDates() {
        List<UrlDate> result = new ArrayList<UrlDate>();
        if (url != null) {
            result.add(getUrlDate(url));
        }
        return result;
    }

    /**
     * <p>
     * A URL has only one date, retrieve it using this method.
     * </p>
     * 
     * @return The {@link UrlDate} of this URL, or <code>null</code> if no date was extracted.
     */
    public UrlDate getFirstDate() {
        UrlDate date = null;
        if (url != null) {
            date = getUrlDate(url);
        }
        return date;

    }

    /**
     * <p>
     * A URL has only one date, retrieve it for the specified URL.
     * </p>
     * 
     * @param url The URL from which to retrieve the date.
     * @return The {@link UrlDate} of the specified URL, or <code>null</code> if no date was extracted.
     */
    public UrlDate getFirstDate(String url) {
        UrlDate date = null;
        if (url != null) {
            date = getUrlDate(url);
        }
        return date;
    }

    /**
     * Looks up for a date in the URL.
     * 
     * @param url
     * @return a extracted Date
     */
    private UrlDate getUrlDate(String url) {
        UrlDate ret = null;
        DateFormat[] regExpArray = RegExp.URL_DATES;
        int index = 0;
        ExtractedDate date = null;
        while (date == null && index < regExpArray.length) {
            date = DateParser.getDateFromString(url, regExpArray[index]);
            index++;
        }
        if (date != null) {
            ret = new UrlDate(date, url);
        }
        return ret;
    }

}
