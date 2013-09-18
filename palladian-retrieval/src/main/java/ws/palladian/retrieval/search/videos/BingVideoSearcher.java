package ws.palladian.retrieval.search.videos;

import org.apache.commons.configuration.Configuration;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.retrieval.resources.BasicWebVideo;
import ws.palladian.retrieval.resources.WebVideo;
import ws.palladian.retrieval.search.BaseBingSearcher;

/**
 * <p>
 * Bing Video search.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class BingVideoSearcher extends BaseBingSearcher<WebVideo> {

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
    protected WebVideo parseResult(JSONObject currentResult) throws JSONException {
        BasicWebVideo.Builder builder = new BasicWebVideo.Builder();
        builder.setTitle(currentResult.getString("Title"));
        builder.setUrl(currentResult.getString("MediaUrl"));
        // interpret a value of "0", as "no run time specified"
        Long runTime = currentResult.getLong("RunTime");
        if (runTime != 0) {
            builder.setDuration(runTime);
        }
        return builder.create();
    }

    @Override
    protected String getSourceType() {
        return "Video";
    }

    @Override
    protected int getDefaultFetchSize() {
        return 50;
    }

}
