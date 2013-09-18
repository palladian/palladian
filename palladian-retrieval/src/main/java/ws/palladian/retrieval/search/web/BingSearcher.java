package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.BaseBingSearcher;

/**
 * <p>
 * Bing search.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class BingSearcher extends BaseBingSearcher<WebContent> {

    /**
     * @see BaseBingSearcher#BaseBingSearcher(String)
     */
    public BingSearcher(String accountKey) {
        super(accountKey);
    }

    /**
     * @see BaseBingSearcher#BaseBingSearcher(Configuration)
     */
    public BingSearcher(Configuration configuration) {
        super(configuration);
    }

    @Override
    public String getName() {
        return "Bing";
    }

    @Override
    protected String getSourceType() {
        return "Web";
    }

    @Override
    protected WebContent parseResult(JSONObject currentResult) throws JSONException {
        BasicWebContent.Builder builder = new BasicWebContent.Builder();
        builder.setUrl(currentResult.getString("Url"));
        if (currentResult.has("Title")) {
            builder.setTitle(currentResult.getString("Title"));
        }
        if (currentResult.has("Description")) {
            builder.setSummary(currentResult.getString("Description"));
        }
        if (currentResult.has("DateTime")) {
            String dateString = currentResult.getString("DateTime");
            builder.setPublished(parseDate(dateString));
        }
        return builder.create();
    }

    @Override
    protected int getDefaultFetchSize() {
        return 50;
    }

}
