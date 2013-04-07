package ws.palladian.retrieval.search.images;

import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.retrieval.search.BaseGoogleSearcher;

/**
 * <p>
 * Google image search.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class GoogleImageSearcher extends BaseGoogleSearcher<WebImageResult> {

    @Override
    protected String getBaseUrl() {
        return "http://ajax.googleapis.com/ajax/services/search/images";
    }

    @Override
    protected WebImageResult parseResult(JSONObject resultData) throws JSONException {
        String pageUrl = resultData.getString("originalContextUrl");
        String imageUrl = resultData.getString("unescapedUrl");
        String caption = resultData.getString("content");
        int width = resultData.getInt("width");
        int height = resultData.getInt("height");
        return new WebImageResult(pageUrl, imageUrl, caption, null, width, height, null, null);
    }

    @Override
    public String getName() {
        return "Google Images";
    }

}
