package ws.palladian.retrieval.search.images;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Predicates;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.configuration.StringConfigurationOption;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Search for images on <a href="http://www.flickr.com/">Flickr</a>.
 * </p>
 *
 * @author Philipp Katz
 * @see <a href="http://www.flickr.com/services/api/">Flickr Services</a>
 * @see <a href="http://www.flickr.com/services/api/flickr.photos.search.html">API: flickr.photos.search</a>
 * @see <a href="http://www.flickr.com/services/api/misc.api_keys.html">Obtaining an API key</a>
 */
public final class FlickrSearcher extends AbstractMultifacetSearcher<WebImage> {
    public static final class FlickrSearcherMetaInfo implements SearcherMetaInfo<FlickrSearcher, WebImage> {
        private static final StringConfigurationOption API_KEY_OPTION = new StringConfigurationOption("API Key", "apikey");

        @Override
        public String getSearcherName() {
            return SEARCHER_NAME;
        }

        @Override
        public String getSearcherId() {
            return "flickr";
        }

        @Override
        public Class<WebImage> getResultType() {
            return WebImage.class;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Arrays.asList(API_KEY_OPTION);
        }

        @Override
        public FlickrSearcher create(Map<ConfigurationOption<?>, ?> config) {
            var apiKey = API_KEY_OPTION.get(config);
            return new FlickrSearcher(apiKey);
        }

        @Override
        public String getSearcherDocumentationUrl() {
            return "https://www.flickr.com/services/api/";
        }

