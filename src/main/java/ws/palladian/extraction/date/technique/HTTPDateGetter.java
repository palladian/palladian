package ws.palladian.extraction.date.technique;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.date.DateConverter;
import ws.palladian.extraction.date.DateGetterHelper;
import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.dates.DateType;
import ws.palladian.extraction.date.dates.ExtractedDate;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.helper.RegExp;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * 
 * This class finds dates in HTTP-connection.
 * 
 * @author Martin Gregor
 * 
 */
public class HTTPDateGetter extends TechniqueDateGetter<MetaDate> {

    @Override
    public ArrayList<MetaDate> getDates() {
        ArrayList<MetaDate> result = new ArrayList<MetaDate>();
        if (url != null) {
            result = getHTTPHeaderDate(url);
        }
        return result;
    }

    /**
     * Extracts date form HTTP-header.<br>
     * Look up only in tags with keywords of {@link KeyWords#HTTP_KEYWORDS}.
     * 
     * @param url
     * @return The extracted Date.
     */
    private static ArrayList<MetaDate> getHTTPHeaderDate(String url) {
        ArrayList<MetaDate> result = new ArrayList<MetaDate>();
        DocumentRetriever crawler = new DocumentRetriever();
        Map<String, List<String>> headers = new  HashMap<String, List<String>>();
        if(url.indexOf("http") != -1){
        	headers = crawler.getHeaders(url);
        }
        String[] keywords = KeyWords.HTTP_KEYWORDS;
        for (int i = 0; i < keywords.length; i++) {
            ArrayList<MetaDate> temp = checkHttpTags(keywords[i], headers);
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
    private static ArrayList<MetaDate> checkHttpTags(String keyword, Map<String, List<String>> headers) {
        ArrayList<MetaDate> result = new ArrayList<MetaDate>();
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
                	MetaDate httpDate = DateConverter.convert(date, DateType.MetaDate);
                    // HTTPDate httpDate = DateConverter.convertToHTTPDate(date);
                    httpDate.setKeyword(keyword);
                    result.add(httpDate);
                }
            }
        }
        return result;
    }

}
