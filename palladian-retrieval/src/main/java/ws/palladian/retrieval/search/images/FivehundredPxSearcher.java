package ws.palladian.retrieval.search.images;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.Facet;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Searcher for <a href="http://500px.com">500px</a>.
 * </p>
 * 
 * @see <a href="https://github.com/500px/api-documentation/blob/master/endpoints/photo/GET_photos_search.md">API: Photo
 *      Resources</a>
 * @see <a href="http://developers.500px.com">500px / Developer</a>
 * @author pk
 */
public final class FivehundredPxSearcher extends AbstractMultifacetSearcher<WebImage> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FivehundredPxSearcher.class);

    /** The name of this searcher. */
    private static final String NAME = "500px";

    /** Maximum number of results which can be retrieved by one request. */
    private static final int MAX_RESULTS_PER_PAGE = 100;

    /** The format for parsing the dates. */
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    /** Key for the configuration with the comsumer key. */
    public static final String CONFIG_CONSUMER_KEY = "api.500px.consumerKey";

    /**
     * <p>
     * Order parameter for result list.
     * </p>
     * 
     * @author pk
     */
    public static enum OrderBy implements Facet {
        /** Default: sort by time of upload, most recent first */
        CREATED_AT,
        /** Sort by current rating, highest rated first */
        RATING,
        /** Sort by the number of views, most viewed first */
        TIMES_VIEWED,
        /** Sort by the number of votes, most voted on first */
        VOTES_COUNT,
        /** Sort by the number of favorites, most favorited first */
        FAVORITES_COUNT,
        /** Sort by the number of comments, most commented first */
        COMMENTS_COUNT,
        /**
         * Sort by the original date of the image extracted from metadata, most recent first (might not be available for
         * all images)
         */
        TAKEN_AT;

        private static final String FIVEHUNDREDPX_RESULT_ORDER = "youtube.resultOrder";

        @Override
        public String getIdentifier() {
            return FIVEHUNDREDPX_RESULT_ORDER;
        }

        public String getValue() {
            return toString().toLowerCase();
        }

    }

    /** The key for authentication. */
    private final String consumerKey;

    /**
     * @param consumerKey The consumer key for authentication, not <code>null</code> or empty.
     */
    public FivehundredPxSearcher(String consumerKey) {
        Validate.notEmpty(consumerKey, "consumerKey must not be empty");
        this.consumerKey = consumerKey;
    }

    /**
     * <p>
     * Create a new {@link FivehundredPxSearcher} from a given {@link Configuration}.
     * </p>
     * 
     * @param configuration The configuration which must provide the consumer key via {@value #CONFIG_CONSUMER_KEY}, not
     *            <code>null</code>.
     */
    public FivehundredPxSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_CONSUMER_KEY));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public SearchResults<WebImage> search(MultifacetQuery query) throws SearcherException {
        int numPages = (int)Math.ceil((double)query.getResultCount() / MAX_RESULTS_PER_PAGE);
        LOGGER.debug("# necessary request for {} : {}", query.getResultCount(), numPages);
        Long totalItems = null;
        List<WebImage> images = CollectionHelper.newArrayList();
        for (int page = 1; page <= numPages; page++) {
            String requestUrl = createRequestUrl(query, page, MAX_RESULTS_PER_PAGE);
            LOGGER.debug("Request URL = {}", requestUrl);
            HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
            HttpResult httpResult;
            try {
                httpResult = httpRetriever.httpGet(requestUrl);
            } catch (HttpException e) {
                throw new SearcherException("Encountered HTTP error while trying to access \"" + requestUrl + "\".", e);
            }
            if (httpResult.errorStatus()) {
                throw new SearcherException("Got HTTP response code " + httpResult.getStatusCode()
                        + " while accessing \"" + requestUrl + "\".");
            }
            try {
                JsonObject jsonResult = new JsonObject(httpResult.getStringContent());
                totalItems = jsonResult.getLong("total_items");
                JsonArray jsonPhotos = jsonResult.getJsonArray("photos");
                for (Object photoObject : jsonPhotos) {
                    BasicWebImage.Builder builder = new BasicWebImage.Builder();
                    JsonObject photoJson = (JsonObject)photoObject;
                    // additionally available information from result:
                    // times_viewed, rating, category, privacy, votes_count, favorites_count, comments_count, nsfw
                    builder.setIdentifier(photoJson.getString("id"));
                    builder.setTitle(photoJson.getString("name"));
                    builder.setSummary(photoJson.getString("description"));
                    builder.setPublished(parseDate(photoJson.getString("created_at")));
                    builder.setSize(photoJson.getInt("width"), photoJson.getInt("height"));
                    builder.setImageUrl(photoJson.getString("image_url"));
                    builder.setUrl("http://500px.com/photo/" + photoJson.getString("id"));
                    builder.setSource(NAME);
                    JsonArray tagsArray = photoJson.getJsonArray("tags");
                    for (Object tag : tagsArray) {
                        builder.addTag((String)tag);
                    }
                    images.add(builder.create());
                    if (images.size() >= query.getResultCount()) {
                        break;
                    }
                }
            } catch (JsonException e) {
                throw new SearcherException("Error while trying to parse response as JSON (\""
                        + httpResult.getStringContent() + "\").", e);
            }
        }
        return new SearchResults<WebImage>(images, totalItems);
    }

    private static Date parseDate(String dateString) {
        try {
            // There is no parse format for time zone information as given in "2012-02-10T00:39:03-05:00";
            // split it up, remove colon from zone part and use 'Z' placeholder from SimpleDateFormat.
            String datePart = dateString.substring(0, 19);
            String timeZonePart = dateString.substring(19, 25).replace(":", "");
            return new SimpleDateFormat(DATE_FORMAT).parse(datePart + timeZonePart);
        } catch (Exception e) {
            LOGGER.warn("Could not parse {} using {}", dateString, DATE_FORMAT);
            return null;
        }
    }

    /**
     * @param query The {@link MultifacetQuery}, not <code>null</code>.
     * @param page The page to show, 1-based.
     * @param resultsPerPage The number of results per page, maximum 100.
     * @return The request URL.
     * @throws SearcherException In case necessary data is missing for the query (either term, tag, or geo must be
     *             given).
     */
    private String createRequestUrl(MultifacetQuery query, int page, int resultsPerPage) throws SearcherException {
        Validate.notNull(query, "query must not be null");
        Validate.isTrue(page >= 1, "page must be greater/equal one");
        Validate.isTrue(resultsPerPage <= 100, "resultsPerPage must be 100 maximum");
        StringBuilder requestUrl = new StringBuilder();
        requestUrl.append("https://api.500px.com/v1/photos/search");
        requestUrl.append("?consumer_key=").append(consumerKey);
        boolean validQuery = false;
        if (StringUtils.isNotBlank(query.getText())) {
            requestUrl.append("&term=").append(UrlHelper.encodeParameter(query.getText()));
            validQuery = true;
        }
        if (query.getTags().size() > 0) {
            requestUrl.append("&tag=").append(CollectionHelper.getFirst(query.getTags()));
            validQuery = true;
        }
        GeoCoordinate coordinate = query.getCoordinate();
        if (coordinate != null) {
            double radius = query.getRadius() != null ? query.getRadius() : 10;
            double lat = coordinate.getLatitude();
            double lng = coordinate.getLongitude();
            requestUrl.append("&geo=").append(String.format("%s,%s,%skm", lat, lng, radius));
            validQuery = true;
        }
        if (!validQuery) {
            throw new SearcherException(
                    "Necessary information for performing the query is missing, either text, tag or coordinate must be given.");
        }
        requestUrl.append("&page=").append(page);
        requestUrl.append("&rpp=").append(resultsPerPage);
        requestUrl.append("&tags=true");
        if (query.getFacet(OrderBy.FIVEHUNDREDPX_RESULT_ORDER) != null) {
            OrderBy orderByFacet = (OrderBy)query.getFacet(OrderBy.FIVEHUNDREDPX_RESULT_ORDER);
            requestUrl.append("&sort=").append(orderByFacet.getValue());
        }
        return requestUrl.toString();
    }

}
