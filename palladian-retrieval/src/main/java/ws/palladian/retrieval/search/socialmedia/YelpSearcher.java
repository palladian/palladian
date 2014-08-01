package ws.palladian.retrieval.search.socialmedia;

import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.OAuthParams;
import ws.palladian.retrieval.OAuthUtil;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.Facet;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Searcher for locations on <a href="http://www.yelp.com">Yelp</a>.
 * 
 * @author pk
 * @see <a href="http://www.yelp.com/developers/documentation/v2/search_api">API overview</a>
 */
public final class YelpSearcher extends AbstractMultifacetSearcher<WebContent> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(YelpSearcher.class);

    /**
     * <p>
     * Filtering facet which allows to specify a category when searching. The supported categories by Yelp can be found
     * <a href="http://www.yelp.com/developers/documentation/category_list">here</a>. Usage as follows:
     * 
     * <pre>
     * MultifacetQuery query = new MultifacetQuery.Builder().setCoordinate(51.05268, 13.739176).setRadius(10.)
     *         .addFacet(new CategoryFilter(&quot;theater&quot;, &quot;ticketsales&quot;)).create();
     * </pre>
     * 
     * @author pk
     */
    public static final class CategoryFilter implements Facet {
        private static final String YELP_CATEGORY_FACET = "yelp.categories";
        private final Set<String> categories;

        public CategoryFilter(Set<String> categories) {
            Validate.notNull(categories, "categories must not be null");
            this.categories = categories;
        }

        public CategoryFilter(String... categories) {
            Validate.notNull(categories, "categories must not be null");
            this.categories = CollectionHelper.newHashSet(categories);
        }

        @Override
        public String getIdentifier() {
            return YELP_CATEGORY_FACET;
        }

        private String getCategories() {
            return StringUtils.join(categories, ",");
        }
    }

    /** The identifier for the {@link Configuration} key with the OAuth consumer key. */
    public static final String CONFIG_CONSUMER_KEY = "api.yelp.consumerKey";

    /** The identifier for the {@link Configuration} key with the OAuth consumer secret. */
    public static final String CONFIG_CONSUMER_SECRET = "api.yelp.consumerSecret";

    /** The identifier for the {@link Configuration} key with the OAuth access token. */
    public static final String CONFIG_ACCESS_TOKEN = "api.yelp.accessToken";

    /** The identifier for the {@link Configuration} key with the OAuth access token secret. */
    public static final String CONFIG_ACCESS_TOKEN_SECRET = "api.yelp.accessTokenSecret";

    /** Name of this searcher. */
    private static final String SEARCHER_NAME = "Yelp";

    /** For HTTP requests. */
    private static final HttpRetriever HTTP_RETRIEVER = HttpRetrieverFactory.getHttpRetriever();

    /** The authentication parameters. */
    private final OAuthParams oAuthParams;

    /**
     * <p>
     * Create a new {@link YelpSearcher}.
     * 
     * @param oAuthParams The parameters for oAuth authentication.
     */
    public YelpSearcher(OAuthParams oAuthParams) {
        Validate.notNull(oAuthParams, "oAuthParams must not be null");
        this.oAuthParams = oAuthParams;
    }

    /**
     * <p>
     * Create a new {@link YelpSearcher}.
     * 
     * @param configuration A {@link Configuration} instance providing the necessary parameters for OAuth authentication
     *            ({@value #CONFIG_CONSUMER_KEY}, {@value #CONFIG_CONSUMER_SECRET}, {@value #CONFIG_ACCESS_TOKEN},
     *            {@value #CONFIG_ACCESS_TOKEN_SECRET}), not <code>null</code>.
     */
    public YelpSearcher(Configuration configuration) {
        this(new OAuthParams(configuration.getString(CONFIG_CONSUMER_KEY),
                configuration.getString(CONFIG_CONSUMER_SECRET), configuration.getString(CONFIG_ACCESS_TOKEN),
                configuration.getString(CONFIG_ACCESS_TOKEN_SECRET)));
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {
        Validate.notNull(query, "query must not be null");

        HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, "http://api.yelp.com/v2/search");
        if (StringUtils.isNotBlank(query.getText())) {
            httpRequest.addParameter("term", query.getText());
        }
        httpRequest.addParameter("limit", Math.min(20, query.getResultCount()));
        if (query.getResultPage() > 0) {
            httpRequest.addParameter("offset", query.getResultPage() * query.getResultCount());
        }
        // TODO sort
        GeoCoordinate coordinate = query.getCoordinate();
        if (coordinate != null) {
            String latLng = coordinate.getLatitude() + "," + coordinate.getLongitude();
            httpRequest.addParameter("ll", latLng);
            if (query.getRadius() != null) {
                int radius = Math.min((int)(query.getRadius() * 1000), 40000);
                httpRequest.addParameter("radius_filter", radius);
            }
        }
        if (query.getLanguage() != null) {
            httpRequest.addParameter("lang", query.getLanguage().getIso6391());
        }
        if (query.getFacet(CategoryFilter.YELP_CATEGORY_FACET) != null) {
            CategoryFilter categoryFilter = (CategoryFilter)query.getFacet(CategoryFilter.YELP_CATEGORY_FACET);
            httpRequest.addParameter("category_filter", categoryFilter.getCategories());
        }

        HttpRequest signedRequest = OAuthUtil.createSignedRequest(httpRequest, oAuthParams);
        LOGGER.debug("Request = {}", signedRequest);
        HttpResult result;
        try {
            result = HTTP_RETRIEVER.execute(signedRequest);
        } catch (HttpException e) {
            throw new SearcherException("HTTP exception for request: " + signedRequest, e);
        }

        try {
            JsonObject jsonResult = new JsonObject(result.getStringContent());
            LOGGER.trace("JSON result: {}", result.getStringContent());
            List<WebContent> results = CollectionHelper.newArrayList();
            JsonArray jsonBusinesses = jsonResult.getJsonArray("businesses");
            for (Object jsonEntry : jsonBusinesses) {
                JsonObject jsonObject = (JsonObject)jsonEntry;
                BasicWebContent.Builder builder = new BasicWebContent.Builder();
                builder.setIdentifier(jsonObject.getString("id"));
                builder.setTitle(jsonObject.getString("name"));
                builder.setSummary(jsonObject.tryGetString("snippet_text"));
                builder.setUrl(jsonObject.getString("url"));
                JsonArray jsonCategories = jsonObject.getJsonArray("categories");
                for (Object categoryEntry : jsonCategories) {
                    JsonArray jsonCategory = (JsonArray)categoryEntry;
                    builder.addTag(jsonCategory.getString(1));
                }
                results.add(builder.create());
            }
            return new SearchResults<WebContent>(results, jsonResult.getLong("total"));
        } catch (JsonException e) {
            throw new SearcherException(
                    "Encountered JSON exception while parsing '" + result.getStringContent() + "'.", e);
        }

    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

}
