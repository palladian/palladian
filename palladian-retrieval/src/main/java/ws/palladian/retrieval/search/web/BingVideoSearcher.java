package ws.palladian.retrieval.search.web;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.search.SearcherException;

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
        String url = currentResult.getString("MediaUrl");
        // interpret a value of "0", as "no run time specified"
        Long runTime = currentResult.getLong("RunTime");
        if (runTime == 0) {
            runTime = null;
        }
        return new WebVideoResult(url, title, runTime);
    }

    @Override
    protected String getSourceType() {
        return "Video";
    }

    @Override
    protected int getDefaultFetchSize() {
        return 10;
    }

    public static void main(String[] args) throws SearcherException {
        BingVideoSearcher searcher = new BingVideoSearcher("D35DE1803D6F6F03AB5044430997A91924AD347A");
        List<WebVideoResult> results = searcher.search("dresden", 50);
        CollectionHelper.print(results);
    }

}
