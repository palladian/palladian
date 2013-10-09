package ws.palladian.retrieval.search.socialmedia;

import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.BaseGoogleSearcher;


/**
 * <p>
 * Google blogs search.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class GoogleBlogsSearcher extends BaseGoogleSearcher<WebContent> {

    @Override
    protected String getBaseUrl() {
        return "http://ajax.googleapis.com/ajax/services/search/blogs";
    }

    @Override
    protected WebContent parseResult(JSONObject resultData) throws JSONException {
        BasicWebContent.Builder builder = new BasicWebContent.Builder();
        builder.setTitle(resultData.getString("titleNoFormatting"));
        builder.setSummary(resultData.getString("content"));
        builder.setUrl(resultData.getString("postUrl"));
        return builder.create();
    }

    @Override
    public String getName() {
        return "Google Blogs";
    }

}
