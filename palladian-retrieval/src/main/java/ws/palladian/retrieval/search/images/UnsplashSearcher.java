package ws.palladian.retrieval.search.images;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.MapBuilder;
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
 * Search for free images on <a href="https://unsplash.com/documentation">Unsplash</a>.
 * </p>
 * 
 * @author David Urbansky
 * @see <a href="https://unsplash.com/documentation">Unsplash API Docs</a>
 */
public class UnsplashSearcher extends AbstractSearcher<WebImage> {
    /** The name of this searcher. */
    private static final String SEARCHER_NAME = "Unsplash";

    /** Identifier for the API key when supplied via {@link Configuration}. */
    public static final String CONFIG_API_KEY = "api.unsplash.key";

    private final String apiKey;

    /**
     * <p>
     * Creates a new Unsplash searcher.
     * </p>
     *
     * @param apiKey The API key for accessing Unsplash, not <code>null</code> or empty.
     */
    public UnsplashSearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
    }

    /**
     * <p>
     * Creates a new Unsplash searcher.
     * </p>
     *
     * @param configuration The configuration which must provide an API key for accessing Unsplash, which must be
     *            provided as string via key {@value UnsplashSearcher#CONFIG_API_KEY} in the configuration.
     */
    public UnsplashSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    @Override
    /**
     * @param language Supported languages are English.
     */
    public List<WebImage> search(String query, int resultCount, Language language) throws SearcherException {
        List<WebImage> results = new ArrayList<>();

        resultCount = Math.min(1000, resultCount);
        int resultsPerPage = Math.min(100, resultCount);
        int pagesNeeded = (int)Math.ceil(resultCount / (double)resultsPerPage);

        DocumentRetriever documentRetriever = new DocumentRetriever();
        Map<String, String> globalHeaders = new HashMap<>();
        globalHeaders.put("Authorization", "Client-ID " + apiKey);

        documentRetriever.setGlobalHeaders(globalHeaders);

        for (int page = 1; page <= pagesNeeded; page++) {
            String requestUrl = buildRequest(query, page, Math.min(100, resultCount - results.size()));
            try {
                JsonObject jsonResponse = documentRetriever.getJsonObject(requestUrl);
                if (jsonResponse == null) {
                    throw new SearcherException("Failed to get JSON from " + requestUrl);
                }
                JsonObject json = new JsonObject(jsonResponse);
                JsonArray jsonArray = json.getJsonArray("results");
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject resultHit = jsonArray.getJsonObject(i);

                    BasicWebImage.Builder builder = new BasicWebImage.Builder();
                    builder.setAdditionalData("id", resultHit.tryQueryString("id"));
                    builder.setUrl(resultHit.tryQueryString("urls/raw"));
                    builder.setImageUrl(resultHit.tryQueryString("urls/regular"));
                    builder.setTitle(resultHit.tryQueryString("description"));
                    builder.setWidth(resultHit.getInt("width"));
                    builder.setHeight(resultHit.getInt("height"));
                    builder.setImageType(ImageType.PHOTO);
                    builder.setThumbnailUrl(resultHit.tryQueryString("urls/thumb"));
                    builder.setLicense(License.FREE);
                    builder.setLicenseLink("https://unsplash.com/license");
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

    private String buildRequest(String searchTerms, int page, int resultsPerPage) {
        return String.format("https://api.unsplash.com/search/photos?query=%s&per_page=%s&page=%s", UrlHelper.encodeParameter(searchTerms), resultsPerPage, page);
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    public static void main(String[] args) throws SearcherException {
        UnsplashSearcher searcher = new UnsplashSearcher("KEY");
        List<WebImage> results = searcher.search("pizza", 101);
        CollectionHelper.print(results);
    }
}
