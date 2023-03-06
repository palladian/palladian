package ws.palladian.retrieval.ranking.services;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * RankingService implementation to get the number of posts containing a given URL on plurk.com. Does fulltext search,
 * e.g. it finds also posts that have parts of the url - only usable for longer URLs.
 * </p>
 * <p>
 * Current limit is 50.000 calls pr. day
 * </p>
 * TODO implement follow up request if has_more:true
 *
 * @author Julien Schmehl
 * @see http://www.plurk.com
 */
public final class PlurkPosts extends AbstractRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PlurkPosts.class);

    /** {@link Configuration} key for the API key. */
    public static final String CONFIG_API_KEY = "api.plurk.key";

    /** The config values. */
    private final String apiKey;

    /** The id of this service. */
    private static final String SERVICE_ID = "plurk";

    /** The ranking value types of this service **/
    public static final RankingType POSTS = new RankingType("plurk_posts", "Plurk.com posts", "The number of posts on plurk.com mentioning this url.");
    /** All available ranking types by {@link PlurkPosts}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(POSTS);

    /**
     * <p>
     * Create a new {@link PlurkPosts} ranking service.
     * </p>
     *
     * @param configuration The configuration which must provide an API key (<tt>api.plurk.key</tt>) for accessing the
     *                      service.
     */
    public PlurkPosts(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    /**
     * <p>
     * Create a new {@link PlurkPosts} ranking service.
     * </p>
     *
     * @param apiKey The required API key for accessing the service, not <code>null</code> or empty.
     */
    public PlurkPosts(String apiKey) {
        Validate.notEmpty(apiKey, "The required API key is missing.");
        this.apiKey = apiKey;
    }

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Ranking.Builder builder = new Ranking.Builder(this, url);

        try {
            String encUrl = UrlHelper.encodeParameter(url);
            HttpResult httpResult = retriever.httpGet("http://www.plurk.com/API/PlurkSearch/search?api_key=" + getApiKey() + "&query=" + encUrl);
            JsonObject json = new JsonObject(httpResult.getStringContent());
            JsonArray plurks = json.getJsonArray("plurks");
            builder.add(POSTS, plurks.size());
            LOGGER.trace("Plurk.com posts for " + url + " : " + plurks.size());

        } catch (JsonException e) {
            throw new RankingServiceException("JSONException " + e.getMessage(), e);
        } catch (HttpException e) {
            throw new RankingServiceException("JSONException " + e.getMessage(), e);
        }
        return builder.create();
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

    public String getApiKey() {
        return apiKey;
    }

}
