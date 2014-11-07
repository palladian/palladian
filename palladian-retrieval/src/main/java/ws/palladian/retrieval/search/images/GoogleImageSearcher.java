package ws.palladian.retrieval.search.images;

import org.apache.commons.lang3.StringEscapeUtils;

import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
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
@Deprecated
public final class GoogleImageSearcher extends BaseGoogleSearcher<WebImage> {

    @Override
    protected String getBaseUrl() {
        return "http://ajax.googleapis.com/ajax/services/search/images";
    }

    @Override
    protected WebImage parseResult(JsonObject resultData) throws JsonException {
        BasicWebImage.Builder builder = new BasicWebImage.Builder();
        builder.setUrl(resultData.getString("originalContextUrl"));
        builder.setImageUrl(resultData.getString("unescapedUrl"));
        builder.setTitle(StringEscapeUtils.unescapeHtml4(resultData.getString("content")));
        builder.setWidth(resultData.getInt("width"));
        builder.setHeight(resultData.getInt("height"));
        return builder.create();
    }

    @Override
    public String getName() {
        return "Google Images";
    }

}
