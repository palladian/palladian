package ws.palladian.daterecognition.technique;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ws.palladian.daterecognition.DateConverter;
import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.daterecognition.KeyWords;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.HTTPDate;
import ws.palladian.helper.RegExp;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * 
 * This class finds dates in HTTP-connection.
 * 
 * @author Martin Gregor
 * 
 */
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
     * Extracts date form HTTP-header.<br>
     * Look up only in tags with keywords of {@link KeyWords#HTPP_KEYWORDS}.
     * 
     * @param url
     * @return The extracted Date.
     */
    private static ArrayList<HTTPDate> getHTTPHeaderDate(String url) {
        ArrayList<HTTPDate> result = new ArrayList<HTTPDate>();
        DocumentRetriever crawler = new DocumentRetriever();
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

    /**
     * Look up for date in tag that has specified keyword.<br>
     * 
     * @param keyword To look for
     * @param headers Map of headers.
     * @return HTTP-date.
     */
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
