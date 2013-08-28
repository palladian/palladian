package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation to count entries containing a given url, aggregated on Friendfeed, excluding internal
 * posts and services that have their own RankingService class. Entries for services already in Rankify (e.g.
 * Twitter,...) are not counted, we should exclude other services if they get their own RankingService implementation.
 * </p>
 * <p>
 * No specifics on rate limiting.
 * </p>
 * 
 * @author Julien Schmehl
 * @see http://www.friendfeed.com/
 * @see http://friendfeed.com/api/services
 */
public final class FriendfeedAggregatedStats extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendfeedAggregatedStats.class);

    private static final String GET_ENTRIES = "http://friendfeed.com/api/feed/url?url=";

    /**
     * The external services users can have in their feed that we don't want to
     * count since we have seperate RankingService classes for them.
     * */
    private static final String[] EXCLUDE_SERVICES = { "internal", "feed", "blog", "delicious", "digg", "facebook",
            "plurk", "reddit", "twitter" };

    /** No config values. */

    /** The id of this service. */
    private static final String SERVICE_ID = "friendfeed_external";

    /** The ranking value types of this service **/
    public static final RankingType ENTRIES = new RankingType("friendfeed_ext_entries",
            "FriendFeed entries for external services",
            "The number of entries from varying services containing the given url on FriendFeed.");
    public static final RankingType LIKES = new RankingType("friendfeed_ext_likes",
            "FriendFeed likes for external services",
            "The number of likes on entries from varying services containing the given url on FriendFeed.");
    public static final RankingType COMMENTS = new RankingType("friendfeed_ext_comments",
            "FriendFeed comments for external services",
            "The number of comments on entries from varying services containing the given url on FriendFeed.");
    /** All available ranking types by {@link FriendfeedAggregatedStats}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(ENTRIES, LIKES, COMMENTS);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 1;

    public FriendfeedAggregatedStats() {
        super();
    }

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);
        if (isBlocked()) {
            return ranking;
        }

        try {
            String encUrl = UrlHelper.encodeParameter(url);
            HttpResult httpResult = retriever.httpGet(GET_ENTRIES + encUrl);
            JSONObject json = new JSONObject(httpResult.getStringContent());

            JSONArray entriesArray = json.getJSONArray("entries");
            float entries = 0;
            float likes = 0;
            float comments = 0;
            for (int i = 0; i < entriesArray.length(); i++) {
                JSONObject post = entriesArray.getJSONObject(i);
                if (!Arrays.asList(EXCLUDE_SERVICES).contains(post.getJSONObject("service").getString("id"))) {
                    entries++;
                    likes += post.getJSONArray("likes").length();
                    comments += post.getJSONArray("comments").length();
                }
            }
            results.put(ENTRIES, entries);
            results.put(LIKES, likes);
            results.put(COMMENTS, comments);
            LOGGER.trace("FriendFeed stats for " + url + " : " + results);

        } catch (JSONException e) {
            checkBlocked();
            throw new RankingServiceException("JSONException " + e.getMessage(), e);
        } catch (HttpException e) {
            checkBlocked();
            throw new RankingServiceException("HttpException " + e.getMessage(), e);
        }
        return ranking;
    }

    @Override
    public boolean checkBlocked() {
        boolean error = false;
        try {
            HttpResult httpResult = retriever.httpGet(GET_ENTRIES + UrlHelper.encodeParameter("http://www.google.com/"));
            JSONObject json = new JSONObject(httpResult.getStringContent());
            if (json.has("errorCode")) {
                if (json.get("errorCode").equals("limit-exceeded")) {
                    error = true;
                }
            }
        } catch (JSONException e) {
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
        LOGGER.error("FriendFeed Aggregated Stats Ranking Service is momentarily blocked. Will check again in 1min.");
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
