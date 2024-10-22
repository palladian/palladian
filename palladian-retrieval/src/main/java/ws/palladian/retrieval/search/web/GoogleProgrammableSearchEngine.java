package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.configuration.StringConfigurationOption;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Searcher for
 * <a href="https://programmablesearchengine.google.com/about/">Google
 * Programmable Search Engine</a> (previously “Google Custom Search”). The free
 * plan allows max. 100 queries/day. Although not obviously visible, the search
 * engine can be configured to search to <b>entire web</b>; see the links
 * provided below for more information. The searcher return max. 100 items per
 * query.
 *
 * @author Philipp Katz
 * @see <a href="https://developers.google.com/custom-search/v1/overview">Overview Google Custom Search</a>
 * @see <a href="http://code.google.com/apis/console/?api=customsearch">Google API console</a>
 * @see <a href="http://www.google.com/cse">Google Custom Search settings</a>
 */
public final class GoogleProgrammableSearchEngine extends AbstractMultifacetSearcher<WebContent> {
    public static final class GoogleProgrammableSearchEngineMetaInfo implements SearcherMetaInfo<GoogleProgrammableSearchEngine, WebContent> {
        private static final StringConfigurationOption API_KEY_OPTION = new StringConfigurationOption("API Key",
                "apikey");
        private static final StringConfigurationOption SEARCH_ENGINE_ID_OPTION = new StringConfigurationOption(
                "Search Engine ID", "search_engine_id");

        @Override
        public String getSearcherName() {
            return SEARCHER_NAME;
        }

        @Override
        public String getSearcherId() {
            return "google_custom_searcher";
        }

        @Override
        public Class<WebContent> getResultType() {
            return WebContent.class;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Arrays.asList(SEARCH_ENGINE_ID_OPTION, API_KEY_OPTION);
        }

        @Override
        public GoogleProgrammableSearchEngine create(Map<ConfigurationOption<?>, ?> config) {
            var apiKey = API_KEY_OPTION.get(config);
            var searchEngineId = SEARCH_ENGINE_ID_OPTION.get(config);
            return new GoogleProgrammableSearchEngine(apiKey, searchEngineId);
        }

        @Override
        public String getSearcherDocumentationUrl() {
            return "https://developers.google.com/custom-search/v1/overview";
        }

        @Override
        public String getSearcherDescription() {
            return "Searcher for <a href=\"https://programmablesearchengine.google.com/about/\"> "
                    + "Google Programmable Search Engine</a> (previously “Google Custom Search”).";
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleProgrammableSearchEngine.class);

    /** The name of this WebSearcher. */
    private static final String SEARCHER_NAME = "Google Programmable Search Engine";

    /** The identifier of the {@link Configuration} key for the api key. */
    public static final String CONFIG_API_KEY = "api.google.key";

    /** The identifier of the {@link Configuration} key for the search engine identifier. */
    public static final String CONFIG_SEARCH_ENGINE_IDENTIFIER = "api.google.customSearch.identifier";

    /** The API key for accessing the service. */
    private final String apiKey;

    /** The identifier of the Google Custom Search engine. */
    private final String searchEngineIdentifier;

    private final HttpRetriever retriever;

    /**
     * Creates a new GoogleProgrammableSearchEngine.
     *
     * @param apiKey                 The API key for accessing Google Custom Search, not empty or <code>null</code>.
     * @param searchEngineIdentifier The identifier of the custom search, not empty or <code>null</code>.
     */
    public GoogleProgrammableSearchEngine(String apiKey, String searchEngineIdentifier) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        Validate.notEmpty(searchEngineIdentifier, "searchEngineIdentifier must not be empty");

