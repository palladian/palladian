package ws.palladian.retrieval.search.services;

import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.retrieval.search.Searcher;
import ws.palladian.retrieval.search.WebImageResult;

/**
 * <p>
 * Google image search.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class GoogleImageSearcher extends BaseGoogleSearcher<WebImageResult> implements Searcher<WebImageResult> {

    @Override
    protected String getBaseUrl() {
        return "http://ajax.googleapis.com/ajax/services/search/images";
    }

    @Override
    protected WebImageResult parseResult(JSONObject resultData) throws JSONException {
        String url = resultData.getString("unescapedUrl");
        String caption = resultData.getString("content");
        int width = resultData.getInt("width");
        int height = resultData.getInt("height");
        return new WebImageResult(url, caption, width, height);
    }

    @Override
    public String getName() {
        return "Google Images";
    }

}
