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
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpMethod;
import ws.palladian.retrieval.HttpRequest2Builder;
import ws.palladian.retrieval.HttpRetrieverFactory;
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
 * Search for free images on <a href="https://unsplash.com/documentation">Unsplash</a>.
 * </p>
 *
 * @author David Urbansky
 * @see <a href="https://unsplash.com/documentation">Unsplash API Docs</a>
 */
public class UnsplashSearcher extends AbstractSearcher<WebImage> {
    public static final class UnsplashSearcherMetaInfo implements SearcherMetaInfo<UnsplashSearcher, WebImage> {
        private static final StringConfigurationOption ACCESS_KEY_OPTION = new StringConfigurationOption("Application Access Key",
                "access_key");

        @Override
        public String getSearcherName() {
            return SEARCHER_NAME;
        }

        @Override
        public String getSearcherId() {
            return "unsplash";
        }

        @Override
        public Class<WebImage> getResultType() {
            return WebImage.class;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Arrays.asList(ACCESS_KEY_OPTION);
        }

        @Override
        public UnsplashSearcher create(Map<ConfigurationOption<?>, ?> config) {
            var accessKey = ACCESS_KEY_OPTION.get(config);
            return new UnsplashSearcher(accessKey);
        }

        @Override
        public String getSearcherDocumentationUrl() {
            return "https://unsplash.com/documentation";
        }

        @Override
        public String getSearcherDescription() {
            return "Search for free images on <a href=\"https://unsplash.com/documentation\">Unsplash</a>.";
        }
    }

    /** The name of this searcher. */
    private static final String SEARCHER_NAME = "Unsplash";

    /** Identifier for the API key when supplied via {@link Configuration}. */
    public static final String CONFIG_ACCESS_KEY = "api.unsplash.key";

    private static final int MAX_PER_PAGE = 30;

    private final String apiKey;

    /**
     * <p>
     * Creates a new Unsplash searcher.
     * </p>
     *
     * @param accessKey The API key for accessing Unsplash, not <code>null</code> or empty.
     */
    public UnsplashSearcher(String accessKey) {
        Validate.notEmpty(accessKey, "accessKey must not be empty");
        this.apiKey = accessKey;
    }

    public UnsplashSearcher(String accessKey, int defaultResultCount) {
        this(accessKey);
        this.defaultResultCount = defaultResultCount;
    }

    /**
     * <p>
     * Creates a new Unsplash searcher.
     * </p>
     *
     * @param configuration The configuration which must provide an API key for accessing Unsplash, which must be
     *                      provided as string via key {@value UnsplashSearcher#CONFIG_ACCESS_KEY} in the configuration.
     */
    public UnsplashSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_ACCESS_KEY));
    }

    public UnsplashSearcher(Configuration config, int defaultResultCount) {
        this(config);
        this.defaultResultCount = defaultResultCount;
    }

    @Override
    /**
     * @param language Supported languages are English.
     */ public List<WebImage> search(String query, int resultCount, Language language) throws SearcherException {
        return search(query, resultCount, language, null);
    }

    public List<WebImage> search(String query, int resultCount, Language language, Orientation orientation) throws SearcherException {
        List<WebImage> results = new ArrayList<>();

        resultCount = defaultResultCount == null ? resultCount : defaultResultCount;
        resultCount = Math.min(1000, resultCount);
        int resultsPerPage = Math.min(MAX_PER_PAGE, resultCount);
        int pagesNeeded = (int) Math.ceil(resultCount / (double) resultsPerPage);

        var retriever = HttpRetrieverFactory.getHttpRetriever();

        for (int page = 1; page <= pagesNeeded; page++) {
            String requestUrl = buildRequest(query, page, Math.min(MAX_PER_PAGE, resultCount - results.size()), orientation);
            try {
                var request = new HttpRequest2Builder(HttpMethod.GET, requestUrl).addHeader("Authorization", "Client-ID " + apiKey).create();
                var response = retriever.execute(request);
                if (response.errorStatus()) {
                    throw new SearcherException("Encountered HTTP status " + response.getStatusCode());
                }
                JsonObject json = new JsonObject(response.getStringContent());
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
                throw new SearcherException(e);
            } catch (HttpException e) {
                throw new SearcherException(e);
            }
        }

        return results;
    }

    private String buildRequest(String searchTerms, int page, int resultsPerPage, Orientation orientation) {
        String request = String.format("https://api.unsplash.com/search/photos?query=%s&per_page=%s&page=%s", UrlHelper.encodeParameter(searchTerms), resultsPerPage, page);
        if (orientation != null) {
            request += "&orientation=" + orientation.name().toLowerCase();
        }
        return request;
    }

    public JsonObject getPhotoInformation(String photoId) {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        Map<String, String> globalHeaders = new HashMap<>();
        globalHeaders.put("Authorization", "Client-ID " + apiKey);
        documentRetriever.setGlobalHeaders(globalHeaders);

        return documentRetriever.tryGetJsonObject("https://api.unsplash.com/photos/" + photoId);
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    public static void main(String[] args) throws SearcherException {
        UnsplashSearcher searcher = new UnsplashSearcher("KEY");
        List<WebImage> results = searcher.search("nature", 101, Language.ENGLISH, Orientation.PORTRAIT);
        JsonObject information = searcher.getPhotoInformation((String) results.get(0).getAdditionalData().get("id"));
        System.out.println(information);
        CollectionHelper.print(results);
    }
}
