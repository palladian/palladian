package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpMethod;
import ws.palladian.retrieval.HttpRequest2Builder;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.StringHttpEntity;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.configuration.StringConfigurationOption;
import ws.palladian.retrieval.helper.FixedIntervalRequestThrottle;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.SearcherException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * Web searcher for the <a href="https://dataforseo.com/apis/serp-api">DataForSEO SERP API</a>. Uses the
 * <code>google/organic/live/regular</code> endpoint to obtain Google SERP results. Authentication is performed via
 * HTTP Basic auth with the account login (email) and password (or API key) from
 * <a href="https://app.dataforseo.com/api-access">app.dataforseo.com/api-access</a>.
 * </p>
 *
 * @author David Urbansky
 * @see <a href="https://docs.dataforseo.com/v3/serp/google/organic/live/regular/">DataForSEO SERP API docs</a>
 */
public final class DataForSeoSearcher extends AbstractSearcher<WebContent> {

    private static final String NAME = "DataForSEO";

    /** Default location code: United States. */
    public static final int DEFAULT_LOCATION_CODE = 2840;

    /** Default language code if no language is supplied. */
    private static final String DEFAULT_LANGUAGE_CODE = "en";

    /** DataForSEO success status code. */
    private static final int STATUS_OK = 20000;

    /** Maximum number of results per request, as enforced by DataForSEO. */
    private static final int MAX_DEPTH = 200;

    private static final String ENDPOINT = "https://api.dataforseo.com/v3/serp/google/organic/live/regular";

    /** Identifier of the {@link Configuration} key for the DataForSEO login (account email). */
    public static final String CONFIG_LOGIN = "api.dataforseo.login";

    /** Identifier of the {@link Configuration} key for the DataForSEO password (or API key). */
    public static final String CONFIG_PASSWORD = "api.dataforseo.password";

    public static final class DataForSeoSearcherMetaInfo implements SearcherMetaInfo<DataForSeoSearcher, WebContent> {
        private static final StringConfigurationOption LOGIN_OPTION = new StringConfigurationOption("Login", "login");
        private static final StringConfigurationOption PASSWORD_OPTION = new StringConfigurationOption("Password", "password");
        private static final StringConfigurationOption LOCATION_CODE_OPTION = new StringConfigurationOption("Location Code", "location_code",
                String.valueOf(DEFAULT_LOCATION_CODE));

        @Override
        public String getSearcherName() {
            return NAME;
        }

        @Override
        public String getSearcherId() {
            return "data_for_seo";
        }

        @Override
        public Class<WebContent> getResultType() {
            return WebContent.class;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Arrays.asList(LOGIN_OPTION, PASSWORD_OPTION, LOCATION_CODE_OPTION);
        }

        @Override
        public DataForSeoSearcher create(Map<ConfigurationOption<?>, ?> config) {
            String login = LOGIN_OPTION.get(config);
            String password = PASSWORD_OPTION.get(config);
            String locationCodeString = LOCATION_CODE_OPTION.get(config);
            int locationCode = DEFAULT_LOCATION_CODE;
            if (locationCodeString != null && !locationCodeString.isEmpty()) {
                try {
                    locationCode = Integer.parseInt(locationCodeString.trim());
                } catch (NumberFormatException e) {
                    // fall back to default
                }
            }
            return new DataForSeoSearcher(login, password, locationCode);
        }

        @Override
        public String getSearcherDocumentationUrl() {
            return "https://docs.dataforseo.com/v3/serp/google/organic/live/regular/";
        }

        @Override
        public String getSearcherDescription() {
            return "Searcher for the <a href=\"https://dataforseo.com/apis/serp-api\">DataForSEO SERP API</a>. "
                    + "Returns Google organic search results via the <code>google/organic/live/regular</code> endpoint.";
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DataForSeoSearcher.class);

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    /**
     * Throttle to avoid hammering the API.
     */
    private static final FixedIntervalRequestThrottle THROTTLE = new FixedIntervalRequestThrottle(200);

    private final String login;
    private final String password;
    private final int locationCode;

    public DataForSeoSearcher(String login, String password) {
        this(login, password, DEFAULT_LOCATION_CODE);
    }

    public DataForSeoSearcher(String login, String password, int locationCode) {
        Validate.notEmpty(login, "login must not be empty");
        Validate.notEmpty(password, "password must not be empty");
        this.login = login;
        this.password = password;
        this.locationCode = locationCode;
    }

