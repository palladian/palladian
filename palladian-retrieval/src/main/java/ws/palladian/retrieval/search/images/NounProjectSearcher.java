package ws.palladian.retrieval.search.images;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpMethod;
import ws.palladian.retrieval.HttpRequest2Builder;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.OAuthParams;
import ws.palladian.retrieval.OAuthUtil;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.configuration.StringConfigurationOption;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Search for images on <a href="https://thenounproject.com/">the noun project</a>.
 *
 * @author David Urbansky
 * @see <a href="thenounproject.com">Nounproject API</a>
 */
public class NounProjectSearcher extends AbstractMultifacetSearcher<WebImage> {
    public static final class NounProjectSearcherMetaInfo implements SearcherMetaInfo<NounProjectSearcher, WebImage> {
        private static final StringConfigurationOption API_KEY_OPTION = new StringConfigurationOption("API Key", "apikey");
        private static final StringConfigurationOption API_KEY_SECRET_OPTION = new StringConfigurationOption("API Secret Key", "apiSecret");

        @Override
        public String getSearcherName() {
            return SEARCHER_NAME;
        }

        @Override
        public String getSearcherId() {
            return "nounproject";
        }

        @Override
        public Class<WebImage> getResultType() {
            return WebImage.class;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Arrays.asList(API_KEY_OPTION, API_KEY_SECRET_OPTION);
        }

        @Override
        public NounProjectSearcher create(Map<ConfigurationOption<?>, ?> config) {
            var apiKey = API_KEY_OPTION.get(config);
            var apiKeySecret = API_KEY_SECRET_OPTION.get(config);
            return new NounProjectSearcher(apiKey, apiKeySecret);
        }

        @Override
        public String getSearcherDocumentationUrl() {
            return "https://api.thenounproject.com/documentation.html";
        }

        @Override
        public String getSearcherDescription() {
            return "Search for public domain images on <a href=\"https://thenounproject.com\">The Noun Project</a>.";
        }
    }

    /** The name of this searcher. */
    private static final String SEARCHER_NAME = "Nounproject";

    /** Identifier for the API key when supplied via {@link Configuration}. */
    public static final String CONFIG_API_KEY = "api.nounproject.key";
    public static final String CONFIG_API_SECRET = "api.nounproject.secret";

    private final String apiKey;
    private final String apiSecret;

    private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.MINUTES, 100);

    /**
     * Creates a new NounProject searcher.
     */
    public NounProjectSearcher(String apiKey, String apiKeySecret) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        Validate.notEmpty(apiKeySecret, "apiKeySecret must not be empty");
        this.apiKey = apiKey;
        this.apiSecret = apiKeySecret;
    }

    public NounProjectSearcher(String apiKey, String apiKeySecret, int defaultResultCount) {
        this(apiKey, apiKeySecret);
        this.defaultResultCount = defaultResultCount;
    }

    /**
     * Creates a new Nounproject searcher.
     */
    public NounProjectSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY), configuration.getString(CONFIG_API_SECRET));
    }

    public NounProjectSearcher(Configuration config, int defaultResultCount) {
        this(config);
        this.defaultResultCount = defaultResultCount;
    }

    @Override
    public SearchResults<WebImage> search(MultifacetQuery query) throws SearcherException {
        List<WebImage> results = new ArrayList<>();
        Long totalResults = null;

        var resultCount = defaultResultCount == null ? query.getResultCount() : defaultResultCount;
        resultCount = Math.min(1000, resultCount);
        int resultsPerPage = Math.min(50, resultCount);

        String page = "";
        while (results.size() < resultCount) {
            try {
                THROTTLE.hold();
                String requestUrl = "https://api.thenounproject.com/v2/icon?query=" + query.getText() + "&limit=" + resultsPerPage + "&page=" + page;
                if (query.getFacet("onlyPublicDomain") != null && query.getFacet("onlyPublicDomain").getValue().equals("true")) {
                    requestUrl += "&limit_to_public_domain=1";
                }
                JsonObject json = makeRequest(requestUrl);
                if (totalResults == null) {
                    totalResults = json.getLong("total");
                }
                JsonArray jsonArray = Optional.ofNullable(json).orElse(new JsonObject()).tryGetJsonArray("icons");
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject resultHit = jsonArray.getJsonObject(i);
                    String licenseDescription = resultHit.tryGetString("license_description");
                    BasicWebImage.Builder builder = new BasicWebImage.Builder();
                    builder.setAdditionalData("id", resultHit.tryGetString("id"));
                    builder.setAdditionalData("attribution", resultHit.tryGetString("attribution"));
                    builder.setUrl("https://thenounproject.com" + resultHit.getString("permalink"));
                    builder.setImageUrl(resultHit.getString("thumbnail_url"));
                    builder.setTitle(json.tryGetString("term"));
                    builder.setWidth(200);
                    builder.setHeight(200);
                    builder.setImageType(ImageType.CLIPART);
                    builder.setThumbnailUrl(resultHit.getString("thumbnail_url"));
                    builder.setLicense(licenseDescription.equals("public-domain") ? License.PUBLIC_DOMAIN : License.ATTRIBUTION);
                    builder.setLicenseLink(licenseDescription);
                    results.add(builder.create());
                    if (results.size() >= resultCount) {
                        break;
                    }
                }
                page = json.tryGetString("next_page");
                if (page == null) {
                    break;
                }
            } catch (Exception e) {
                throw new SearcherException(e);
            }
        }

        return new SearchResults<>(results, totalResults);
    }

    private JsonObject makeRequest(String requestUrl) throws IOException, SearcherException {
        var oautUtil = new OAuthUtil(new OAuthParams(apiKey, apiSecret));
        var request = new HttpRequest2Builder(HttpMethod.GET, requestUrl).create();
        var signedRequest = oautUtil.createSignedRequest(request);
        var retriever = HttpRetrieverFactory.getHttpRetriever();
        var result = retriever.execute(signedRequest);
        if (result.errorStatus()) {
            throw new SearcherException("Request failed with status code " + result.getStatusCode());
        }

        return JsonObject.tryParse(result.getStringContent());
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    public static void main(String[] args) throws SearcherException {
        NounProjectSearcher searcher = new NounProjectSearcher("KEY", "SECRET");
        SearchResults<WebImage> searchResult = searcher.search(new MultifacetQuery.Builder().setText("car").setResultCount(101).create());
        System.out.println(searchResult.getResultCount());
        CollectionHelper.print(searchResult);
    }
}
