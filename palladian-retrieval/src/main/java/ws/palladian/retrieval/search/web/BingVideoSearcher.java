package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>
 * Bing Video search.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class BingVideoSearcher extends BaseBingSearcher<WebVideoResult> {

    /**
     * @see BaseBingSearcher#BaseBingSearcher(Configuration)
     */
    public BingVideoSearcher(Configuration configuration) {
        super(configuration);
    }

    /**
     * @see BaseBingSearcher#BaseBingSearcher(String)
     */
    public BingVideoSearcher(String accountKey) {
        super(accountKey);
    }

    @Override
    public String getName() {
        return "Bing Videos";
    }

    @Override
    protected WebVideoResult parseResult(JSONObject currentResult) throws JSONException {
        String title = currentResult.getString("Title");
        String pageUrl = currentResult.getString("MediaUrl");
        // interpret a value of "0", as "no run time specified"
        Long runTime = currentResult.getLong("RunTime");
        if (runTime == 0) {
            runTime = null;
        }
        return new WebVideoResult(pageUrl, null, title, runTime, null);
    }

    @Override
    protected String getSourceType() {
        return "Video";
    }

    @Override
    protected int getDefaultFetchSize() {
        return 10;
    }

}
