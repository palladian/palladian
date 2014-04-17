package ws.palladian.retrieval.search.socialmedia;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.Facet;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.RateLimitedException;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Search for images on <a href="http://instagram.com">Instagram</a> by tags. This searcher has one peculiarity: As tags
 * are one word terms, only the first term in the query is considered, the rest is silently ignored. Authentication
 * needs to be done via OAuth 2.0 in advance, the accessToken is necessary for initializing this searcher. The necessary
 * steps are not implemented here, as they require user interaction.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://instagram.com/developer/">Instagram Developer Documentation</a>
 * @see <a href="http://instagram.com/developer/authentication/">Authentication</a>
 */
public final class InstagramSearcher extends AbstractMultifacetSearcher<WebImage> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(InstagramSearcher.class);

    /**
     * Facet which enables the "deep coordinate retrieval" mode. This is only relevant when using coordinates as query
     * facet. When enabled, not only the coordinate endpoint of Instagram is queried (<code>/media/search</code>), but
     * we also query the locations endpoint (<code>/locations/search</code>) with the coordinate and then search for
     * images in all locations. This gives in general more results, but also results in considerably more API requests
     * for each query.
     * 
     * @author pk
     */
    public static enum DeepCoordinateRetrieval implements Facet {
        DEEP;

        private static final String INSTAGRAM_DEEP_COORDINATE_RETRIEVAL_ID = "instagram.deepCoordinateRetrieval";

        @Override
        public String getIdentifier() {
            return INSTAGRAM_DEEP_COORDINATE_RETRIEVAL_ID;
        }
    }

    /**
     * Facet, which allows searching Instagram by a Instagram-specific location ID.
     * 
     * @see <a href="http://instagram.com/developer/endpoints/locations/">Location Endpoints</a>
     * @author pk
     */
    public static final class LocationId implements Facet {
        private static final String INSTAGRAM_LOCATION_ID = "instagram.locationId";

        private final int locationId;

        public LocationId(int locationId) {
            this.locationId = locationId;
        }

        public int getLocationId() {
            return locationId;
        }

        @Override
        public String getIdentifier() {
            return INSTAGRAM_LOCATION_ID;
        }
    }

    /** The identifier for the {@link Configuration} key with the access token. */
    public static final String CONFIG_ACCESS_TOKEN = "api.instagram.accessToken";

    /** Name of this searcher. */
    public static final String SEARCHER_NAME = "Instagram";

    /** Instagram allows maximum 5000 requests/hour; however, we take a smaller value to be on the save side. */
    private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.HOURS, 4500);

    private final String accessToken;

    private final HttpRetriever retriever;

    /**
     * <p>
     * Initialize a new {@link InstagramTagSearcher} with the specified OAuth access token.
     * </p>
     * 
     * @param accessToken The OAuth access token, not <code>null</code> or empty.
     */
    public InstagramSearcher(String accessToken) {
        Validate.notEmpty(accessToken, "accessToken must not be empty");
        this.accessToken = accessToken;
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    /**
     * <p>
     * Initialize a new {@link InstagramTagSearcher} with the an OAuth access token provided via the
     * {@link Configuration} instance.
     * </p>
     * 
     * @param configuration The configuration providing an OAuth access token with the identifier
     *            {@value #CONFIG_ACCESS_TOKEN}, not <code>null</code>.
     */
    public InstagramSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_ACCESS_TOKEN));
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    private List<WebImage> fetchResult(int resultCount, String queryUrl) throws SearcherException {
        List<WebImage> result = CollectionHelper.newArrayList();
        page: for (;;) {
            HttpResult httpResult = performGet(queryUrl);
            String jsonString = httpResult.getStringContent();
            LOGGER.trace("JSON = {}", jsonString);
            try {
                JsonObject jsonResult = new JsonObject(jsonString);
                if (jsonResult.get("data") instanceof JsonArray) {
                    // result list
                    JsonArray dataArray = jsonResult.getJsonArray("data");
                    for (int i = 0; i < dataArray.size(); i++) {
                        JsonObject data = dataArray.getJsonObject(i);
                        WebImage webImage = parseJsonObject(data);
                        result.add(webImage);
                        if (result.size() == resultCount) {
                            break page;
                        }
                    }
                } else {
                    // single result
                    result.add(parseJsonObject(jsonResult.getJsonObject("data")));
                }
                if (jsonResult.get("pagination") == null) {
                    break;
                }
                JsonObject paginationJson = jsonResult.getJsonObject("pagination");
                if (paginationJson.get("next_url") == null) {
                    break;
                }
                queryUrl = paginationJson.getString("next_url");
            } catch (JsonException e) {
                throw new SearcherException("Parse exception while parsing JSON data: \"" + jsonString + "\", URL: \""
                        + queryUrl + "\"", e);
            }
        }
        return result;
    }

    /**
     * Perform a HTTP get, check rate limits (proactive using the {@link RequestThrottle}, and after request by checking
     * the status code.
     * 
     * @param queryUrl The URL to GET.
     * @return The {@link HttpResult} for the GET operation.
     * @throws RateLimitedException In case the service gave a rate limited error.
     * @throws SearcherException In other cases of HTTP errors.
     */
    private HttpResult performGet(String queryUrl) throws RateLimitedException, SearcherException {
        LOGGER.debug("GET {}", queryUrl);
        THROTTLE.hold();
        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(queryUrl);
            if (httpResult.getStatusCode() == 503) {
                throw new RateLimitedException("Rate limit exceeded.", null);
            }
            if (httpResult.errorStatus()) {
                throw new SearcherException("Received HTTP code " + httpResult.getStatusCode() + " while accessing \""
                        + queryUrl + "\" (" + getErrorMessage(httpResult.getStringContent()) + ")");
            }
        } catch (HttpException e) {
            throw new SearcherException("Encountered HTTP exception while accessing \"" + queryUrl + "\": "
                    + e.getMessage(), e);
        }
        return httpResult;
    }

    /**
     * Parse a JSON entry into a WebImage.
     * 
     * @param data The JSON data.
     * @return A parsed WebImage.
     * @throws JsonException In case parsing failes.
     */
    private static WebImage parseJsonObject(JsonObject data) throws JsonException {
        BasicWebImage.Builder builder = new BasicWebImage.Builder();
        builder.setUrl(data.getString("link"));
        builder.setPublished(new Date(data.getLong("created_time") * 1000));
        JsonObject imageData = data.getJsonObject("images").getJsonObject("standard_resolution");
        builder.setImageUrl(imageData.getString("url"));
        builder.setWidth(imageData.getInt("width"));
        builder.setHeight(imageData.getInt("height"));
        if (data.get("caption") != null) {
            builder.setTitle(data.getJsonObject("caption").getString("text"));
        }
        if (data.get("location") != null) {
            JsonObject jsonLocation = data.getJsonObject("location");
            if (jsonLocation.get("longitude") != null && jsonLocation.get("latitude") != null) {
                double longitude = jsonLocation.getDouble("longitude");
                double latitude = jsonLocation.getDouble("latitude");
                builder.setCoordinate(latitude, longitude);
            }
        }
        if (data.get("tags") != null) {
            JsonArray tagArray = data.getJsonArray("tags");
            Set<String> tagSet = CollectionHelper.newHashSet();
            for (int j = 0; j < tagArray.size(); j++) {
                tagSet.add(tagArray.getString(j));
            }
            builder.setTags(tagSet);
        }
        builder.setIdentifier(data.getString("id"));
        builder.setSource(SEARCHER_NAME);
        return builder.create();
    }

    /**
     * Try to extract error message from JSON.
     * 
     * @param stringContent The result string.
     * @return A formatted error message, or an empty String, in case the response could not be parsed.
     */
    private static String getErrorMessage(String stringContent) {
        try {
            if (StringUtils.isNotBlank(stringContent)) {
                JsonObject json = new JsonObject(stringContent);
                String errorType = json.queryString("/meta/error_type");
                String errorMessage = json.queryString("/meta/error_message");
                return String.format("%s: %s", errorType, errorMessage);
            }
        } catch (JsonException e) {
            // ignore
        }
        return StringUtils.EMPTY;
    }

    @Override
    public SearchResults<WebImage> search(MultifacetQuery query) throws SearcherException {
        String id = query.getId();
        String tag = getTag(query);
        List<WebImage> results;
        Facet facet = query.getFacet(LocationId.INSTAGRAM_LOCATION_ID);
        LocationId locationIdFacet = facet != null ? (LocationId)facet : null;

        // we search by URL; but first, we need to get the ID ...
        if (query.getUrl() != null) {
            id = getIdForUrl(query.getUrl());
        }

        if (id != null) {
            String queryUrl = String.format("https://api.instagram.com/v1/media/%s?access_token=%s", id, accessToken);
            results = fetchResult(query.getResultCount(), queryUrl);
        } else if (tag != null) {
            String queryUrl = String.format("https://api.instagram.com/v1/tags/%s/media/recent?access_token=%s",
                    UrlHelper.encodeParameter(tag), accessToken);
            results = fetchResult(query.getResultCount(), queryUrl);
        } else if (locationIdFacet != null) {
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("https://api.instagram.com/v1/locations/");
            urlBuilder.append(locationIdFacet.getLocationId());
            urlBuilder.append("/media/recent");
            urlBuilder.append("?access_token=").append(accessToken);
            if (query.getStartDate() != null) {
                urlBuilder.append("&min_timestamp=").append(unixTimestamp(query.getStartDate()));
            }
            if (query.getEndDate() != null) {
                urlBuilder.append("&max_timestamp=").append(unixTimestamp(query.getEndDate()));
            }
            results = fetchResult(query.getResultCount(), urlBuilder.toString());
        } else if (query.getCoordinate() != null) {
            final GeoCoordinate coordinate = query.getCoordinate();
            // 5000 meters is maximum radius
            int radius = query.getRadius() != null ? (int)Math.min(query.getRadius() * 1000, 5000) : 5000;
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("https://api.instagram.com/v1/media/search");
            urlBuilder.append("?lat=").append(coordinate.getLatitude());
            urlBuilder.append("&lng=").append(coordinate.getLongitude());
            if (query.getStartDate() != null) {
                urlBuilder.append("&min_timestamp=").append(unixTimestamp(query.getStartDate()));
            }
            if (query.getEndDate() != null) {
                urlBuilder.append("&max_timestamp=").append(unixTimestamp(query.getEndDate()));
            }
            urlBuilder.append("&distance=").append(radius);
            urlBuilder.append("&access_token=").append(accessToken);
            results = fetchResult(query.getResultCount(), urlBuilder.toString());
            // "deep coordinate retrieval" mode; in addition to the explicit coordinate search, we look for locations
            // matching the given coordinate radius, and use those locations for finding images; experiments show, that
            // this way we can retrieve much more content. On the other hand, we have to perform considerably more API
            // request this way.
            Facet deepFacet = query.getFacet(DeepCoordinateRetrieval.INSTAGRAM_DEEP_COORDINATE_RETRIEVAL_ID);
            if (deepFacet != null) {
                List<LocationId> locationIds = getIdsForCoordinate(coordinate, radius);
                results = CollectionHelper.newArrayList();
                for (LocationId locationId : locationIds) {
                    MultifacetQuery.Builder locationIdQueryBuilder = new MultifacetQuery.Builder();
                    locationIdQueryBuilder.addFacet(locationId);
                    locationIdQueryBuilder.setStartDate(query.getStartDate());
                    locationIdQueryBuilder.setEndDate(query.getEndDate());
                    locationIdQueryBuilder.setResultCount(query.getResultCount());
                    List<WebImage> currentResults = search(locationIdQueryBuilder.create()).getResultList();
                    LOGGER.debug("Found {} results for location with ID {}", currentResults.size(),
                            locationId.getLocationId());
                    results.addAll(currentResults);
                }
                // limit to the requested result count; order by proximity to coordinate
                if (results.size() > query.getResultCount()) {
                    Collections.sort(results, new Comparator<WebContent>() {
                        @Override
                        public int compare(WebContent content1, WebContent content2) {
                            GeoCoordinate c1 = content1.getCoordinate();
                            GeoCoordinate c2 = content2.getCoordinate();
                            if (c1 == null) {
                                return c2 == null ? 0 : 1;
                            }
                            if (c2 == null) {
                                return -1;
                            }
                            return Double.compare(coordinate.distance(c1), coordinate.distance(c2));
                        }
                    });
                    results = results.subList(0, query.getResultCount());
                }
            }
        } else {
            throw new SearcherException(
                    "Search must either provide a tag, a geographic coordinate and a radius, an Instagram location ID, a URL or an Instagram image ID.");
        }
        return new SearchResults<WebImage>(results);
    }

    /**
     * Transforms a {@link Date} to a UNIX timestamp.
     * 
     * @param date The date.
     * @return A UNIX timestamp.
     */
    private static long unixTimestamp(Date date) {
        return date.getTime() / 1000;
    }

    /**
     * In case, the {@link MultifacetQuery} asks for a URL, we first have to transform the URL to an Instagram ID. This
     * costs one additional HTTP request, but it does not require API key authorization, so I guess it does not fall
     * under the quota (and therefore do not use the {@link RequestThrottle} here).
     * 
     * @param url The URL to transform.
     * @return An Instagram ID.
     * @throws SearcherException In case, the transformation fails.
     * @see <a
     *      href="http://stackoverflow.com/questions/16758316/where-do-i-find-the-instagram-media-id-of-a-image">Where
     *      do I find the Instagram media ID of a image</a>
     */
    private String getIdForUrl(String url) throws SearcherException {
        try {
            HttpResult httpResult = retriever.httpGet("http://api.instagram.com/oembed?url=" + url);
            String id = new JsonObject(httpResult.getStringContent()).queryString("/media_id");
            LOGGER.debug("ID for {} = {}", url, id);
            return id;
        } catch (HttpException e) {
            throw new SearcherException("Error while trying to get ID for URL.");
        } catch (JsonException e) {
            throw new SearcherException("Error while trying to get ID for URL \"" + url
                    + "\". Make sure, that this is a valid Instagram URL!");
        }
    }

    /**
     * this is mainly for legacy reasons; try to get tags from explicit tag set (preferred) or extract from given text
     * (deprecated)
     */
    private static String getTag(MultifacetQuery query) {
        Collection<String> allTags = CollectionHelper.newArrayList();
        if (query.getTags().size() > 0) {
            allTags = query.getTags();
        } else if (query.getText() != null) {
            allTags = Arrays.asList(query.getText().split("\\s"));
        }
        String firstTag = CollectionHelper.getFirst(allTags);
        if (firstTag == null) {
            return null;
        }
        firstTag = firstTag.replaceAll("[^A-Za-z0-9]", ""); // remove special characters
        if (allTags.size() > 1) {
            LOGGER.warn("Query consists of multiple terms ({}), only the first one ({}) is considered!", allTags,
                    firstTag);
        }
        return firstTag;
    }

    /**
     * When searching by {@link GeoCoordinate}s, we can first retrieve all available locations for that coordinate
     * (seems, that we can get 20 locations maximum), and then use the obtained location IDs to retrieve content. This
     * seems to yield in more results than the <code>/media/search</code> endpoint.
     * 
     * @param coordinate The coordinate.
     * @param distance The distance.
     * @return A list of location IDs for the given coordinate+distance, or an empty list.
     * @throws SearcherException In case, the query fails.
     */
    private List<LocationId> getIdsForCoordinate(GeoCoordinate coordinate, int distance) throws SearcherException {
        Validate.notNull(coordinate, "coordinate must not be null");
        Validate.isTrue(distance >= 0, "distance must be greater/equal zero");
        List<LocationId> locationIds = CollectionHelper.newArrayList();
        String coordinateToLocationsQueryUrl = String.format(
                "https://api.instagram.com/v1/locations/search?lat=%s&lng=%s&distance=%s&access_token=%s",
                coordinate.getLatitude(), coordinate.getLongitude(), distance, accessToken);
        HttpResult httpResult = performGet(coordinateToLocationsQueryUrl);
        try {
            JsonArray jsonData = new JsonObject(httpResult.getStringContent()).getJsonArray("data");
            for (int i = 0; i < jsonData.size(); i++) {
                JsonObject currentData = (JsonObject)jsonData.get(i);
                locationIds.add(new LocationId(currentData.getInt("id")));
            }
        } catch (JsonException e) {
            throw new SearcherException("Exception while parsing JSON (" + httpResult.getStringContent() + ").");
        }
        return locationIds;
    }

}