    /**
     * Creates a new {@link DataForSeoSearcher} pulling the login and password from the given configuration via the
     * keys {@value #CONFIG_LOGIN} and {@value #CONFIG_PASSWORD}.
     *
     * @param configuration The configuration providing the credentials.
     */
    public DataForSeoSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_LOGIN), configuration.getString(CONFIG_PASSWORD));
    }

    @Override
    public List<WebContent> search(String query, int resultCount, Language language) throws SearcherException {
        int depth = Math.min(MAX_DEPTH, Math.max(1, resultCount));

        JsonObject task = new JsonObject();
        task.put("keyword", query);
        task.put("location_code", locationCode);
        task.put("language_code", getLanguageCode(language));
        task.put("depth", depth);

        JsonArray body = new JsonArray();
        body.add(task);

        HttpRequest2Builder requestBuilder = new HttpRequest2Builder(HttpMethod.POST, ENDPOINT);
        requestBuilder.setBasicAuth(login, password);
        requestBuilder.addHeader("Content-Type", "application/json");
        requestBuilder.setEntity(new StringHttpEntity(body.toString(), ContentType.APPLICATION_JSON));

        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();

        HttpResult httpResult;
        try {
            THROTTLE.hold();
            TOTAL_REQUEST_COUNT.incrementAndGet();
            httpResult = httpRetriever.execute(requestBuilder.create());
        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": " + e.getMessage(), e);
        }

        String responseString = httpResult.getStringContent();
        LOGGER.debug("Response: {}", responseString);

        if (httpResult.errorStatus()) {
            throw new SearcherException(
                    "HTTP status " + httpResult.getStatusCode() + " from " + getName() + " (response: \"" + responseString + "\").");
        }

        return parse(responseString, resultCount);
    }

    static List<WebContent> parse(String responseString, int resultCount) throws SearcherException {
        List<WebContent> results = new ArrayList<>();
        if (responseString == null || responseString.isEmpty()) {
            throw new SearcherException("Empty response from " + NAME + ".");
        }

        JsonObject response;
        try {
            response = new JsonObject(responseString);
        } catch (JsonException e) {
            throw new SearcherException("Could not parse JSON response from " + NAME + ": " + e.getMessage() + " (response: \"" + responseString + "\")", e);
        }

        int statusCode = response.tryGetInt("status_code") != null ? response.tryGetInt("status_code") : 0;
        if (statusCode != STATUS_OK) {
            String statusMessage = response.tryGetString("status_message");
            throw new SearcherException("Error from " + NAME + " API: " + statusMessage + " (status_code=" + statusCode + ").");
        }

        JsonArray tasks = response.tryGetJsonArray("tasks");
        if (tasks == null || tasks.isEmpty()) {
            return results;
        }

        for (int taskIdx = 0; taskIdx < tasks.size(); taskIdx++) {
            JsonObject task = tasks.tryGetJsonObject(taskIdx);
            if (task == null) {
                continue;
            }
            Integer taskStatus = task.tryGetInt("status_code");
            if (taskStatus != null && taskStatus != STATUS_OK) {
                String statusMessage = task.tryGetString("status_message");
                throw new SearcherException("Task error from " + NAME + " API: " + statusMessage + " (status_code=" + taskStatus + ").");
            }
            JsonArray taskResults = task.tryGetJsonArray("result");
            if (taskResults == null) {
                continue;
            }
            for (int resultIdx = 0; resultIdx < taskResults.size(); resultIdx++) {
                JsonObject taskResult = taskResults.tryGetJsonObject(resultIdx);
                if (taskResult == null) {
                    continue;
                }
                JsonArray items = taskResult.tryGetJsonArray("items");
                if (items == null) {
                    continue;
                }
                for (int itemIdx = 0; itemIdx < items.size(); itemIdx++) {
                    JsonObject item = items.tryGetJsonObject(itemIdx);
                    if (item == null) {
                        continue;
                    }
                    if (!"organic".equals(item.tryGetString("type"))) {
                        continue;
                    }
                    String url = item.tryGetString("url");
                    if (url == null || url.isEmpty()) {
                        continue;
                    }
                    BasicWebContent.Builder builder = new BasicWebContent.Builder();
                    builder.setTitle(item.tryGetString("title"));
                    builder.setSummary(item.tryGetString("description"));
                    builder.setUrl(url);
                    builder.setSource(NAME);
                    Integer rank = item.tryGetInt("rank_absolute");
                    if (rank != null) {
                        builder.setAdditionalData("rank_absolute", rank);
                    }
                    String domain = item.tryGetString("domain");
                    if (domain != null) {
                        builder.setAdditionalData("domain", domain);
                    }
                    results.add(builder.create());
                }
            }
        }

        return CollectionHelper.getSublist(results, 0, resultCount);
    }

    private static String getLanguageCode(Language language) {
        if (language == null) {
            return DEFAULT_LANGUAGE_CODE;
        }
        String code = language.getIso6391();
        return code != null && !code.isEmpty() ? code : DEFAULT_LANGUAGE_CODE;
    }

    @Override
    public String getName() {
        return NAME;
    }

    /**
     * @return The number of HTTP requests sent to DataForSEO.
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.get();
    }
}
