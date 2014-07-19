package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
import ws.palladian.retrieval.ranking.RankingServiceException;
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
public final class RedditStats extends AbstractRankingService implements RankingService {

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

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Ranking.Builder builder = new Ranking.Builder(this, url);
        if (isBlocked()) {
            return builder.create();
        }

        try {

            String encUrl = UrlHelper.encodeParameter(url);
            HttpResult httpResult = retriever.httpGet(GET_INFO + encUrl);
            JsonObject json = new JsonObject(httpResult.getStringContent());

            JsonArray children = json.getJsonObject("data").getJsonArray("children");
            int votes = 0;
            int comments = 0;
            for (int i = 0; i < children.size(); i++) {
                JsonObject child = children.getJsonObject(i);
                // all post have "kind" : "t3" -- there is no documentation, what this means,
                // but for robustness sake we check here
                if (child.getString("kind").equals("t3")) {
                    votes += child.getJsonObject("data").getInt("score");
                    comments += child.getJsonObject("data").getInt("num_comments");
                }
            }
            builder.add(VOTES, votes);
            builder.add(COMMENTS, comments);
            LOGGER.trace("Reddit stats for " + url + " : " + builder);

        } catch (JsonException e) {
            checkBlocked();
            throw new RankingServiceException("JSONException " + e.getMessage(), e);
        } catch (HttpException e) {
            checkBlocked();
            throw new RankingServiceException("HttpException " + e.getMessage(), e);
        }
        return builder.create();
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
