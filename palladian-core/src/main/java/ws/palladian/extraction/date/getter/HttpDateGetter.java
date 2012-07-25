package ws.palladian.extraction.date.getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.date.KeyWords;
import ws.palladian.helper.DateFormat;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.dates.DateParser;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.date.dates.MetaDate;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;

/**
 * 
 * This class finds dates in HTTP-connection.
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public class HttpDateGetter extends TechniqueDateGetter<MetaDate> {

    @Override
    public List<MetaDate> getDates() {
        List<MetaDate> result = new ArrayList<MetaDate>();
        if (url != null) {
            try {
                HttpResult httpResult = performHttpHead(url);
                result = getHttpHeaderDate(httpResult);
            } catch (HttpException e) {
            }
        }
        return result;
    }
    
    private HttpResult performHttpHead(String url) throws HttpException {
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        return httpRetriever.httpHead(url);
    }

    public List<MetaDate> getHttpHeaderDate(HttpResult httpResult) {
        List<MetaDate> result = new ArrayList<MetaDate>();
        Map<String, List<String>> headers = httpResult.getHeaders();
        String[] keywords = KeyWords.HTTP_KEYWORDS;
        for (int i = 0; i < keywords.length; i++) {
            result.addAll(checkHttpTags(keywords[i], headers));
        }
        return result;
    }

//    /**
//     * Extracts date form HTTP-header.<br>
//     * Look up only in tags with keywords of {@link KeyWords#HTTP_KEYWORDS}.
//     * 
//     * @param url
//     * @return The extracted Date.
//     */
//    private static ArrayList<MetaDate> getHTTPHeaderDate(String url) {
//        ArrayList<MetaDate> result = new ArrayList<MetaDate>();
//        HttpRetriever retriever = new HttpRetriever();
//        Map<String, List<String>> headers = new  HashMap<String, List<String>>();
//        if(url.indexOf("http") != -1){
//        	headers = retriever.getHeaders(url);
//        }
//        String[] keywords = KeyWords.HTTP_KEYWORDS;
//        for (int i = 0; i < keywords.length; i++) {
//            ArrayList<MetaDate> temp = checkHttpTags(keywords[i], headers);
//            if (temp != null) {
//                result.addAll(temp);
//            }
//        }
//        return result;
//    }

    /**
     * Look up for date in tag that has specified keyword.<br>
     * 
     * @param keyword To look for
     * @param headers Map of headers.
     * @return HTTP-date.
     */
    private static List<MetaDate> checkHttpTags(String keyword, Map<String, List<String>> headers) {
        List<MetaDate> result = new ArrayList<MetaDate>();
        DateFormat[] regExpArray = RegExp.HTTP_DATES;
        ExtractedDate date = null;
        if (headers.containsKey(keyword)) {
            List<String> dateList = headers.get(keyword);
            for(String dateString : dateList) {
                int index = 0;
                while (date == null && index < regExpArray.length) {
                    date = DateParser.getDateFromString(dateString, regExpArray[index]);
                    index++;
                }
                if (date != null) {
                    result.add(new MetaDate(date, keyword));
                }
                
            }
            
//            Iterator<String> dateListIterator = dateList.iterator();
//            while (dateListIterator.hasNext()) {
//                String dateString = dateListIterator.next().toString();
//                int index = 0;
//                while (date == null && index < regExpArray.length) {
//                    date = DateGetterHelper.getDateFromString(dateString, regExpArray[index]);
//                    index++;
//                }
//                if (date != null) {
//                    result.add(new MetaDate(date, keyword));
//                }
//            }
        }
        return result;
    }

}
