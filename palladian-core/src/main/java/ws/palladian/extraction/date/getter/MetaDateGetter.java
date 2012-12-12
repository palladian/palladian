package ws.palladian.extraction.date.getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;

import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.ParserException;

/**
 * <p>
 * This {@link TechniqueDateGetter} extracts dates from meta information, it combines {@link HttpDateGetter} to extract
 * dates from HTTP response headers and {@link HeadDateGetter} to extract dates from HTML documents' <code>head</code>
 * section.
 * </p>
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public class MetaDateGetter extends TechniqueDateGetter<MetaDate> {

    private final HttpDateGetter httpDateGetter = new HttpDateGetter();
    private final HeadDateGetter headDateGetter = new HeadDateGetter();

    @Override
    public List<MetaDate> getDates(String url) {
        try {
            HttpResult httpResult = httpRetriever.httpGet(url);
            return getDates(httpResult);
        } catch (HttpException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<MetaDate> getDates(HttpResult httpResult) {
        List<MetaDate> dates = new ArrayList<MetaDate>();
        dates.addAll(httpDateGetter.getDates(httpResult));

        try {
            Document document = htmlParser.parse(httpResult);
            dates.addAll(headDateGetter.getDates(document));
        } catch (ParserException e) {
        }

        return dates;
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

}
