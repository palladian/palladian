package ws.palladian.retrieval.search.images;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.helper.JsonObjectWrapper;
import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.SearcherException;
import ws.palladian.retrieval.search.web.WebSearcher;

/**
 * <p>
 * Search for public domain images on <a href="http://www.pixabay.com/">Pixabay</a>.
 * </p>
 * 
 * @author David Urbansky
 * @see <a href="http://pixabay.com/api/docs/">Pixabay API</a>
 */
public class PixabaySearcher extends WebSearcher<WebImageResult> {

    /**
     * Identifier for the API key when supplied via {@link Configuration}.
     */
    public static final String CONFIG_API_USER = "api.pixabay.user";
    public static final String CONFIG_API_KEY = "api.pixabay.key";

    private final String apiUser;
    private final String apiKey;

    /**
     * <p>
     * Creates a new Pixabay searcher.
     * </p>
     * 
     * @param apiKey The API key for accessing Pixabay, not <code>null</code> or empty.
     */
    public PixabaySearcher(String apiUser, String apiKey) {
        Validate.notEmpty(apiKey, "apiUser must not be empty");
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiUser = apiUser;
        this.apiKey = apiKey;
    }

    /**
     * <p>
     * Creates a new Pixabay searcher.
     * </p>
     * 
     * @param configuration The configuration which must provide an API key for accessing Pixabay, which must be
     *            provided
     *            as string via key {@value PixabaySearcher#CONFIG_API_KEY} in the configuration.
     */
    public PixabaySearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_USER), configuration.getString(CONFIG_API_KEY));
    }

    @Override
    /**
     * @param language Supported languages are  id, cs, de, en, es, fr, it, nl, no, hu, ru, pl, pt, ro, fi, sv, tr, ja, ko, and zh.
     */
    public List<WebImageResult> search(String query, int resultCount, Language language) throws SearcherException {
        List<WebImageResult> results = CollectionHelper.newArrayList();

        resultCount = Math.min(1000, resultCount);
        int resultsPerPage = Math.min(100, resultCount);
        int pagesNeeded = (int)Math.ceil(resultCount / (double)resultsPerPage);

        DocumentRetriever documentRetriever = new DocumentRetriever();

        for (int page = 1; page <= pagesNeeded; page++) {

            String requestUrl = buildRequest(query, page, Math.min(100, resultCount - results.size()), language);
            JsonObjectWrapper json = new JsonObjectWrapper(documentRetriever.getText(requestUrl));
            JSONArray jsonArray = json.getJSONArray("hits");
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JsonObjectWrapper resultHit = new JsonObjectWrapper(jsonArray.getJSONObject(i));
                    String url = resultHit.getString("pageURL");
                    String imageUrl = resultHit.getString("webformatURL");
                    String summary = resultHit.getString("tags"); // TODO add tags to image
                    String imageTypeString = resultHit.getString("type");
                    WebImageResult webImageResult = new WebImageResult(url, imageUrl, summary, summary, -1, -1, null,
                            null);

                    webImageResult.setLicense(License.PUBLIC_DOMAIN);
                    webImageResult.setImageType(getImageType(imageTypeString));

                    results.add(webImageResult);

                } catch (JSONException e) {
                    throw new SearcherException(e.getMessage());
                }

            }
        }

        return results;
    }

    private ImageType getImageType(String imageTypeString) {
        if (imageTypeString.equalsIgnoreCase("photo")) {
            return ImageType.PHOTO;
        } else if (imageTypeString.equalsIgnoreCase("clipart")) {
            return ImageType.CLIPART;
        } else if (imageTypeString.equalsIgnoreCase("vector")) {
            return ImageType.VECTOR;
        }

        return ImageType.UNKNOWN;
    }

    private String buildRequest(String searchTerms, int page, int resultsPerPage, Language language) {
        searchTerms = UrlHelper.encodeParameter(searchTerms);
        String url = "http://pixabay.com/api/?username=" + apiUser + "&key=" + apiKey + "&search_term=" + searchTerms
                + "&image_type=all&page=" + page + "&per_page=" + resultsPerPage + "&lang=" + language.getIso6391();

        return url;
    }

    @Override
    public String getName() {
        return "Pixabay";
    }

    /**
     * @param args
     * @throws SearcherException
     */
    public static void main(String[] args) throws SearcherException {
        PixabaySearcher pixabaySearcher = new PixabaySearcher("USER", "KEY");
        List<WebImageResult> results = pixabaySearcher.search("car", 101);
        CollectionHelper.print(results);
    }
}
