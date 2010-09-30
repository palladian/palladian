package tud.iir.daterecognition.technique;

import java.util.ArrayList;

import tud.iir.daterecognition.DateConverter;
import tud.iir.daterecognition.DateGetterHelper;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.URLDate;
import tud.iir.knowledge.RegExp;

public class URLDateGetter extends TechniqueDateGetter<URLDate> {

    @Override
    public ArrayList<URLDate> getDates() {
        ArrayList<URLDate> result = new ArrayList<URLDate>();
        if (url != null) {
            result.add(getURLDate(url));
        }
        return result;
    }

    public URLDate getFirstDate() {

        URLDate date = new URLDate();
        if (url != null) {
            date = getURLDate(url);
        }
        return date;

    }

    /**
     * looks up for a date in the URL
     * 
     * @param url
     * @return a extracted Date
     */
    private URLDate getURLDate(String url) {
        ExtractedDate date = null;
        URLDate temp = null;
        Object[] regExpArray = RegExp.getURLRegExp();
        int index = 0;
        while (date == null && index < regExpArray.length) {
            date = DateGetterHelper.getDateFromString(url, (String[]) regExpArray[index]);
            index++;
        }
        if (date != null) {
            temp = DateConverter.convert(date, DateConverter.TECH_URL);
            temp.setUrl(url);
        }
        return temp;
    }

}
