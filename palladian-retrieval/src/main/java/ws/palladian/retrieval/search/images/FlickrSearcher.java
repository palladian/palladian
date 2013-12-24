package ws.palladian.retrieval.search.images;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.ImmutableGeoCoordinate;
import ws.palladian.helper.UrlHelper;
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
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.Facet;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

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

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FlickrSearcher.class);

    /** The name of this searcher. */
    private static final String SEARCHER_NAME = "Flickr";

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    
    /** flickr allows 3.6000 requests/hour. */
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
            this.licenses = new HashSet<License>(Arrays.asList(licenses));
        }

        public Licenses(Collection<License> licenses) {
            this.licenses = new HashSet<License>(licenses);
        }

        @Override
        public String getIdentifier() {
            return LICENSES_IDENTIFIER;
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
    public static enum License {
        /** All Rights Reserved */
        ALL_RIGHTS_RESERVED(0),
        /** Attribution-NonCommercial-ShareAlike License */
        ATTRIBUTION_NONCOMMERCIAL_SHAREALIKE(1),
        /** Attribution-NonCommercial License */
        ATTRIBUTION_NONCOMMERCIAL(2),
        /** Attribution-NonCommercial-NoDerivs License */
        ATTRIBUTION_NONCOMMERCIAL_NODERIVS(3),
        /** Attribution License */
        ATTRIBUTION(4),
        /** Attribution-ShareAlike License */
        ATTRIBUTION_SHAREALIKE(5),
        /** Attribution-NoDerivs License */
        ATTRIBUTION_NODERIVS(6),
        /** No known copyright restrictions */
        NO_KNOWN_COPYRIGHT_RESTRICTIONS(7),
        /** United States Government Work */
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

    /** Identifier for the API key when supplied via {@link Configuration}. */
    public static final String CONFIG_API_KEY = "api.flickr.key";

    /** The API key for accessing flickr. */
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

    /**
     * <p>
     * Creates a new Flickr searcher.
     * </p>
     * 
     * @param configuration The configuration which must provide an API key for accessing Flickr, which must be provided
     *            as string via key {@value FlickrSearcher#CONFIG_API_KEY} in the configuration.
     */
    public FlickrSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
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
        List<WebImage> result = new ArrayList<WebImage>();

        int resultCount = query.getResultCount();
        int resultsPerPage = Math.min(resultCount, 500); // max. 500 per page
        int neccessaryPages = (int)Math.ceil((double)resultCount / resultsPerPage);

        long availableResults = 0;

        for (int p = 0; p < neccessaryPages; p++) {
            String requestUrl = buildRequestUrl(query, resultsPerPage, p);
            LOGGER.debug("Requesting page {} with {}", p, requestUrl);
            HttpResult httpResult;
            THROTTLE.hold();
            try {
                httpResult = retriever.httpGet(requestUrl);
            } catch (HttpException e) {
                throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": "
                        + e.getMessage() + ", request URL was \"" + requestUrl + "\"", e);
            }
            // TODO implement checking for error codes.
            String jsonString = httpResult.getStringContent();
            
            try {
                JsonObject resultJson = new JsonObject(jsonString);
                JsonObject photosJson = resultJson.getJsonObject("photos");
                if (photosJson.get("total") != null) {
                    availableResults = photosJson.getLong("total");
                }
                JsonArray photoJsonArray = photosJson.getJsonArray("photo");
                for (int i = 0; i < photoJsonArray.size(); i++) {
                    JsonObject photoJson = photoJsonArray.getJsonObject(i);
                    BasicWebImage.Builder builder = new BasicWebImage.Builder();

                    String farmId = photoJson.getString("farm");
                    String serverId = photoJson.getString("server");
                    String id = photoJson.getString("id");
                    String secret = photoJson.getString("secret");
                    String userId = photoJson.getString("owner");
                    String tags = photoJson.getString("tags");

                    builder.setTitle(photoJson.getString("title"));
                    builder.setImageUrl(buildImageUrl(farmId, serverId, id, secret));
                    builder.setUrl(buildPageUrl(id, userId));
                    builder.setSummary(photoJson.getJsonObject("description").getString("_content"));
                    builder.setPublished(parseDate(photoJson.getString("datetaken")));
                    builder.setCoordinate(parseCoordinate(photoJson));
                    builder.setIdentifier(id);

                    // License license = License.get(photoJson.getInt("license"));
                    Integer width = photoJson.tryGetInt("o_width");
                    Integer height = photoJson.tryGetInt("o_height");
                    if (width != null && height != null) {
                        builder.setWidth(width);
                        builder.setHeight(height);
                    }
                    builder.setTags(new HashSet<String>(Arrays.asList(tags.split("\\s"))));
                    builder.setSource(SEARCHER_NAME);
                    result.add(builder.create());
                }
            } catch (JsonException e) {
                throw new SearcherException("Parse error while searching for \"" + query + "\" with " + getName()
                        + ": " + e.getMessage() + ", JSON was \"" + jsonString + "\"", e);
            }
        }
        return new SearchResults<WebImage>(result, availableResults);
    }

    private static final GeoCoordinate parseCoordinate(JsonObject photoJson) throws JsonException {
        double lat = photoJson.getDouble("latitude");
        double lng = photoJson.getDouble("longitude");
        if (lat == 0.0 && lng == 0.0) {
            return null;
        }
        return new ImmutableGeoCoordinate(lat, lng);
    }

    private static final Date parseDate(String string) {
        try {
            return new SimpleDateFormat(DATE_PATTERN).parse(string);
        } catch (ParseException e) {
            LOGGER.warn("Error while parsing date string '" + string + "'.");
            return null;
        }
    }

    /**
     * @param query The {@link MultifacetQuery} to process.
     * @param perPage Number of results to return per page.
     * @param page The page to return.
     * @return The query URL.
     */
    private String buildRequestUrl(MultifacetQuery query, int perPage, int page) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://api.flickr.com/services/rest/");
        urlBuilder.append("?method=flickr.photos.search");
        urlBuilder.append("&api_key=").append(apiKey);
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
            double[] bbox = query.getCoordinate().getBoundingBox(query.getRadius());
            String params = String.format("%s,%s,%s,%s", bbox[1], bbox[0], bbox[3], bbox[2]);
            urlBuilder.append("&bbox=").append(params);
        }
        Facet facet = query.getFacet(Licenses.LICENSES_IDENTIFIER);
        if (facet != null) {
            Licenses licensesFacet = (Licenses)facet;
            if (licensesFacet.licenses.size() > 0) {
                urlBuilder.append("&license=").append(licensesFacet.getLicensesString());
            }
        }
        urlBuilder.append("&per_page=").append(perPage);
        urlBuilder.append("&page=").append(page);
        urlBuilder.append("&format=json");
        urlBuilder.append("&nojsoncallback=1");
        urlBuilder.append("&extras=description,license,date_taken,geo,tags,o_dims,");
        LOGGER.debug("Query URL {}", urlBuilder);
        return urlBuilder.toString();
    }

    /**
     * <p>
     * Transforms the given parts back to an image URL.
     * </p>
     * 
     * @param farmId
     * @param serverId
     * @param id
     * @param secret
     * @return
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
     * @param id
     * @param userId
     * @return
     */
    private String buildPageUrl(String id, String userId) {
        return String.format("http://www.flickr.com/photos/%s/%s", userId, id);
    }

}
