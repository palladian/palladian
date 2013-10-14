package ws.palladian.retrieval.search.news;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
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
 * Search news on <a href="http://newsseecr.com">NewsSeecr</a>
 * </p>
 * 
 * @see <a href="https://www.mashape.com/qqilihq/newsseecr">API documentation on Mashape</a>
 * @see <a href="http://blog.mashape.com/important-changes-to-mashape-authorization-ke">Information on new
 *      authentication mechanism</a>
 * @author Philipp Katz
 */
public final class NewsSeecrSearcher extends AbstractMultifacetSearcher<WebContent> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NewsSeecrSearcher.class);

    private static final String SEARCHER_NAME = "NewsSeecr";

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    
    private static final String DATE_PARAMETER_FORMAT = "yyyy-MM-dd";

    private static final String BASE_URL = "https://qqilihq-newsseecr.p.mashape.com/news";

    private static final int RESULTS_PER_REQUEST = 100;

    private final String mashapeKey;

    private final HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

    /** Configuration key for the Mashape key. */
    public static final String CONFIG_MASHAPE_KEY = "api.newsseecr.mashapeKey";

    /**
     * <p>
     * Create a new {@link NewsSeecrSearcher} with the provided Mashape key.
     * </p>
     * 
     * @param mashapeKey The Mashape key, not empty or <code>null</code>.
     */
    public NewsSeecrSearcher(String mashapeKey) {
        Validate.notEmpty(mashapeKey, "mashapeKey must not be empty");
        this.mashapeKey = mashapeKey;
    }

    /**
     * <p>
     * Create a new {@link NewsSeecrSearcher} with the provided crendentials from Mashape supplied via a
     * {@link Configuration}.
     * </p>
     * 
     * @param configuration The configuration supplying the Mashape key as {@value #CONFIG_MASHAPE_KEY}. Not
     *            <code>null</code>.
     */
    public NewsSeecrSearcher(Configuration configuration) {
        String mashapeKey = configuration.getString(CONFIG_MASHAPE_KEY);
        this.mashapeKey = mashapeKey;
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {

        final int resultCount = query.getResultCount();
        List<WebContent> webResults = CollectionHelper.newArrayList();

        for (int offset = 0; offset < Math.ceil((double)resultCount / RESULTS_PER_REQUEST); offset++) {

            HttpRequest request = makeRequest(query, resultCount, offset);

            LOGGER.debug("Performing request: " + request);
            HttpResult result;
            try {
                result = retriever.execute(request);
            } catch (HttpException e) {
                throw new SearcherException("Encountered HTTP error when executing the request: " + request + ": "
                        + e.getMessage(), e);
            }
            if (result.getStatusCode() != 200) {
                // TODO get message
                throw new SearcherException("Encountered HTTP status " + result.getStatusCode()
                        + " when executing the request: " + request + ", result: " + result.getStringContent());
            }

            String jsonString = result.getStringContent();
            LOGGER.debug("JSON result: " + jsonString);

            try {
                JsonArray resultArray = new JsonObject(jsonString).queryJsonArray("/results");
                for (int i = 0; i < resultArray.size(); i++) {
                    JsonObject resultObject = resultArray.getJsonObject(i);
                    BasicWebContent.Builder builder = new BasicWebContent.Builder();
                    builder.setTitle(resultObject.queryString("/title"));
                    builder.setUrl(resultObject.queryString("/url"));
                    builder.setSummary(resultObject.queryString("/summary"));
                    Date date = parseDate(resultObject.queryString("/published"));
                    builder.setPublished(date);
                    webResults.add(builder.create());
                    if (webResults.size() == resultCount) {
                        break;
                    }
                }
            } catch (JsonException e) {
                throw new SearcherException("Error while parsing the JSON response (" + jsonString + "): "
                        + e.getMessage(), e);
            }
        }

        return new SearchResults<WebContent>(webResults);
    }

    private HttpRequest makeRequest(MultifacetQuery query, int resultCount, int offset) {
        HttpRequest request = new HttpRequest(HttpMethod.GET, BASE_URL);
        request.addParameter("page", offset);
        request.addParameter("numResults", Math.min(resultCount, RESULTS_PER_REQUEST));
        if (query.getText() != null) {
            request.addParameter("query", query.getText());
        }
        if (query.getStartDate() != null) {
            request.addParameter("startDate", formatParameterDate(query.getStartDate()));
        }
        if (query.getEndDate() != null) {
            request.addParameter("endDate", formatParameterDate(query.getEndDate()));
        }
        GeoCoordinate coordinate = query.getCoordinate();
        if (coordinate != null) {
            String coordinateParam = String.format("%s,%s", coordinate.getLatitude(), coordinate.getLongitude());
            request.addParameter("coordinates", coordinateParam);
        }
        if (query.getRadius() != null) {
            request.addParameter("distance", query.getRadius());
        }
        request.addHeader("X-Mashape-Authorization", mashapeKey);
        return request;
    }

    private String formatParameterDate(Date date) {
        return new SimpleDateFormat(DATE_PARAMETER_FORMAT).format(date);
    }

    public static Date parseDate(String dateString) {
        DateFormat dateParser = new SimpleDateFormat(DATE_FORMAT);
        try {
            return dateParser.parse(dateString);
        } catch (ParseException e) {
            LOGGER.warn("Error parsing date " + dateString);
            return null;
        }
    }

    public static void main(String[] args) throws SearcherException {
        // old, deprecated:
        // String publicKey = "u3ewnlzvxvbg3gochzqcrulimgngsb";
        // String privateKey = "dxkyimj8rjoyti1mqx2lqragbbg71k";
        // NewsSeecrSearcher searcher = new NewsSeecrSearcher(publicKey, privateKey);

        // new:
        String mashapeKey = "tr1dn3mc0bdhzzjngkvzahqloxph0e";
        NewsSeecrSearcher searcher = new NewsSeecrSearcher(mashapeKey);
        // List<WebContent> results = searcher.search("obama", 20);
        MultifacetQuery.Builder builder = new MultifacetQuery.Builder();
        builder.setCoordinate(38.89, -77.023611);
        builder.setRadius(25.);
        // builder.setText("afghanistan");
        // builder.setStartDate(DateParser.parseDate("2012-11-01").getNormalizedDate());
        // builder.setEndDate(DateParser.parseDate("2012-11-15").getNormalizedDate());
        SearchResults<WebContent> results = searcher.search(builder.create());
        CollectionHelper.print(results);
    }

}
