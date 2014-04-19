package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation for posts containing a given url on FriendFeed. Only entries with the service id
 * "internal" are not counted, these are FriendFeed posts.
 * </p>
 * <p>
 * No specifics on rate limiting.
 * </p>
 * 
 * @author Julien Schmehl
 * @see http://www.friendfeed.com/
 * @see http://friendfeed.com/api/services
 */
public final class FriendfeedStats extends AbstractRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendfeedStats.class);

    private static final String GET_ENTRIES = "http://friendfeed.com/api/feed/url?url=";

    /** The id of this service. */
    private static final String SERVICE_ID = "friendfeed";

    /** The ranking value types of this service **/
    public static final RankingType POSTS = new RankingType("friendfeed_int_posts", "FriendFeed posts",
            "The number of entries posted on FriendFeed containing the given url.");
    public static final RankingType LIKES = new RankingType("friendfeed_int_likes", "FriendFeed likes",
            "The number of likes for entries posted on FriendFeed containing the given url.");
    public static final RankingType COMMENTS = new RankingType("friendfeed_int_comments", "FriendFeed comments",
            "The number of comments for entries posted on FriendFeed containing the given url.");
    /** All available ranking types by {@link FriendfeedStats}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(POSTS, LIKES, COMMENTS);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 1;

    public FriendfeedStats() {
        super();
    }

    @Override
    public Ranking getRanking(String url) {

        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);
        if (isBlocked()) {
            return ranking;
        }

        try {
            String encUrl = UrlHelper.encodeParameter(url);
            HttpResult httpResult = retriever.httpGet(GET_ENTRIES + encUrl);
            JsonObject json = new JsonObject(httpResult.getStringContent());
            JsonArray entries = json.getJsonArray("entries");
            float posts = 0;
            float likes = 0;
            float comments = 0;
            for (int i = 0; i < entries.size(); i++) {
                JsonObject post = entries.getJsonObject(i);
                if (post.getJsonObject("service").getString("id").equals("internal")) {
                    posts++;
                    likes += post.getJsonArray("likes").size();
                    comments += post.getJsonArray("comments").size();
                }
            }
            results.put(POSTS, posts);
            results.put(LIKES, likes);
            results.put(COMMENTS, comments);
            LOGGER.trace("FriendFeed stats for " + url + " : " + results);
        } catch (JsonException e) {
            LOGGER.error("JSONException " + e.getMessage());
            checkBlocked();
        } catch (HttpException e) {
            LOGGER.error("HttpException " + e.getMessage());
            checkBlocked();
        }
        return ranking;
    }

    @Override
    public boolean checkBlocked() {
        boolean error = false;
        try {
            HttpResult httpResult = retriever.httpGet(GET_ENTRIES + UrlHelper.encodeParameter("http://www.google.com/"));
            JsonObject json = new JsonObject(httpResult.getStringContent());
            if (json != null) {
                if (json.get("errorCode") != null) {
                    if (json.get("errorCode").equals("limit-exceeded")) {
                        error = true;
                    }
                }
            }
        } catch (JsonException e) {
            LOGGER.error("JSONException " + e.getMessage());
        } catch (HttpException e) {
            LOGGER.error("HttpException " + e.getMessage());
        }
        if (!error) {
            blocked = false;
            lastCheckBlocked = new Date().getTime();
            return false;
        }
        blocked = true;
        lastCheckBlocked = new Date().getTime();
        LOGGER.error("FriendFeed Ranking Service is momentarily blocked. Will check again in 1min.");
        return true;
    }

    @Override
    public boolean isBlocked() {
        if (new Date().getTime() - lastCheckBlocked < checkBlockedIntervall) {
            return blocked;
        } else {
            return checkBlocked();
        }
    }

    @Override
    public void resetBlocked() {
        blocked = false;
        lastCheckBlocked = new Date().getTime();
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

}
