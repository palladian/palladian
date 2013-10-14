package ws.palladian.retrieval.search.socialmedia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.ImmutableGeoCoordinate;
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
import ws.palladian.retrieval.search.MultifacetQuery;
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

            HttpResult httpResult;
            try {
                httpResult = retriever.httpGet(queryUrl);
            } catch (HttpException e) {
                throw new SearcherException("Encountered HTTP exception while accessing \"" + queryUrl + "\": "
                        + e.getMessage(), e);
            }

            String jsonString = httpResult.getStringContent();

            try {
                JsonObject jsonResult = new JsonObject(jsonString);
                JsonArray dataArray = jsonResult.getJsonArray("data");

                for (int i = 0; i < dataArray.size(); i++) {
                    JsonObject data = dataArray.getJsonObject(i);
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
                            builder.setCoordinate(new ImmutableGeoCoordinate(latitude, longitude));
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
                    result.add(builder.create());

                    if (result.size() == resultCount) {
                        break page;
                    }

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
                throw new SearcherException("Parse exception while parsing JSON data: \"" + jsonString + "\"", e);
            }

        }
        return result;
    }

    @Override
    public SearchResults<WebImage> search(MultifacetQuery query) throws SearcherException {

        String tag = getTag(query);
        String queryUrl;

        if (tag != null) {
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
            throw new SearcherException("Search must either provide a tag or a geographic coordinate and a radius.");
        }

        LOGGER.debug("Query URL: {}", queryUrl);
        return new SearchResults<WebImage>(processResult(query.getResultCount(), queryUrl));
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
        if (allTags.size() > 1) {
            LOGGER.warn("Query consists of multiple terms ({}), only the first one ({}) is considered!", allTags,
                    firstTag);
        }
        return firstTag;
    }

}
