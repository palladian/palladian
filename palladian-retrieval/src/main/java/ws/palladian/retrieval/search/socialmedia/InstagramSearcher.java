package ws.palladian.retrieval.search.socialmedia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
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

    /** The identifier for the {@link Configuration} key with the access token. */
    public static final String CONFIG_ACCESS_TOKEN = "api.instagram.accessToken";

    /** Name of this searcher. */
    private static final String SEARCHER_NAME = "Instagram";

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

    private List<WebImage> processResult(int resultCount, String queryUrl) throws SearcherException {
        List<WebImage> result = new ArrayList<WebImage>();

        page: for (;;) {

            THROTTLE.hold();
            HttpResult httpResult;
            try {
                httpResult = retriever.httpGet(queryUrl);
                if (httpResult.getStatusCode() == 503) {
                    throw new RateLimitedException("Rate limit exceeded.", null);
                }
                if (httpResult.errorStatus()) {
                    throw new SearcherException("Received HTTP code " + httpResult.getStatusCode()
                            + " while accessing \"" + queryUrl + "\" ("
                            + getErrorMessage(httpResult.getStringContent()) + ")");
                }
            } catch (HttpException e) {
                throw new SearcherException("Encountered HTTP exception while accessing \"" + queryUrl + "\": "
                        + e.getMessage(), e);
            }

            String jsonString = httpResult.getStringContent();
            System.out.println(jsonString);

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
     * Parse a JSON entry into a WebImage.
     * 
     * @param data The JSON data.
     * @return A parsed WebImage.
     * @throws JsonException In case parsing failes.
     */
    private WebImage parseJsonObject(JsonObject data) throws JsonException {
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
            JsonObject jsonLocaiton = data.getJsonObject("location");
            if (jsonLocaiton.get("longitude") != null && jsonLocaiton.get("latitude") != null) {
                double longitude = jsonLocaiton.getDouble("longitude");
                double latitude = jsonLocaiton.getDouble("latitude");
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
    private String getErrorMessage(String stringContent) {
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
        String queryUrl;

        // we search by URL; but first, we need to get the ID ...
        if (query.getUrl() != null) {
            id = getIdForUrl(query.getUrl());
        }

        if (id != null) {
            queryUrl = String.format("https://api.instagram.com/v1/media/%s?access_token=%s", id, accessToken);
        } else if (tag != null) {
            queryUrl = String.format("https://api.instagram.com/v1/tags/%s/media/recent?access_token=%s",
                    UrlHelper.encodeParameter(tag), accessToken);
        } else if (query.getCoordinate() != null) {
            GeoCoordinate coordinate = query.getCoordinate();
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("https://api.instagram.com/v1/media/search");
            urlBuilder.append("?lat=").append(coordinate.getLatitude());
            urlBuilder.append("&lng=").append(coordinate.getLongitude());
            if (query.getStartDate() != null) {
                long minTimestamp = query.getStartDate().getTime() / 1000;
                urlBuilder.append("&min_timestamp=").append(minTimestamp);
            }
            if (query.getEndDate() != null) {
                long maxTimestamp = query.getEndDate().getTime() / 1000;
                urlBuilder.append("&max_timestamp=").append(maxTimestamp);
            }
            // 5000 meteres is maximum radius
            double radius = query.getRadius() != null ? query.getRadius() * 1000 : 5000;
            urlBuilder.append("&distance=").append(radius);
            urlBuilder.append("&access_token=").append(accessToken);
            queryUrl = urlBuilder.toString();
        } else {
            throw new SearcherException(
                    "Search must either provide a tag, a geographic coordinate and a radius, a URL or an Instagram ID.");
        }

        LOGGER.debug("Query URL: {}", queryUrl);
        return new SearchResults<WebImage>(processResult(query.getResultCount(), queryUrl));
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
    private String getTag(MultifacetQuery query) {
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

}
