package ws.palladian.extraction.date.getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.helper.constants.DateFormat;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpResult;

/**
 * <p>
 * This {@link TechniqueDateGetter} extracts dates from HTTP headers.
 * </p>
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public class HttpDateGetter extends TechniqueDateGetter<MetaDate> {

    @Override
    public List<MetaDate> getDates(HttpResult httpResult) {
        List<MetaDate> result = new ArrayList<MetaDate>();
        Map<String, List<String>> headers = httpResult.getHeaders();
        for (String keyword : KeyWords.HTTP_KEYWORDS) {
            result.addAll(checkHttpTags(keyword, headers));
        }
        return result;
    }

    @Override
    public List<MetaDate> getDates(Document document) {

        // get the http result without querying the URL again, this saves bandwidth and time
        HttpResult httpResult = (HttpResult)document.getUserData(DocumentRetriever.HTTP_RESULT_KEY);
        if (httpResult != null) {
            return getDates(httpResult);
        }

        return getDates(getUrl(document));
    }

    private static List<MetaDate> checkHttpTags(String keyword, Map<String, List<String>> headers) {
        List<MetaDate> result = new ArrayList<MetaDate>();
        if (headers.containsKey(keyword)) {
            List<String> dateList = headers.get(keyword);
            for (String dateString : dateList) {
                for (DateFormat dateFormat : RegExp.HTTP_DATES) {
                    ExtractedDate date = DateParser.findDate(dateString, dateFormat);
                    if (date != null) {
                        result.add(new MetaDate(date, keyword));
                    }
                }
            }
        }
        return result;
    }

}
