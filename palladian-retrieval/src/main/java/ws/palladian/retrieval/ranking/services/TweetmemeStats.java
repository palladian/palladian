package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation for tweets containing a given URL.
 * </p>
 * <p>
 * Currently 250 req/h, whitelisting possible. Information about current limits in HTTP headers: X-RateLimit-Limit and
 * X-RateLimit-Remaining.
 * </p>
 * 
 * @author Julien Schmehl
 * @see http://help.tweetmeme.com/category/developers/api/
 */
public final class TweetmemeStats extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(TweetmemeStats.class);

    /** No config values. */

    /** The id of this service. */
    private static final String SERVICE_ID = "tweetmeme";

    /** The ranking value types of this service **/
    public static final RankingType TWEETS = new RankingType("twitter_tweets", "Twitter tweets",
            "The number of tweets mentioning this url, derived from tweetmeme.");

    /** All available ranking types by TweetmemeStats. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(TWEETS);

    /**
     * The number of comments tweets mentioning this url.
     * Commitment value is 1.5128
     * Max. Ranking value is 3
     * 
     * This RankingType is not used at the moment
     * 
     */
    /*
     * static RankingType COMMENTS = new RankingType("tweetmeme_comments", "Tweetmeme comments", "The number of " +
     * "comments on tweetmeme for this url.", 1.5128f, 3, new int[]{0,0,0,0,0,1,1,2,3});
     */

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 60;

    public TweetmemeStats() {
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

            String encUrl = UrlHelper.urlEncode(url);
            HttpResult httpResult = retriever.httpGet("http://api.tweetmeme.com/url_info.json?url=" + encUrl);
            JSONObject json = new JSONObject(HttpHelper.getStringContent(httpResult));

            if (json.has("story")) {
                float count = json.getJSONObject("story").getInt("url_count");
                // int comments = json.getJSONObject("story").getInt("comment_count");
                results.put(TWEETS, count);
                // results.put(COMMENTS, COMMENTS.normalize(comments));
                LOGGER.trace("Tweetmeme stats for " + url + " : " + results);
            } else if (json.has("comment")) {
                if (json.getString("comment").equals("unable to resolve URL")) {
                    results.put(TWEETS, 0f);
                    // results.put(COMMENTS, 0f);
                    LOGGER.trace("Tweetmeme stats for " + url + " : " + results);
                }
            } else {
                results.put(TWEETS, null);
                // results.put(COMMENTS, null);
                LOGGER.trace("Tweetmeme stats for " + url + "could not be fetched");
                checkBlocked();
            }

        } catch (JSONException e) {
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
            HttpResult httpResult = retriever.httpGet("http://api.tweetmeme.com/url_info.json?url="
                    + UrlHelper.urlEncode("http://www.google.com/"));
            JSONObject json = new JSONObject(HttpHelper.getStringContent(httpResult));
            if (json.has("status")) {
                if (json.get("status").equals("failure")) {
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
        LOGGER.error("Twitter Ranking Service is momentarily blocked. Will check again in 1h. Try resetting your IP-Address.");
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
