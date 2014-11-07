package ws.palladian.retrieval.search.web;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.FixedIntervalRequestThrottle;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Searcher for <a href="http://wikilocation.org">WikiLocation</a>, which allows searching for Wikipedia articles by
 * coordinates (further supported facets are text, language, limit, and radius).
 * </p>
 * 
 * @see <a href="http://wikilocation.org/documentation">API documentation</a>
 * @author pk
 */
public final class WikiLocationSearcher extends AbstractMultifacetSearcher<WebContent> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikiLocationSearcher.class);

    /** The name of this searcher. */
    private static final String NAME = "WikiLocation";

    /** Send max. 1 request/second, as stated in the API documentation of the service. */
    private static final RequestThrottle THROTTLE = new FixedIntervalRequestThrottle(1, TimeUnit.SECONDS);

    /** Retriever for HTTP requests. */
    private HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {
        String requestUrl = buildUrl(query);
        LOGGER.debug("Requesting '{}'", requestUrl);
        HttpResult httpResult;
        try {
            THROTTLE.hold();
            httpResult = httpRetriever.httpGet(requestUrl);
        } catch (HttpException e) {
            throw new SearcherException("Encountered error while requesting \"" + requestUrl + "\"", e);
        }
        try {
            JsonObject jsonObject = new JsonObject(httpResult.getStringContent());
            JsonArray articlesJson = jsonObject.queryJsonArray("/articles");
            List<WebContent> resultList = CollectionHelper.newArrayList();
            for (Object entry : articlesJson) {
                JsonObject articleJson = (JsonObject)entry;
                BasicWebContent.Builder builder = new BasicWebContent.Builder();
                builder.setIdentifier(articleJson.queryString("/id"));
                builder.setCoordinate(articleJson.queryDouble("/lat"), articleJson.queryDouble("/lng"));
                builder.setTitle(articleJson.queryString("/title"));
                builder.setUrl(articleJson.queryString("/url"));
                builder.setSource(NAME);
                resultList.add(builder.create());
            }
            return new SearchResults<WebContent>(resultList);
        } catch (JsonException e) {
            throw new SearcherException("Encountered error while parsing response using JSON, the result was \""
                    + httpResult.getStringContent() + "\".", e);
        }

    }

    private String buildUrl(MultifacetQuery query) throws SearcherException {
        if (query.getCoordinate() == null) {
            throw new SearcherException("The query must at least provide coordinates (MultifacetQuery#getCoordinate).");
        }
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://api.wikilocation.org/articles");
        urlBuilder.append("?lat=").append(query.getCoordinate().getLatitude());
        urlBuilder.append("&lng=").append(query.getCoordinate().getLongitude());
        if (query.getRadius() != null) {
            // radius is specified in meters
            urlBuilder.append("&radius=").append(query.getRadius() * 1000);
        }
        urlBuilder.append("&limit=").append(query.getResultCount());
        // TODO offset is supported
        // TODO type is supported
        if (query.getText() != null) {
            urlBuilder.append("&title=").append(UrlHelper.encodeParameter(query.getText()));
        }
        if (query.getLanguage() != null) {
            urlBuilder.append("&locale=").append(query.getLanguage().getIso6391());
        }
        urlBuilder.append("&format=json");
        return urlBuilder.toString();
    }

    public static void main(String[] args) throws SearcherException {
        MultifacetQuery.Builder builder = new MultifacetQuery.Builder();
        builder.setText("dresden");
        builder.setCoordinate(51.049259, 13.73836);
        builder.setRadius(5.);
        builder.setLanguage(Language.GERMAN);
        SearchResults<WebContent> results = new WikiLocationSearcher().search(builder.create());
        CollectionHelper.print(results.getResultList());
    }

}
