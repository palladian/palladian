package ws.palladian.retrieval.search.web;

import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.BaseGoogleSearcher;

/**
 * <p>
 * Google search.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class GoogleSearcher extends BaseGoogleSearcher<WebContent> {

    @Override
    protected String getBaseUrl() {
        return "http://ajax.googleapis.com/ajax/services/search/web";
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
        return "Google";
    }

}
