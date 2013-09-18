package ws.palladian.retrieval.search.news;

import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.retrieval.search.BaseGoogleSearcher;
import ws.palladian.retrieval.search.WebContent;
import ws.palladian.retrieval.search.web.BasicWebContent;


/**
 * <p>
 * Google news search.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class GoogleNewsSearcher extends BaseGoogleSearcher<WebContent> {

    @Override
    protected String getBaseUrl() {
        return "http://ajax.googleapis.com/ajax/services/search/news";
    }

    @Override
    protected WebContent parseResult(JSONObject resultData) throws JSONException {
        String title = resultData.getString("titleNoFormatting");
        String content = resultData.getString("content");
        String url = resultData.getString("unescapedUrl");
        return new BasicWebContent(url, title, content);
    }

    @Override
    public String getName() {
        return "Google News";
    }

}
