package ws.palladian.retrieval.search.services;

import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.retrieval.search.WebResult;
import ws.palladian.retrieval.search.WebSearcher;
import ws.palladian.retrieval.search.WebSearcherManager;

public final class GoogleBlogsSearcher extends BaseGoogleSearcher implements WebSearcher {

    @Override
    protected String getBaseUrl() {
        return "http://ajax.googleapis.com/ajax/services/search/blogs";
    }

    @Override
    protected WebResult parseResult(JSONObject resultData) throws JSONException {
        String title = resultData.getString("titleNoFormatting");
        String content = resultData.getString("content");
        String url = resultData.getString("postUrl");
        WebResult webResult = new WebResult(WebSearcherManager.GOOGLE, 0, url, title, content);
        return webResult;
    }

    @Override
    public String getName() {
        return "Google Blogs";
    }

}
