package ws.palladian.extraction.date.getter;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import ws.palladian.extraction.date.dates.UrlDate;
import ws.palladian.helper.DateFormat;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.retrieval.HttpResult;

/**
 * <p>
 * This {@link TechniqueDateGetter} extracts dates from a URL string. Therefore the regular expressions from
 * {@link RegExp#URL_DATES} are used.
 * </p>
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public class UrlDateGetter extends TechniqueDateGetter<UrlDate> {

    @Override
    public List<UrlDate> getDates(String url) {
        List<UrlDate> result = new ArrayList<UrlDate>();
        for (DateFormat dateFormat : RegExp.URL_DATES) {
            ExtractedDate extractedDate = DateParser.findDate(url, dateFormat);
            if (extractedDate != null) {
                result.add(new UrlDate(extractedDate, url));
                break;
            }
        }
        return result;
    }

    @Override
    public List<UrlDate> getDates(HttpResult httpResult) {
        return getDates(httpResult.getUrl());
    }

    @Override
    public List<UrlDate> getDates(Document document) {
        return getDates(getUrl(document));
    }

}
