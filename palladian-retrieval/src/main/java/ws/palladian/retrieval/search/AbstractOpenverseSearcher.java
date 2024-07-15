package ws.palladian.retrieval.search;

import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.*;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.configuration.StringConfigurationOption;
import ws.palladian.retrieval.resources.WebContent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Base class for Openverse searchers (image, audio).
 *
 * @author David Urbansky
 * @author Philipp Katz
 * @see <a href="https://api.openverse.org/v1/">Openverse API</a>
 */
public abstract class AbstractOpenverseSearcher<T extends WebContent> extends AbstractMultifacetSearcher<T> {

    public static abstract class AbstractOpenverseSearcherMetaInfo<T extends WebContent> implements SearcherMetaInfo<AbstractOpenverseSearcher<T>, T> {
        private static final StringConfigurationOption CLIENT_ID = new StringConfigurationOption("Client ID", "client_id", null, false);
        private static final StringConfigurationOption CLIENT_SECRET = new StringConfigurationOption("Client Secret", "client_secret", null, false);

        @Override
        public final List<ConfigurationOption<?>> getConfigurationOptions() {
            return Arrays.asList(CLIENT_ID, CLIENT_SECRET);
        }

        @Override
        public final AbstractOpenverseSearcher<T> create(Map<ConfigurationOption<?>, ?> config) {
            var clientId = CLIENT_ID.get(config);
            var clientSecret = CLIENT_SECRET.get(config);
            return create(clientId, clientSecret);
        }

        protected abstract AbstractOpenverseSearcher<T> create(String clientId, String clientSecret);

        @Override
        public final String getSearcherDocumentationUrl() {
            return "https://api.openverse.org/v1/";
        }
    }

    // Maximum is 500 for authenticated requests, and 20 for unauthenticated
    // requests.
    private static final int MAX_PER_PAGE_UNAUTHENTICATED = 20;
    private static final int MAX_PER_PAGE_AUTHENTICATED = 50;

    private String licenses = "all-cc,commercial";

    /** If null, search all sources. */
    private String sources = null;

    private final String clientId;

    private final String clientSecret;

    private String accessToken = null;
    private Long accessTokenExpirationTs = null;

    protected AbstractOpenverseSearcher(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public final SearchResults<T> search(MultifacetQuery query) throws SearcherException {
        String accessToken = null;
        if (clientId != null && clientSecret != null) {
            accessToken = getAccessToken();
        }

        List<T> results = new ArrayList<>();
        Long totalAvailableResults = null;

        var resultCountTemp = defaultResultCount == null ? query.getResultCount() : defaultResultCount;
        var resultCount = Math.min(10000, resultCountTemp);
        int maxPerPage = accessToken != null ? MAX_PER_PAGE_AUTHENTICATED : MAX_PER_PAGE_UNAUTHENTICATED;
        int resultsPerPage = Math.min(maxPerPage, resultCount);
        int pagesNeeded = (int) Math.ceil(resultCount / (double) resultsPerPage);

        for (int page = 1; page <= pagesNeeded; page++) {
            String requestUrl = buildRequest(query.getText(), page, Math.min(maxPerPage, resultCount - results.size()));
            try {
                var requestBuilder = new HttpRequest2Builder(HttpMethod.GET, requestUrl);
                if (accessToken != null) {
                    requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
                }
                var jsonResponse = HttpRetrieverFactory.getHttpRetriever().execute(requestBuilder.create());
                if (jsonResponse.errorStatus()) {
                    throw new SearcherException("Failed to get JSON from " + requestUrl);
                }
                JsonObject json = new JsonObject(jsonResponse.getStringContent(StandardCharsets.UTF_8));
                if (totalAvailableResults == null) {
                    totalAvailableResults = json.tryGetLong("result_count");
                }
                JsonArray jsonArray = json.getJsonArray("results");
                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonObject resultHit = jsonArray.getJsonObject(i);
                        results.add(parseResult(resultHit));
                        if (results.size() >= resultCount) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                throw new SearcherException(e);
            }
        }

        return new SearchResults<>(results, totalAvailableResults);
    }

    protected abstract T parseResult(JsonObject json);

    private String getAccessToken() throws SearcherException {
        if (accessToken != null && accessTokenExpirationTs != null && System.currentTimeMillis() < accessTokenExpirationTs) {
            return accessToken;
        }
        var requestBuilder = new HttpRequest2Builder(HttpMethod.POST, "https://api.openverse.org/v1/auth_tokens/token/");
        var formEntityBuilder = new FormEncodedHttpEntity.Builder();
        formEntityBuilder.addData("client_id", clientId);
        formEntityBuilder.addData("client_secret", clientSecret);
        formEntityBuilder.addData("grant_type", "client_credentials");
        requestBuilder.setEntity(formEntityBuilder.create());
        var request = requestBuilder.create();
        HttpResult response;
        try {
            response = HttpRetrieverFactory.getHttpRetriever().execute(request);
        } catch (HttpException e) {
            throw new SearcherException("HTTP error", e);
        }
        if (response.errorStatus()) {
            throw new SearcherException("HTTP status " + response.getStatusCode() + " from token endpoint: " + response.getStringContent());
        }
        try {
            JsonObject jsonResponse = new JsonObject(response.getStringContent());
            accessToken = jsonResponse.getString("access_token");
            accessTokenExpirationTs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(jsonResponse.getInt("expires_in"));
            return accessToken;
        } catch (JsonException e) {
            throw new SearcherException("Could not parse JSON: " + response.getStringContent(), e);
        }
    }

    protected abstract String buildRequest(String searchTerms, int page, int resultsPerPage);

    public String getLicenses() {
        return licenses;
    }

    public void setLicenses(String licenses) {
        this.licenses = licenses;
    }

    public String getSources() {
        return sources;
    }

    public void setSources(String sources) {
        this.sources = sources;
    }

}
