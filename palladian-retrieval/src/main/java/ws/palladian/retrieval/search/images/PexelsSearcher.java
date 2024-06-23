package ws.palladian.retrieval.search.images;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.configuration.StringConfigurationOption;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.SearcherException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Search for free images on <a href="http://www.pexels.com/">Pexels</a>.
 * </p>
 *
 * @author David Urbansky
 * @see <a href="https://www.pexels.com/api/documentation/">Pexels API Docs</a>
 */
public class PexelsSearcher extends AbstractSearcher<WebImage> {
    public static final class PexelsSearcherMetaInfo implements SearcherMetaInfo<PexelsSearcher, WebImage> {
        private static final StringConfigurationOption API_KEY_OPTION = new StringConfigurationOption("API Key", "apikey");

        @Override
        public String getSearcherName() {
            return SEARCHER_NAME;
        }

        @Override
        public String getSearcherId() {
            return "pexels";
        }

        @Override
        public Class<WebImage> getResultType() {
            return WebImage.class;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Arrays.asList(API_KEY_OPTION);
        }

        @Override
        public PexelsSearcher create(Map<ConfigurationOption<?>, ?> config) {
            var apiKey = API_KEY_OPTION.get(config);
            return new PexelsSearcher(apiKey);
        }
    }

    /**
     * The name of this searcher.
     */
    private static final String SEARCHER_NAME = "Pexels";

    /**
     * Identifier for the API key when supplied via {@link Configuration}.
     */
    public static final String CONFIG_API_KEY = "api.pexels.key";

    private final String apiKey;

    /**
     * <p>
     * Creates a new Pexels searcher.
     * </p>
     *
     * @param apiKey The API key for accessing Pexels, not <code>null</code> or empty.
     */
    public PexelsSearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
    }

    public PexelsSearcher(String apiKey, int defaultResultCount) {
        this(apiKey);
        this.defaultResultCount = defaultResultCount;
    }

    /**
     * <p>
     * Creates a new Pexels searcher.
     * </p>
     *
     * @param configuration The configuration which must provide an API key for accessing Pexels, which must be
     *                      provided as string via key {@value PexelsSearcher#CONFIG_API_KEY} in the configuration.
     */
    public PexelsSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    public PexelsSearcher(Configuration config, int defaultResultCount) {
        this(config);
        this.defaultResultCount = defaultResultCount;
    }

    @Override
    /**
     * @param language Supported languages are English.
     */ public List<WebImage> search(String query, int resultCount, Language language) throws SearcherException {
        List<WebImage> results = new ArrayList<>();

        resultCount = defaultResultCount == null ? resultCount : defaultResultCount;
        resultCount = Math.min(1000, resultCount);
        int resultsPerPage = Math.min(100, resultCount);
        int pagesNeeded = (int) Math.ceil(resultCount / (double) resultsPerPage);

        DocumentRetriever documentRetriever = new DocumentRetriever();
        Map<String, String> globalHeaders = new HashMap<>();
        globalHeaders.put("Authorization", apiKey);

        documentRetriever.setGlobalHeaders(globalHeaders);

        for (int page = 1; page <= pagesNeeded; page++) {

            String requestUrl = buildRequest(query, page, Math.min(100, resultCount - results.size()));
            try {
                JsonObject jsonResponse = documentRetriever.getJsonObject(requestUrl);
                if (jsonResponse == null) {
                    throw new SearcherException("Failed to get JSON from " + requestUrl);
                }
                JsonObject json = new JsonObject(jsonResponse);
                JsonArray jsonArray = json.tryGetJsonArray("photos", new JsonArray());
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject resultHit = jsonArray.getJsonObject(i);

                    BasicWebImage.Builder builder = new BasicWebImage.Builder();
                    builder.setAdditionalData("id", resultHit.tryGetInt("id") + "");
                    builder.setUrl(resultHit.getString("url"));
                    builder.setImageUrl(resultHit.tryQueryString("src/original"));
                    builder.setTitle("");
                    builder.setWidth(resultHit.getInt("width"));
                    builder.setHeight(resultHit.getInt("height"));
                    builder.setImageType(ImageType.PHOTO);
                    builder.setThumbnailUrl(resultHit.tryQueryString("src/tiny"));
                    builder.setLicense(License.FREE);
                    builder.setLicenseLink("https://www.pexels.com/photo-license/");
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
        return String.format("https://api.pexels.com/v1/search?query=%s&per_page=%s&page=%s", UrlHelper.encodeParameter(searchTerms), resultsPerPage, page);
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    public static void main(String[] args) throws SearcherException {
        PexelsSearcher searcher = new PexelsSearcher("KEY");
        List<WebImage> results = searcher.search("pizza", 101);
        CollectionHelper.print(results);
    }
}
