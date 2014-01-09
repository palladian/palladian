package ws.palladian.retrieval.search.socialmedia;

import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
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
@Deprecated
public final class GoogleBlogsSearcher extends BaseGoogleSearcher<WebContent> {

    @Override
    protected String getBaseUrl() {
        return "http://ajax.googleapis.com/ajax/services/search/blogs";
    }

    @Override
    protected WebContent parseResult(JsonObject resultData) throws JsonException {
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
