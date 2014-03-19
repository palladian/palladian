package ws.palladian.retrieval.search.videos;

import org.apache.commons.configuration.Configuration;

import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
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
    protected WebVideo parseResult(JsonObject currentResult) throws JsonException {
        BasicWebVideo.Builder builder = new BasicWebVideo.Builder();
        builder.setTitle(currentResult.getString("Title"));
        builder.setUrl(currentResult.getString("MediaUrl"));
        // interpret a value of "0", as "no run time specified"
        Integer runTime = currentResult.getInt("RunTime");
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
