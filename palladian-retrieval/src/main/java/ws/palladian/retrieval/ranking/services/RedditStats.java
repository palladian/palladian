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
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation for votes and comments on a given url on reddit.com.
 * </p>
 * <p>
 * Not more than 1 request every 2 seconds
 * </p>
 * 
 * @author Julien Schmehl
 * @author Philipp Katz
 * @see http://www.reddit.com/
 * @see http://code.reddit.com/wiki/API
 */
public final class RedditStats extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RedditStats.class);

    private static final String GET_INFO = "http://www.reddit.com/api/info.json?url=";

    /** The id of this service. */
    private static final String SERVICE_ID = "reddit";

    /** The ranking value types of this service **/
    public static final RankingType VOTES = new RankingType("reddit_votes", "Reddit.com votes",
            "The number of up-votes minus down-votes for this url on reddit.com.");
    public static final RankingType COMMENTS = new RankingType("reddit_comments", "Reddit.com comments",
            "The number of comments users have left for this url on reddit.com.");
    /** All available ranking types by {@link RedditStats}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(VOTES, COMMENTS);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 1;

    public RedditStats() {
        super();
        // we could use proxies here to circumvent request limitations (1req/2sec)
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
            HttpResult httpResult = retriever.httpGet(GET_INFO + encUrl);
            JSONObject json = new JSONObject(HttpHelper.getStringContent(httpResult));

            JSONArray children = json.getJSONObject("data").getJSONArray("children");
            float votes = 0;
            float comments = 0;
            for (int i = 0; i < children.length(); i++) {
                JSONObject child = children.getJSONObject(i);
                // all post have "kind" : "t3" -- there is no documentation, what this means,
                // but for robustness sake we check here
                if (child.getString("kind").equals("t3")) {
                    votes += child.getJSONObject("data").getInt("score");
                    comments += child.getJSONObject("data").getInt("num_comments");
                }
            }
            results.put(VOTES, votes);
            results.put(COMMENTS, comments);
            LOGGER.trace("Reddit stats for " + url + " : " + results);

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
        int status = -1;
        try {
            status = retriever.httpGet(GET_INFO + "http://www.google.com/").getStatusCode();
        } catch (HttpException e) {
            LOGGER.error("HttpException " + e.getMessage());
        }
        if (status == 200) {
            blocked = false;
            lastCheckBlocked = new Date().getTime();
            return false;
        }
        blocked = true;
        lastCheckBlocked = new Date().getTime();
        LOGGER.error("Reddit Ranking Service is momentarily blocked. Will check again in 1min.");
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
