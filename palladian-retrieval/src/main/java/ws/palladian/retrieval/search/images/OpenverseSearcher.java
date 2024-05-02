package ws.palladian.retrieval.search.images;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.FormEncodedHttpEntity;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpMethod;
import ws.palladian.retrieval.HttpRequest2Builder;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.configuration.StringConfigurationOption;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.SearcherException;

/**
 * Search for free images on <a href="https://openverse.org">Openverse</a>.
 *
 * @author David Urbansky
 * @see <a href="https://api.openverse.engineering/v1/">Creative Commons API
 *      Docs</a>
 */
public class OpenverseSearcher extends AbstractSearcher<WebImage> {
    public static final class OpenverseSearcherMetaInfo implements SearcherMetaInfo<OpenverseSearcher, WebImage> {
        private static final StringConfigurationOption CLIENT_ID = new StringConfigurationOption("Client ID",
                "client_id", null, false);
        private static final StringConfigurationOption CLIENT_SECRET = new StringConfigurationOption("Client Secret",
                "client_secret", null, false);

        @Override
        public String getSearcherName() {
            return SEARCHER_NAME;
        }

        @Override
        public String getSearcherId() {
            return "openverse";
        }

        @Override
        public Class<WebImage> getResultType() {
            return WebImage.class;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Arrays.asList(CLIENT_ID, CLIENT_SECRET);
        }

        @Override
        public OpenverseSearcher create(Map<ConfigurationOption<?>, ?> config) {
            var clientId = CLIENT_ID.get(config);
            var clientSecret = CLIENT_SECRET.get(config);
            return new OpenverseSearcher(clientId, clientSecret);
        }
    }

    /** The name of this searcher. */
    private static final String SEARCHER_NAME = "Openverse";

    // Maximum is 500 for authenticated requests, and 20 for unauthenticated
    // requests.
    private static final int MAX_PER_PAGE_UNAUTHENTICATED = 20;
    private static final int MAX_PER_PAGE_AUTHENTICATED = 500;

    private String licenses = "all-cc,commercial";

    /** If null, search all sources. */
    private String sources = null;

    private final String clientId;

    private final String clientSecret;

    public OpenverseSearcher() {
        this(null, null);
    }

    public OpenverseSearcher(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public List<WebImage> search(String query, int resultCount, Language language) throws SearcherException {
        String accessToken = null;
        if (clientId != null && clientSecret != null) {
            accessToken = getAccessToken();
            // TODO cache this
        }

        List<WebImage> results = new ArrayList<>();

        resultCount = Math.min(10000, resultCount);
        int maxPerPage = accessToken != null ? MAX_PER_PAGE_AUTHENTICATED : MAX_PER_PAGE_UNAUTHENTICATED;
        int resultsPerPage = Math.min(maxPerPage, resultCount);
        int pagesNeeded = (int) Math.ceil(resultCount / (double) resultsPerPage);

        for (int page = 1; page <= pagesNeeded; page++) {
            String requestUrl = buildRequest(query, page, Math.min(maxPerPage, resultCount - results.size()));
            try {
                var requestBuilder = new HttpRequest2Builder(HttpMethod.GET, requestUrl);
                if (accessToken != null) {
                    requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
                }
                var jsonResponse = HttpRetrieverFactory.getHttpRetriever().execute(requestBuilder.create());
                if (jsonResponse.errorStatus()) {
                    throw new SearcherException("Failed to get JSON from " + requestUrl);
                }
                JsonObject json = new JsonObject(jsonResponse.getStringContent());
                JsonArray jsonArray = json.getJsonArray("results");
                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonObject resultHit = jsonArray.getJsonObject(i);
                        BasicWebImage.Builder builder = new BasicWebImage.Builder();
                        builder.setAdditionalData("id", resultHit.tryQueryString("id"));
                        builder.setUrl(resultHit.tryQueryString("url"));
                        builder.setImageUrl(resultHit.tryQueryString("url"));
                        builder.setTitle(resultHit.tryQueryString("title"));
                        builder.setWidth(Optional.ofNullable(resultHit.tryGetInt("width")).orElse(0));
                        builder.setHeight(Optional.ofNullable(resultHit.tryGetInt("height")).orElse(0));
                        builder.setImageType(ImageType.UNKNOWN);
                        builder.setThumbnailUrl(resultHit.tryQueryString("thumbnail"));
                        builder.setLicense(License.FREE);
                        builder.setLicenseLink(resultHit.tryQueryString("license_url"));
                        results.add(builder.create());
                        if (results.size() >= resultCount) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                throw new SearcherException(e);
            }
        }

        return results;
    }

    private String getAccessToken() throws SearcherException {
        var requestBuilder = new HttpRequest2Builder(HttpMethod.POST,
                "https://api.openverse.engineering/v1/auth_tokens/token/");
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
            throw new SearcherException(
                    "HTTP status " + response.getStatusCode() + " from token endpoint: " + response.getStringContent());
        }
        try {
            var jsonResponse = new JsonObject(response.getStringContent());
            return jsonResponse.getString("access_token");
        } catch (JsonException e) {
            throw new SearcherException("Could not parse JSON: " + response.getStringContent(), e);
        }
    }

    private String buildRequest(String searchTerms, int page, int resultsPerPage) {
        String url = String.format(
                "https://api.openverse.engineering/v1/images/?q=%s&license_type=%s&page=%s&page_size=%s&mature=true",
                UrlHelper.encodeParameter(searchTerms), licenses, page, resultsPerPage);
        if (this.sources != null) {
            url += "&source=" + this.sources;
        }

        System.out.println("request= " + url);
        return url;
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

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

    public static void main(String[] args) throws SearcherException {
        var searcher = new OpenverseSearcher();
//		var searcher = new OpenverseSearcher("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
//				"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        searcher.setSources(
                "wikimedia,thorvaldsensmuseum,thingiverse,svgsilh,sketchfab,rijksmuseum,rawpixel,phylopic,nypl,museumsvictoria,met,mccordmuseum,iha,geographorguk,floraon,eol,digitaltmuseum,deviantart,clevelandmuseum,brooklynmuseum,behance,animaldiversity,WoRMS,CAPL,500px");
        List<WebImage> results = searcher.search("brain", 100);
        CollectionHelper.print(results);
    }
}
