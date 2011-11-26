package ws.palladian.retrieval.search.services;

import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.retrieval.search.WebImageResult;

/**
 * <p>
 * Bing Image search.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class BingImageSearcher extends BaseBingSearcher<WebImageResult> {

    @Override
    public String getName() {
        return "Bing Images";
    }

    @Override
    protected WebImageResult parseResult(JSONObject currentResult) throws JSONException {
        String url = currentResult.getString("MediaUrl");
        int width = currentResult.getInt("Width");
        int height = currentResult.getInt("Height");
        String title = currentResult.getString("Title");
        WebImageResult webResult = new WebImageResult(url, title, width, height);
        return webResult;
    }

    @Override
    protected String getSourceType() {
        return "Image";
    }

    @Override
    protected int getDefaultFetchSize() {
        return 25;
    }

}
