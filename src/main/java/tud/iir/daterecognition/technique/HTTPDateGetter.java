package tud.iir.daterecognition.technique;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import tud.iir.daterecognition.DateConverter;
import tud.iir.daterecognition.DateGetterHelper;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.HTTPDate;
import tud.iir.knowledge.KeyWords;
import tud.iir.knowledge.RegExp;
import tud.iir.web.Crawler;

public class HTTPDateGetter extends TechniqueDateGetter<HTTPDate> {

    @Override
    public ArrayList<HTTPDate> getDates() {
        ArrayList<HTTPDate> result = new ArrayList<HTTPDate>();
        if (url != null) {
            result = getHTTPHeaderDate(url);
        }
        return result;
    }

    /**
     * Extracts date form HTTP-header, that is written in "Last-Modified"-tag.
     * 
     * @param url
     * @return The extracted Date.
     */
    private static ArrayList<HTTPDate> getHTTPHeaderDate(String url) {
        ArrayList<HTTPDate> result = new ArrayList<HTTPDate>();
        Crawler crawler = new Crawler();
        Map<String, List<String>> headers = crawler.getHeaders(url);
        String[] keywords = KeyWords.HTPP_KEYWORDS;
        for (int i = 0; i < keywords.length; i++) {
            ArrayList<HTTPDate> temp = checkHttpTags(keywords[i], headers);
            if (temp != null) {
                result.addAll(temp);
            }
        }
        return result;
    }

    private static ArrayList<HTTPDate> checkHttpTags(String keyword, Map<String, List<String>> headers) {
        ArrayList<HTTPDate> result = new ArrayList<HTTPDate>();
        Object[] regExpArray = RegExp.getHTTPRegExp();
        ExtractedDate date = null;
        if (headers.containsKey(keyword)) {
            List<String> dateList = headers.get(keyword);
            Iterator<String> dateListIterator = dateList.iterator();
            while (dateListIterator.hasNext()) {
                String dateString = dateListIterator.next().toString();
                int index = 0;
                while (date == null && index < regExpArray.length) {
                    date = DateGetterHelper.getDateFromString(dateString, (String[]) regExpArray[index]);
                    index++;
                }
                if (date != null) {
                    HTTPDate httpDate = DateConverter.convert(date, DateConverter.TECH_HTTP_HEADER);
                    // HTTPDate httpDate = DateConverter.convertToHTTPDate(date);
                    httpDate.setKeyword(keyword);
                    result.add(httpDate);
                }
            }
        }
        return result;
    }

}