        this.apiKey = apiKey;
        this.searchEngineIdentifier = searchEngineIdentifier;
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    /**
     * Creates a new GoogleProgrammableSearchEngine.
     *
     * @param configuration The configuration which must provide an API key with the identifier {@value #CONFIG_API_KEY}
     *                      and a search engine identifier {@value #CONFIG_SEARCH_ENGINE_IDENTIFIER}.
     */
    public GoogleProgrammableSearchEngine(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY), configuration.getString(CONFIG_SEARCH_ENGINE_IDENTIFIER));
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {

        List<WebContent> results = new ArrayList<>();
        Long resultCount = null;

        // Google Custom Search gives chunks of max. 10 items, and allows 10 chunks, i.e. max. 100 results.
        double numChunks = Math.min(10, Math.ceil((double) query.getResultCount() / 10));

        for (int start = 1; start <= numChunks; start++) {

            String searchUrl = createRequestUrl(query.getText(), start, Math.min(10, query.getResultCount() - results.size()), query.getLanguage());
            LOGGER.debug("Search with URL " + searchUrl);

            HttpResult httpResult;
            try {
                httpResult = retriever.httpGet(searchUrl);
            } catch (HttpException e) {
                throw new SearcherException("HTTP exception while accessing URL \"" + searchUrl + "\": " + e.getMessage(), e);
            }

            String jsonString = httpResult.getStringContent();
            checkError(jsonString);
            try {
                List<WebContent> current = parse(jsonString);
                if (current.isEmpty()) {
                    break;
                }
                results.addAll(current);
                if (resultCount == null) {
                    resultCount = parseResultCount(jsonString);
                }
            } catch (JsonException e) {
                throw new SearcherException("Error parsing the response from URL \"" + searchUrl + "\" (JSON was: \"" + jsonString + "\"): " + e.getMessage(), e);
            }
        }

        return new SearchResults<WebContent>(results, resultCount);
    }

    private String createRequestUrl(String query, int start, int num, Language language) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://www.googleapis.com/customsearch/v1");
        urlBuilder.append("?key=").append(apiKey);
        urlBuilder.append("&cx=").append(searchEngineIdentifier);
        urlBuilder.append("&q=").append(UrlHelper.encodeParameter(query));
        urlBuilder.append("&start=").append(start);
        urlBuilder.append("&num=").append(num);
        if (language != null) {
            urlBuilder.append("&lr=").append(getLanguageCode(language));
        }
        urlBuilder.append("&alt=json");
        return urlBuilder.toString();
    }

    private String getLanguageCode(Language language) {
        switch (language) {
            case ARABIC:
            case BULGARIAN:
            case CATALAN:
            case CZECH:
            case DANISH:
            case GERMAN:
            case GREEK:
            case ENGLISH:
            case SPANISH:
            case ESTONIAN:
            case FINNISH:
            case FRENCH:
            case CROATIAN:
            case HUNGARIAN:
            case INDONESIAN:
            case ICELANDIC:
            case ITALIAN:
            case JAPANESE:
            case KOREAN:
            case LITHUANIAN:
            case LATVIAN:
            case DUTCH:
            case NORWEGIAN:
            case POLISH:
            case PORTUGUESE:
            case ROMANIAN:
            case RUSSIAN:
            case SLOVAK:
            case SLOVENE:
            case SERBIAN:
            case SWEDISH:
            case TURKISH:
            	return "lang_" + language.getIso6391();
            case HEBREW:
                return "lang_iw";
            case CHINESE:
                return "lang_zh-CN";
            default:
                throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    static void checkError(String jsonString) throws SearcherException {
        if (StringUtils.isBlank(jsonString)) {
            throw new SearcherException("JSON response is empty.");
        }
        try {
            JsonObject jsonObject = new JsonObject(jsonString);
            JsonObject jsonError = jsonObject.getJsonObject("error");
            if (jsonError != null) {
                int errorCode = jsonError.getInt("code");
                String message = jsonError.getString("message");
                throw new SearcherException("Error from API: " + message + " (" + errorCode + ").");
            }
        } catch (JsonException e) {
            throw new SearcherException("Could not parse JSON response ('" + jsonString + "').", e);
        }
    }

    /** default visibility for unit testing. */
    static List<WebContent> parse(String jsonString) throws JsonException {
        JsonObject jsonObject = new JsonObject(jsonString);
        JsonArray jsonItems = jsonObject.getJsonArray("items");
        if (jsonItems == null) {
            LOGGER.warn("JSON result did not contain an 'items' property. (JSON = '" + jsonString + "'.");
            return Collections.emptyList();
        }
        List<WebContent> result = new ArrayList<>();
        for (int i = 0; i < jsonItems.size(); i++) {
            JsonObject jsonItem = jsonItems.getJsonObject(i);
            BasicWebContent.Builder builder = new BasicWebContent.Builder();
            builder.setTitle(jsonItem.getString("title"));
            builder.setUrl(jsonItem.getString("link"));
            builder.setSummary(jsonItem.tryGetString("snippet"));
            builder.setSource(SEARCHER_NAME);
            result.add(builder.create());
        }
        return result;
    }

    /** default visibility for unit testing. */
    static long parseResultCount(String jsonString) throws JsonException {
        JsonObject jsonObject = new JsonObject(jsonString);
        return jsonObject.getJsonObject("searchInformation").getLong("totalResults");
    }

}
