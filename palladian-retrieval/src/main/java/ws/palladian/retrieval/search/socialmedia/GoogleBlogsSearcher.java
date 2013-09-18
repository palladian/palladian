package ws.palladian.retrieval.search.socialmedia;

import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.retrieval.search.BaseGoogleSearcher;
import ws.palladian.retrieval.search.web.BasicWebContent;


/**
 * <p>
 * Google blogs search.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class GoogleBlogsSearcher extends BaseGoogleSearcher<BasicWebContent> {

    @Override
    protected String getBaseUrl() {
        return "http://ajax.googleapis.com/ajax/services/search/blogs";
    }

    @Override
    protected BasicWebContent parseResult(JSONObject resultData) throws JSONException {
        String title = resultData.getString("titleNoFormatting");
        String content = resultData.getString("content");
        String url = resultData.getString("postUrl");
        BasicWebContent webResult = new BasicWebContent(url, title, content);
        return webResult;
    }

    @Override
    public String getName() {
        return "Google Blogs";
    }

}
