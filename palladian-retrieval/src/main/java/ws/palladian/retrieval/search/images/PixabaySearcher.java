package ws.palladian.retrieval.search.images;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Search for public domain images on <a href="http://www.pixabay.com/">Pixabay</a>.
 * </p>
 * 
 * @author David Urbansky
 * @see <a href="http://pixabay.com/api/docs/">Pixabay API</a>
 */
public class PixabaySearcher extends AbstractSearcher<WebImage> {

    /** The name of this searcher. */
    private static final String SEARCHER_NAME = "Pixabay";

    /** Identifier for the API user when supplied via {@link Configuration}. */
    public static final String CONFIG_API_USER = "api.pixabay.user";

    /** Identifier for the API key when supplied via {@link Configuration}. */
    public static final String CONFIG_API_KEY = "api.pixabay.key";

    private final String apiUser;

    private final String apiKey;

    /**
     * <p>
     * Creates a new Pixabay searcher.
     * </p>
     * 
     * @param apiUser The API user for accessing Pixabay, not <code>null</code> or empty.
     * @param apiKey The API key for accessing Pixabay, not <code>null</code> or empty.
     */
    public PixabaySearcher(String apiUser, String apiKey) {
        Validate.notEmpty(apiUser, "apiUser must not be empty");
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
     *            provided as string via key {@value PixabaySearcher#CONFIG_API_KEY} in the configuration.
     */
    public PixabaySearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_USER), configuration.getString(CONFIG_API_KEY));
    }

    @Override
    /**
     * @param language Supported languages are  id, cs, de, en, es, fr, it, nl, no, hu, ru, pl, pt, ro, fi, sv, tr, ja, ko, and zh.
     */
    public List<WebImage> search(String query, int resultCount, Language language) throws SearcherException {
        List<WebImage> results = CollectionHelper.newArrayList();

        if (language == null) {
            language = DEFAULT_SEARCHER_LANGUAGE;
        }

        resultCount = Math.min(1000, resultCount);
        int resultsPerPage = Math.min(100, resultCount);
        int pagesNeeded = (int)Math.ceil(resultCount / (double)resultsPerPage);

        DocumentRetriever documentRetriever = new DocumentRetriever();

        for (int page = 1; page <= pagesNeeded; page++) {

            String requestUrl = buildRequest(query, page, Math.min(100, resultCount - results.size()), language);
            try {
                String textResponse = documentRetriever.getText(requestUrl);
                if (textResponse == null) {
                    throw new SearcherException("Failed to get JSON from " + requestUrl);
                }
                JsonObject json = new JsonObject(textResponse);
                JsonArray jsonArray = json.getJsonArray("hits");
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject resultHit = jsonArray.getJsonObject(i);
                    BasicWebImage.Builder builder = new BasicWebImage.Builder();
                    builder.setUrl(resultHit.getString("pageURL"));
                    builder.setImageUrl(resultHit.getString("webformatURL"));
                    builder.setTitle(resultHit.getString("tags"));
                    builder.setWidth(resultHit.getInt("imageWidth"));
                    builder.setHeight(resultHit.getInt("imageHeight"));
                    builder.setImageType(getImageType(resultHit.getString("type")));
                    builder.setThumbnailUrl(resultHit.getString("previewURL"));
                    builder.setLicense(License.PUBLIC_DOMAIN);
                    builder.setLicenseLink("http://creativecommons.org/publicdomain/zero/1.0/deed.en");
                    results.add(builder.create());
                    if (results.size() >= resultCount) {
                        break;
                    }
                }

            } catch (JsonException e) {
                throw new SearcherException(e.getMessage());
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
        return String.format(
                "http://pixabay.com/api/?username=%s&key=%s&search_term=%s&image_type=all&page=%s&per_page=%s&lang=%s",
                apiUser, apiKey, UrlHelper.encodeParameter(searchTerms), page, resultsPerPage, language.getIso6391());
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    public static void main(String[] args) throws SearcherException {
        PixabaySearcher pixabaySearcher = new PixabaySearcher("USER", "KEY");
        List<WebImage> results = pixabaySearcher.search("car", 101);
        CollectionHelper.print(results);
    }
}
