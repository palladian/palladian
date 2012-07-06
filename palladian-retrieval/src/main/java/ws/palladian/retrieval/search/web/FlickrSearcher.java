package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Search for images on <a href="http://www.flickr.com/">Flickr</a>.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://www.flickr.com/services/api/">Flickr Services</a>
 * @see <a href="http://www.flickr.com/services/api/misc.api_keys.html">Obtaining an API key</a>
 */
public final class FlickrSearcher extends WebSearcher<WebImageResult> {

    /** Identifier for the API key when supplied via {@link Configuration}. */
    public static final String CONFIG_API_KEY = null;

    private final String apiKey;

    /**
     * <p>
     * Creates a new Flickr searcher.
     * </p>
     * 
     * @param apiKey The API key for accessing Flickr, not <code>null</code> or empty.
     */
    public FlickrSearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
    }

    /**
     * <p>
     * Creates a new Flickr searcher.
     * </p>
     * 
     * @param configuration The configuration which must provide an API key for accessing Flickr, which must be provided
     *            as string via key {@value FlickrSearcher#CONFIG_API_KEY} in the configuration.
     */
    public FlickrSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    @Override
    public String getName() {
        return "Flickr";
    }

    @Override
    public List<WebImageResult> search(String query, int resultCount, Language language) throws SearcherException {
        List<WebImageResult> result = new ArrayList<WebImageResult>();

        // TODO paging currently not implemented.
        // TODO implement checking for error codes.

        String requestUrl = buildRequestUrl(query, 1, language);
        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(requestUrl);
        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": "
                    + e.getMessage() + ", request URL was \"" + requestUrl + "\"", e);
        }
        String jsonString = HttpHelper.getStringContent(httpResult);
        try {
            JSONObject resultJson = new JSONObject(jsonString);
            JSONObject photosJson = resultJson.getJSONObject("photos");
            JSONArray photoJsonArray = photosJson.getJSONArray("photo");
            for (int i = 0; i < photoJsonArray.length(); i++) {
                JSONObject photoJson = photoJsonArray.getJSONObject(i);
                String title = photoJson.getString("title");
                String farmId = photoJson.getString("farm");
                String serverId = photoJson.getString("server");
                String id = photoJson.getString("id");
                String secret = photoJson.getString("secret");
                String url = buildImageUrl(farmId, serverId, id, secret);
                result.add(new WebImageResult(url, title, -1, -1));
            }
        } catch (JSONException e) {
            throw new SearcherException("Parse error while searching for \"" + query + "\" with " + getName() + ": "
                    + e.getMessage() + ", JSON was \"" + jsonString + "\"", e);
        }
        return result;
    }

    private String buildRequestUrl(String query, int page, Language language) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://api.flickr.com/services/rest/");
        urlBuilder.append("?method=flickr.photos.search");
        urlBuilder.append("&api_key=").append(apiKey);
        urlBuilder.append("&text=").append(query);
        urlBuilder.append("&format=json");
        urlBuilder.append("&nojsoncallback=1");
        return urlBuilder.toString();
    }

    /**
     * <p>
     * Transforms the given parts back to an image URL.
     * </p>
     * 
     * @param farmId
     * @param serverId
     * @param id
     * @param secret
     * @return
     * @see <a href="http://www.flickr.com/services/api/misc.urls.html">URLs</a>
     */
    private String buildImageUrl(String farmId, String serverId, String id, String secret) {
        return String.format("http://farm%s.staticflickr.com/%s/%s_%s.jpg", farmId, serverId, id, secret);
    }

}
