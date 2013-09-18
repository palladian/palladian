package ws.palladian.retrieval.search.images;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.search.AbstractSearcher;
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
public final class FlickrSearcher extends AbstractSearcher<WebImageResult> {

    /**
     * Identifier for the API key when supplied via {@link Configuration}.
     */
    public static final String CONFIG_API_KEY = "api.flickr.key";

    private final String apiKey;

    /** Search only photos with one of the given licenses. */
    private Collection<Integer> allowedLicenses = CollectionHelper.newHashSet();
    
    private final HttpRetriever retriever;

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
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
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

    /**
     * <pre>
     * 0 = "All Rights Reserved"
     * 1 = "Attribution-NonCommercial-ShareAlike License"
     * 2 = "Attribution-NonCommercial License"
     * 3 = "Attribution-NonCommercial-NoDerivs License"
     * 4 = "Attribution License"
     * 5 = "Attribution-ShareAlike License"
     * 6 = "Attribution-NoDerivs License"
     * 7 = "No known copyright restrictions"
     * 8 = "United States Government Work"
     * </pre>
     * 
     * @param licenses
     */
    public void setAllowedLicenses(Collection<Integer> licenses) {
        allowedLicenses = licenses;
    }

    @Override
    public String getName() {
        return "Flickr";
    }

    @Override
    public List<WebImageResult> search(String query, int resultCount, Language language) throws SearcherException {
        return search(query, null, null, resultCount, language);
    }

    public List<WebImageResult> search(String query, String minUploadDate, String tags, int resultCount,
            Language language) throws SearcherException {
        List<WebImageResult> result = new ArrayList<WebImageResult>();

        int resultsPerPage = Math.min(resultCount, 500); // max. 500 per page
        int neccessaryPages = (int)Math.ceil((double)resultCount / resultsPerPage);

        for (int p = 0; p < neccessaryPages; p++) {
            String requestUrl = buildRequestUrl(query, tags, minUploadDate, resultsPerPage, p, language);
            HttpResult httpResult;
            try {
                httpResult = retriever.httpGet(requestUrl);
            } catch (HttpException e) {
                throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": "
                        + e.getMessage() + ", request URL was \"" + requestUrl + "\"", e);
            }
            // TODO implement checking for error codes.
            String jsonString = httpResult.getStringContent();
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
                    String userId = photoJson.getString("owner");
                    String imageUrl = buildImageUrl(farmId, serverId, id, secret);
                    String pageUrl = buildPageUrl(id, userId);
                    WebImageResult webImageResult = new WebImageResult(pageUrl, imageUrl, title, null, -1, -1, null);
                    webImageResult.setThumbImageUrl(imageUrl);
                    result.add(webImageResult);
                }
            } catch (JSONException e) {
                throw new SearcherException("Parse error while searching for \"" + query + "\" with " + getName()
                        + ": " + e.getMessage() + ", JSON was \"" + jsonString + "\"", e);
            }
        }
        return result;
    }

    /**
     * 
     * @param query
     * @param tags
     * @param uploadDate
     * @param perPage Number of results to return per page.
     * @param page The page to return.
     * @param language
     * @return
     */
    private String buildRequestUrl(String query, String tags, String uploadDate, int perPage, int page,
            Language language) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://api.flickr.com/services/rest/");
        urlBuilder.append("?method=flickr.photos.search");
        urlBuilder.append("&api_key=").append(apiKey);
        if (query != null) {
            urlBuilder.append("&text=").append(UrlHelper.encodeParameter(query));
        }
        if (tags != null) {
            urlBuilder.append("&tags=").append(tags);
        }
        if (uploadDate != null) {
            urlBuilder.append("&min_upload_date=").append(uploadDate);
        }
        if (!allowedLicenses.isEmpty()) {
            urlBuilder.append("&license=").append(StringUtils.join(allowedLicenses, ","));
        }
        urlBuilder.append("&per_page=").append(perPage);
        urlBuilder.append("&page=").append(page);
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

    /**
     * <p>
     * Transforms the given parts to a page URL which gives details about the image.
     * </p>
     * 
     * @param id
     * @param userId
     * @return
     */
    private String buildPageUrl(String id, String userId) {
        return String.format("http://www.flickr.com/photos/%s/%s", userId, id);
    }

}
