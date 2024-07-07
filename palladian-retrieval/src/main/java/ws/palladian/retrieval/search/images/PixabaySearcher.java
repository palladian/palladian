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
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Search for public domain images on <a href="http://www.pixabay.com/">Pixabay</a>.
 * </p>
 *
 * @author David Urbansky
 * @see <a href="https://pixabay.com/api/docs/">Pixabay API</a>
 */
public class PixabaySearcher extends AbstractMultifacetSearcher<WebImage> {
    public static final class PixabaySearcherMetaInfo implements SearcherMetaInfo<PixabaySearcher, WebImage> {
        private static final StringConfigurationOption API_KEY_OPTION = new StringConfigurationOption("API Key", "apikey");

        @Override
        public String getSearcherName() {
            return SEARCHER_NAME;
        }

        @Override
        public String getSearcherId() {
            return "pixabay";
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
        public PixabaySearcher create(Map<ConfigurationOption<?>, ?> config) {
            var apiKey = API_KEY_OPTION.get(config);
            return new PixabaySearcher(apiKey);
        }

        @Override
        public String getSearcherDocumentationUrl() {
            return "https://pixabay.com/api/docs/";
        }

        @Override
        public String getSearcherDescription() {
            return "Search for public domain images on <a href=\"https://www.pixabay.com/\">Pixabay</a>.";
        }
    }

    /** The name of this searcher. */
    private static final String SEARCHER_NAME = "Pixabay";

    /** Identifier for the API key when supplied via {@link Configuration}. */
    public static final String CONFIG_API_KEY = "api.pixabay.key";

    private static final List<String> SUPPORTED_LANGUAGES = Arrays.asList("id", "cs", "de", "en", "es", "fr", "it",
            "nl", "no", "hu", "ru", "pl", "pt", "ro", "fi", "sv", "tr", "ja", "ko", "zh");

    private final String apiKey;

    /**
     * ProxyCrawl allows 100 requests/minute.
     */
    private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.MINUTES, 100);

    /**
     * <p>
     * Creates a new Pixabay searcher.
     * </p>
     *
     * @param apiKey The API key for accessing Pixabay, not <code>null</code> or empty.
     */
    public PixabaySearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
    }

    public PixabaySearcher(String apiKey, int defaultResultCount) {
        this(apiKey);
        this.defaultResultCount = defaultResultCount;
    }

    /**
     * <p>
     * Creates a new Pixabay searcher.
     * </p>
     *
     * @param configuration The configuration which must provide an API key for accessing Pixabay, which must be
     *                      provided as string via key {@value PixabaySearcher#CONFIG_API_KEY} in the configuration.
     */
    public PixabaySearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    public PixabaySearcher(Configuration config, int defaultResultCount) {
        this(config);
        this.defaultResultCount = defaultResultCount;
    }

    @Override
    public SearchResults<WebImage> search(MultifacetQuery query) throws SearcherException {
        List<WebImage> results = new ArrayList<>();
        Long totalResults = null;

        var language = query.getLanguage();
        if (language == null) {
            language = DEFAULT_SEARCHER_LANGUAGE;
        }
        checkSupportedLanguage(language);

        var resultCount = defaultResultCount == null ? query.getResultCount() : defaultResultCount;
        resultCount = Math.min(1000, resultCount);
        int resultsPerPage = Math.min(100, resultCount);
        int pagesNeeded = (int) Math.ceil(resultCount / (double) resultsPerPage);

        DocumentRetriever documentRetriever = new DocumentRetriever();

        for (int page = 1; page <= pagesNeeded; page++) {

            String requestUrl = buildRequest(query.getText(), page, Math.max(3, Math.min(200, resultCount - results.size())), language);
            try {
                THROTTLE.hold();
                String textResponse = documentRetriever.getText(requestUrl);
                if (textResponse == null) {
                    throw new SearcherException("Failed to get JSON from " + requestUrl);
                }
                JsonObject json = JsonObject.tryParse(textResponse);
                if (totalResults == null) {
                    totalResults = json.getLong("total");
                }
                JsonArray jsonArray = Optional.ofNullable(json).orElse(new JsonObject()).tryGetJsonArray("hits");
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject resultHit = jsonArray.getJsonObject(i);
                    BasicWebImage.Builder builder = new BasicWebImage.Builder();
                    builder.setAdditionalData("id", resultHit.tryGetInt("id") + "");
                    builder.setUrl(resultHit.getString("pageURL"));
                    builder.setImageUrl(resultHit.getString("webformatURL"));
                    builder.setTitle(resultHit.getString("tags"));
                    builder.setWidth(resultHit.getInt("imageWidth"));
                    builder.setHeight(resultHit.getInt("imageHeight"));
                    builder.setImageType(getImageType(resultHit.getString("type")));
                    builder.setThumbnailUrl(resultHit.getString("previewURL"));
                    builder.setLicense(License.FREE);
                    builder.setLicenseLink("https://pixabay.com/service/license/");
                    results.add(builder.create());
                    if (results.size() >= resultCount) {
                        break;
                    }
                }

            } catch (JsonException e) {
                throw new SearcherException(e.getMessage());
            }
        }

        return new SearchResults<>(results, totalResults);
    }

    /**
     * @param language Supported languages are id, cs, de, en, es, fr, it, nl, no,
     *                 hu, ru, pl, pt, ro, fi, sv, tr, ja, ko, and zh.
     */
    private static void checkSupportedLanguage(Language language) {
        if (!SUPPORTED_LANGUAGES.contains(language.getIso6391())) {
            throw new IllegalArgumentException(
                    "Unsupported language: " + language + "; supported are: " + String.join(", ", SUPPORTED_LANGUAGES));
        }
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
        return String.format("http://pixabay.com/api/?key=%s&search_term=%s&image_type=all&page=%s&per_page=%s&lang=%s", apiKey, UrlHelper.encodeParameter(searchTerms), page,
                resultsPerPage, language.getIso6391());
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    public static void main(String[] args) throws SearcherException {
        PixabaySearcher pixabaySearcher = new PixabaySearcher("KEY");
        var results = pixabaySearcher.search(new MultifacetQuery.Builder().setText("car").setResultCount(101).create());
        System.out.println(results.getResultCount());
        CollectionHelper.print(results);
    }
}