        @Override
        public String getSearcherDescription() {
            return "Search for images on <a href=\"https://www.flickr.com\">Flickr</a>.";
        }
    }

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FlickrSearcher.class);

    /**
     * The name of this searcher.
     */
    public static final String SEARCHER_NAME = "Flickr";

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * flickr allows 3.6000 requests/hour.
     */
    private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.HOURS, 3500);

    private final HttpRetriever retriever;

    /**
     * <p>
     * Search facet allowing to specify a/multiple {@link License}.
     * </p>
     *
     * @author Philipp Katz
     */
    public static final class Licenses implements Facet {
        private static final String LICENSES_IDENTIFIER = "flickr.licenses";

        private final Set<License> licenses;

        public Licenses(License... licenses) {
            this.licenses = new HashSet<>(Arrays.asList(licenses));
        }

        public Licenses(Collection<License> licenses) {
            this.licenses = new HashSet<>(licenses);
        }

        @Override
        public String getIdentifier() {
            return LICENSES_IDENTIFIER;
        }

        @Override
        public String getValue() {
            return getLicensesString();
        }

        String getLicensesString() {
            StringBuilder licensesString = new StringBuilder();
            boolean first = true;
            for (License license : licenses) {
                if (first) {
                    first = false;
                } else {
                    licensesString.append(",");
                }
                licensesString.append(license.id);
            }
            return licensesString.toString();
        }
    }

    // XXX this should be unified with ws.palladian.retrieval.search.License
    public enum License {
        /**
         * All Rights Reserved
         */
        ALL_RIGHTS_RESERVED(0),
        /**
         * Attribution-NonCommercial-ShareAlike License
         */
        ATTRIBUTION_NONCOMMERCIAL_SHAREALIKE(1),
        /**
         * Attribution-NonCommercial License
         */
        ATTRIBUTION_NONCOMMERCIAL(2),
        /**
         * Attribution-NonCommercial-NoDerivs License
         */
        ATTRIBUTION_NONCOMMERCIAL_NODERIVS(3),
        /**
         * Attribution License
         */
        ATTRIBUTION(4),
        /**
         * Attribution-ShareAlike License
         */
        ATTRIBUTION_SHAREALIKE(5),
        /**
         * Attribution-NoDerivs License
         */
        ATTRIBUTION_NODERIVS(6),
        /**
         * No known copyright restrictions
         */
        NO_KNOWN_COPYRIGHT_RESTRICTIONS(7),
        /**
         * United States Government Work
         */
        UNITED_STATES_GOVERNMENT_WORK(8);

        private int id;

        License(int id) {
            this.id = id;
        }

        static License get(int id) {
            for (License license : values()) {
                if (license.id == id) {
                    return license;
                }
            }
            return null;
        }
    }

    public enum OrderBy implements Facet {
        DATE_POSTED_ASC("date-posted-asc"), //
        DATE_POSTED_DESC("date-posted-desc"), //
        DATE_TAKEN_ASC("date-taken-asc"), //
        DATE_TAKEN_DESC("date-taken-desc"), //
        INTERESTINGNESS_DESC("interestingness-desc"), //
        INTERESTINGNESS_ASC("interestingness-asc"), //
        RELEVANCE("relevance"); //

        private static final String ORDER_BY_IDENTIFIER = "flickr.order";

        private final String orderByValue;

        OrderBy(String orderByValue) {
            this.orderByValue = orderByValue;
        }

        @Override
        public String getIdentifier() {
            return ORDER_BY_IDENTIFIER;
        }

        @Override
        public String getValue() {
            return null;
        }

    }

    /**
     * Identifier for the API key when supplied via {@link Configuration}.
     */
    public static final String CONFIG_API_KEY = "api.flickr.key";

    /**
     * The API key for accessing flickr.
     */
    private final String apiKey;

    /**
     * <p>
     * Creates a new Flickr searcher.
     * </p>
     *
     * @param apiKey The API key for accessing Flickr, not <code>null</code> or empty.
     */
    public FlickrSearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    public FlickrSearcher(String apiKey, int defaultResultCount) {
        this(apiKey);
        this.defaultResultCount = defaultResultCount;
    }

    /**
     * <p>
     * Creates a new Flickr searcher.
     * </p>
     *
     * @param configuration The configuration which must provide an API key for accessing Flickr, which must be provided
     *                      as string via key {@value FlickrSearcher#CONFIG_API_KEY} in the configuration.
     */
    public FlickrSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    public FlickrSearcher(Configuration config, int defaultResultCount) {
        this(config);
        this.defaultResultCount = defaultResultCount;
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    /*
     * (non-Javadoc)
     * @see com.newsseecr.searcher.MutifacetSearcher#search(com.newsseecr.searcher.MultifacetQuery)
     */
    @Override
    public SearchResults<WebImage> search(MultifacetQuery query) throws SearcherException {
        List<WebImage> result = new ArrayList<>();

        int resultCount = defaultResultCount == null ? query.getResultCount() : defaultResultCount;
        int resultsPerPage = Math.min(resultCount, 500); // max. 500 per page
        int neccessaryPages = (int) Math.ceil((double) resultCount / resultsPerPage);

        long availableResults = 0;

        for (int p = 0; p < neccessaryPages; p++) {
            String requestUrl = buildRequestUrl(query, resultsPerPage, p);
            LOGGER.debug("Requesting page {} with {}", p, requestUrl);
            HttpResult httpResult;
            THROTTLE.hold();
            try {
                httpResult = retriever.httpGet(requestUrl);
            } catch (HttpException e) {
                throw new SearcherException(
                        "HTTP error while searching for \"" + query + "\" with " + getName() + ": " + e.getMessage() + ", request URL was \"" + requestUrl + "\"", e);
            }
            if (httpResult.errorStatus()) {
                throw new SearcherException("Encountered HTTP error status: " + httpResult.getStatusCode() + " (" + httpResult.getStringContent() + ").");
            }
            String jsonString = httpResult.getStringContent();

            try {
                JsonObject resultJson = JsonObject.tryParse(jsonString);
                JsonObject photosJson = resultJson.getJsonObject("photos");
                if (photosJson != null) { // result list (search)
                    if (photosJson.get("total") != null) {
                        availableResults = photosJson.getLong("total");
                    }
                    JsonArray photoJsonArray = photosJson.getJsonArray("photo");
                    for (int i = 0; i < photoJsonArray.size(); i++) {
                        JsonObject photoJson = photoJsonArray.getJsonObject(i);
                        result.add(parseWebImage(photoJson));
                    }
                } else { // single result (retrieve picture per ID)
                    JsonObject photoJson = resultJson.getJsonObject("photo");
                    result.add(parseWebImage(photoJson));
                }
            } catch (JsonException e) {
                throw new SearcherException("Parse error while searching for \"" + query + "\" with " + getName() + ": " + e.getMessage() + ", JSON was \"" + jsonString + "\"", e);
            }
        }
        return new SearchResults<WebImage>(result, availableResults);
    }

    private WebImage parseWebImage(JsonObject photoJson) throws JsonException {
        String farmId = photoJson.getString("farm");
        String serverId = photoJson.getString("server");
        String id = photoJson.getString("id");

        String secret = photoJson.getString("secret");
        String userId = photoJson.tryGetString("owner");
        if (userId == null) {
            userId = photoJson.queryString("/owner/nsid");
        }

        BasicWebImage.Builder builder = new BasicWebImage.Builder();
        String title = photoJson.tryGetString("title");
        if (title == null) {
            title = photoJson.queryString("/title/_content");
        }
        builder.setTitle(title);
        builder.setImageUrl(buildImageUrl(farmId, serverId, id, secret));
        builder.setUrl(buildPageUrl(id, userId));
        builder.setSummary(photoJson.getJsonObject("description").getString("_content"));
        builder.setPublished(parseDate(photoJson));
        builder.setCoordinate(parseCoordinate(photoJson));
        builder.setIdentifier(id);

        // License license = License.get(photoJson.getInt("license"));
        Integer width = photoJson.tryGetInt("o_width");
        Integer height = photoJson.tryGetInt("o_height");
        if (width != null && height != null) {
            builder.setWidth(width);
            builder.setHeight(height);
        }
        builder.setTags(parseTags(photoJson));
        builder.setSource(SEARCHER_NAME);
        return builder.create();
    }

    private static Set<String> parseTags(JsonObject photoJson) throws JsonException {
        Object tagsElement = photoJson.get("tags");
        Set<String> tags;
        if (tagsElement instanceof String) {
            tags = new HashSet<>(Arrays.asList(((String) tagsElement).split("\\s")));
        } else {
            JsonObject tagsObject = (JsonObject) tagsElement;
            JsonArray tagsArray = tagsObject.getJsonArray("tag");
            tags = new HashSet<>();
            for (Object tagContent : tagsArray) {
                tags.add(((JsonObject) tagContent).getString("_content"));
            }
        }
        // remove "vision:" tags; http://stackoverflow.com/questions/21287302/flickr-api-what-are-the-vision-tags
        CollectionHelper.remove(tags, Predicates.not(Predicates.regex("vision:.*")));
        return tags;
    }

    /**
     * Parse coordinate from JSON. It is either directly in <code>latitude</code>/<code>longitude</code>, or nested
     * within <code>location</code> (in case of a single result, when querying for an ID).
     *
     * @param photoJson The JSON of the photo.
     * @return The parsed {@link GeoCoordinate}, or <code>null</code>.
     */
    private static GeoCoordinate parseCoordinate(JsonObject photoJson) {
        Double lat = photoJson.tryGetDouble("latitude");
        Double lng = photoJson.tryGetDouble("longitude");
        if (lat == null || lng == null) {
            lat = photoJson.tryQueryDouble("/location/latitude");
            lng = photoJson.tryQueryDouble("/location/longitude");
        }
        if (lat == null || lng == null || (lat == 0.0 && lng == 0.0)) {
            return null;
        }
        return GeoCoordinate.from(lat, lng);
    }

    /**
     * Parse date from JSON. It is either directly in <code>datetaken</code>, or in <code>dates/taken</code> (in case of
     * a single result, when querying for an ID).
     *
     * @param photoJson The JSON of the photo.
     * @return The parsed date, or <code>null</code>.
     */
    private Date parseDate(JsonObject photoJson) {
        String dateString = photoJson.tryGetString("datetaken");
        if (dateString == null) {
            dateString = photoJson.tryQueryString("/dates/taken");
        }
        if (dateString == null) {
            return null;
        }
        try {
            return new SimpleDateFormat(DATE_PATTERN).parse(dateString);
        } catch (ParseException e) {
            LOGGER.warn("Error while parsing date string '" + dateString + "'.");
            return null;
        }
    }

    /**
     * @param query   The {@link MultifacetQuery} to process.
     * @param perPage Number of results to return per page.
     * @param page    The page to return.
     * @return The query URL.
     */
    private String buildRequestUrl(MultifacetQuery query, int perPage, int page) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://api.flickr.com/services/rest/");
        urlBuilder.append("?api_key=").append(apiKey);
        if (StringUtils.isNotBlank(query.getId())) {
            urlBuilder.append("&method=flickr.photos.getInfo");
            urlBuilder.append("&photo_id=").append(query.getId());
        } else {
            urlBuilder.append("&method=flickr.photos.search");
            if (query.getText() != null) {
                urlBuilder.append("&text=").append(UrlHelper.encodeParameter(query.getText()));
            }
            if (query.getTags() != null && !query.getTags().isEmpty()) {
                urlBuilder.append("&tags=").append(StringUtils.join(query.getTags(), ","));
            }
            if (query.getStartDate() != null) {
                urlBuilder.append("&min_taken_date=").append(query.getStartDate().getTime() / 1000);
            }
            if (query.getEndDate() != null) {
                urlBuilder.append("&max_taken_date=").append(query.getEndDate().getTime() / 1000);
            }
            if (query.getCoordinate() != null) {
                double radius = query.getRadius() != null ? query.getRadius() : 10;
                double[] bbox = query.getCoordinate().getBoundingBox(radius);
                String params = String.format("%s,%s,%s,%s", bbox[1], bbox[0], bbox[3], bbox[2]);
                urlBuilder.append("&bbox=").append(params);
            }
            Facet facet = query.getFacet(Licenses.LICENSES_IDENTIFIER);
            if (facet != null) {
                Licenses licensesFacet = (Licenses) facet;
                if (licensesFacet.licenses.size() > 0) {
                    urlBuilder.append("&license=").append(licensesFacet.getLicensesString());
                }
            }
            facet = query.getFacet(OrderBy.ORDER_BY_IDENTIFIER);
            if (facet != null) {
                OrderBy orderByFacet = (OrderBy) facet;
                urlBuilder.append("&sort=").append(orderByFacet.orderByValue);
            }
            urlBuilder.append("&per_page=").append(perPage);
            urlBuilder.append("&page=").append(page);
            urlBuilder.append("&extras=description,license,date_taken,geo,tags,o_dims,");
        }
        urlBuilder.append("&format=json");
        urlBuilder.append("&nojsoncallback=1");
        LOGGER.debug("Query URL {}", urlBuilder);
        return urlBuilder.toString();
    }

    /**
     * <p>
     * Transforms the given parts back to an image URL.
     * </p>
     *
     * @param farmId   The farm ID, not <code>null</code> or empty.
     * @param serverId The server ID, not <code>null</code> or empty.
     * @param id       The image ID, not <code>null</code> or empty.
     * @param secret   The secret, not <code>null</code> or empty.
     * @return A URL pointing to the image.
     * @see <a href="http://www.flickr.com/services/api/misc.urls.html">URLs</a>
     */
    private String buildImageUrl(String farmId, String serverId, String id, String secret) {
        return String.format("http://farm%s.staticflickr.com/%s/%s_%s.jpg", farmId, serverId, id, secret);
    }

    /**
     * <p>
     * Transforms the given parts to a page URL which gives details about the image.
     * </p>
     *
     * @param id     The image ID, not <code>null</code> or empty.
     * @param userId The user ID, not <code>null</code> or empty.
     * @return A URL pointing to the page with the image.
     */
    private String buildPageUrl(String id, String userId) {
        return String.format("http://www.flickr.com/photos/%s/%s", userId, id);
    }

}
