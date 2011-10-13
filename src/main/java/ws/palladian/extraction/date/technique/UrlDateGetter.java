package ws.palladian.extraction.date.technique;

import java.util.ArrayList;

import ws.palladian.extraction.date.DateConverter;
import ws.palladian.extraction.date.DateGetterHelper;
import ws.palladian.extraction.date.dates.DateType;
import ws.palladian.extraction.date.dates.ExtractedDate;
import ws.palladian.extraction.date.dates.UrlDate;
import ws.palladian.helper.RegExp;

/**
 * 
 * This class searches for dates in a url-tring.<br>
 * Therefore it uses other regular expression then other techniques.
 * 
 * @author Martin Gregor
 * 
 */
public class UrlDateGetter extends TechniqueDateGetter<UrlDate> {

    @Override
    public ArrayList<UrlDate> getDates() {
        ArrayList<UrlDate> result = new ArrayList<UrlDate>();
        if (url != null) {
            result.add(getURLDate(url));
        }
        return result;
    }

    /**
     * An url has only one date. So first date is this one. <br>
     * Use setUrl before.
     * @return
     */
    public UrlDate getFirstDate() {

        UrlDate date = new UrlDate();
        if (url != null) {
            date = getURLDate(url);
        }
        return date;

    }
    /**
     * An url has only one date. So first date is this one.
     * 
     * @return
     */
    public UrlDate getFirstDate(String url) {

        UrlDate date = new UrlDate();
        if (url != null) {
            date = getURLDate(url);
        }
        return date;

    }

    /**
     *Looks up for a date in the URL.
     * 
     * @param url
     * @return a extracted Date
     */
    private UrlDate getURLDate(String url) {
        ExtractedDate date = null;
        UrlDate temp = null;
        Object[] regExpArray = RegExp.getURLRegExp();
        int index = 0;
        while (date == null && index < regExpArray.length) {
            date = DateGetterHelper.getDateFromString(url, (String[]) regExpArray[index]);
            index++;
        }
        if (date != null) {
            temp = DateConverter.convert(date, DateType.UrlDate);
            temp.setUrl(url);
        }
        return temp;
    }

}
