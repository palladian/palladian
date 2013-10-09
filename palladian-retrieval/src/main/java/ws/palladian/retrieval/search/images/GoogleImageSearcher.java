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
        BasicWebImage.Builder builder = new BasicWebImage.Builder();
        builder.setUrl(resultData.getString("originalContextUrl"));
        builder.setImageUrl(resultData.getString("unescapedUrl"));
        builder.setTitle(resultData.getString("content"));
        builder.setWidth(resultData.getInt("width"));
        builder.setHeight(resultData.getInt("height"));
        return builder.create();
    }

    @Override
    public String getName() {
        return "Google Images";
    }

}
