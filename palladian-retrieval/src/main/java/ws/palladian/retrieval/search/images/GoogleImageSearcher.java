package ws.palladian.retrieval.search.images;

import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.BaseGoogleSearcher;

/**
 * <p>
 * Google image search.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class GoogleImageSearcher extends BaseGoogleSearcher<WebImage> {

    @Override
    protected String getBaseUrl() {
        return "http://ajax.googleapis.com/ajax/services/search/images";
    }

    @Override
    protected WebImage parseResult(JSONObject resultData) throws JSONException {
        String pageUrl = resultData.getString("originalContextUrl");
        String imageUrl = resultData.getString("unescapedUrl");
        String caption = resultData.getString("content");
        int width = resultData.getInt("width");
        int height = resultData.getInt("height");
        return new BasicWebImage(pageUrl, imageUrl, caption, null, width, height, null);
    }

    @Override
    public String getName() {
        return "Google Images";
    }

}
