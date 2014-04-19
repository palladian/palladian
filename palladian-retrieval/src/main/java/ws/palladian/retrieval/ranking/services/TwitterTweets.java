package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation to find the number of tweets of a URL on Twitter .
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public final class TwitterTweets extends AbstractRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitterTweets.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "twitter";

    /** The ranking value types of this service **/
    public static final RankingType TWEETS = new RankingType("twittertweets", "Twitter Tweets",
            "The Number of Tweets on Twitter");

    /** All available ranking types by {@link TwitterTweets}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(TWEETS);


    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);
        if (isBlocked()) {
            return ranking;
        }

        Integer tweets = null;
        String requestUrl = buildRequestUrl(url);

        try {
            HttpResult httpResult = retriever.httpGet(requestUrl);
            String response = httpResult.getStringContent();

            if (response != null) {
                JsonObject jsonObject = new JsonObject(response);

                tweets = jsonObject.getInt("count");

                LOGGER.trace("Twitter Tweets for " + url + " : " + tweets);
            }
        } catch (Exception e) {
            throw new RankingServiceException(e);
        }

        results.put(TWEETS, (float)tweets);
        return ranking;
    }

    /**
     * <p>
     * Build the request URL.
     * </p>
     * 
     * @param url The URL to search for.
     * @return The request URL.
     */
    private String buildRequestUrl(String url) {
        return "http://urls.api.twitter.com/1/urls/count.json?url=" + UrlHelper.encodeParameter(url);
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

    public static void main(String[] a) throws RankingServiceException {
        TwitterTweets gpl = new TwitterTweets();
        Ranking ranking = null;

        ranking = gpl
                .getRanking("http://lifehacker.com/5945328/bikn-connects-your-iphone-to-your-stuff-or-children-or-animals-so-youll-never-lose-them");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(TwitterTweets.TWEETS) + " tweets");
    }

}
