package ws.palladian.retrieval.search.web;

import java.util.Date;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>
 * Bing search.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class BingSearcher extends BaseBingSearcher<WebResult> {
    /**
     * @see BaseBingSearcher#BaseBingSearcher(String)
     */
    public BingSearcher(String apiKey) {
        super(apiKey);
    }

    /**
     * @see BaseBingSearcher#BaseBingSearcher(PropertiesConfiguration)
     */
    public BingSearcher(PropertiesConfiguration configuration) {
        super(configuration);
    }

    /**
     * @see BaseBingSearcher#BaseBingSearcher()
     */
    public BingSearcher() {
        super();
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
    protected WebResult parseResult(JSONObject currentResult) throws JSONException {
        String url = currentResult.getString("Url");
        String title = null;
        if (currentResult.has("Title")) {
            title = currentResult.getString("Title");
        }
        String summary = null;
        if (currentResult.has("Description")) {
            summary = currentResult.getString("Description");
        }
        Date date = null;
        if (currentResult.has("DateTime")) {
            String dateString = currentResult.getString("DateTime");
            date = parseDate(dateString);
        }
        WebResult webResult = new WebResult(url, title, summary, date);
        return webResult;
    }

    @Override
    protected int getDefaultFetchSize() {
        return 25;
    }

}
