package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation for likes, shares and comments on Facebook.
 * </p>
 * 
 * @author Julien Schmehl
 * @author pk
 */
public final class FacebookLinkStats extends AbstractRankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookLinkStats.class);

    /** The id of this service. */
    public static final String SERVICE_ID = "facebook";

    /** The ranking value types of this service **/
    public static final RankingType LIKES = new RankingType("facebook_likes", "Facebook Likes",
            "The number of times Facebook users have \"Liked\" the page, or liked any comments or re-shares of this page.");

    public static final RankingType SHARES = new RankingType("facebook_shares", "Facebook Shares",
            "The number of times users have shared the page on Facebook.");

    public static final RankingType COMMENTS = new RankingType("facebook_comments", "Facebook Comments",
            "The number of comments users have made on the shared story.");

    /** All available ranking types by {@link FacebookLinkStats}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(LIKES, SHARES, COMMENTS);

    /**
     * Facebook allows 600 calls per 600 seconds; see:
     * http://stackoverflow.com/questions/9272391/facebook-application-request-limit-reached
     */
    private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(600, TimeUnit.SECONDS, 550);

    /** Maximum number of URLs to fetch during each request. */
    private static final int BATCH_SIZE = 50;

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        return getRanking(Collections.singletonList(url)).values().iterator().next();
    }

    @Override
    public Map<String, Ranking> getRanking(Collection<String> urls) throws RankingServiceException {
        Map<String, Ranking> results = CollectionHelper.newHashMap();
        List<String> urlBatch = CollectionHelper.newArrayList();
        for (String url : CollectionHelper.newHashSet(urls)) {
            urlBatch.add(url);
            if (urlBatch.size() >= BATCH_SIZE) {
                Map<String, Ranking> batchRanking = getRanking2(urlBatch);
                results.putAll(batchRanking);
                urlBatch.clear();
            }
        }
        if (urlBatch.size() > 0) {
            Map<String, Ranking> batchRanking = getRanking2(urlBatch);
            results.putAll(batchRanking);
        }
        return results;
    }

    private Map<String, Ranking> getRanking2(List<String> urls) throws RankingServiceException {
        Validate.isTrue(urls.size() <= BATCH_SIZE);
        THROTTLE.hold();
        Map<String, Ranking> results = new HashMap<String, Ranking>();
        String fqlQuery = createQuery(urls);
        LOGGER.debug("FQL = {}", fqlQuery);
        HttpResult response;
        try {
            HttpRequest postRequest = new HttpRequest(HttpMethod.POST, "https://api.facebook.com/method/fql.query");
            postRequest.addParameter("format", "json");
            postRequest.addParameter("query", fqlQuery);
            response = retriever.execute(postRequest);
        } catch (HttpException e) {
            throw new RankingServiceException("HttpException " + e.getMessage(), e);
        }
        String content = response.getStringContent();
        LOGGER.debug("JSON response = {}", content);
        checkError(content);
        JsonArray json;
        try {
            json = new JsonArray(content);
            for (int i = 0; i < urls.size(); i++) {
                Map<RankingType, Integer> result = CollectionHelper.newHashMap();
                result.put(LIKES, json.getJsonObject(i).getInt("like_count"));
                result.put(SHARES, json.getJsonObject(i).getInt("share_count"));
                result.put(COMMENTS, json.getJsonObject(i).getInt("comment_count"));
                results.put(urls.get(i), new Ranking(this, urls.get(i), result));
                LOGGER.trace("Facebook link stats for {}: {}", urls.get(i), result);
            }
        } catch (JsonException e) {
            throw new RankingServiceException("Error while parsing JSON response (" + content + ")", e);
        }
        return results;
    }

    private String createQuery(List<String> urls) {
        StringBuilder fqlQuery = new StringBuilder();
        fqlQuery.append("select total_count,like_count,comment_count,share_count from link_stat where url in (");
        boolean first = true;
        for (String url : urls) {
            if (first) {
                first = false;
            } else {
                fqlQuery.append(',');
            }
            fqlQuery.append('"');
            fqlQuery.append(UrlHelper.encodeParameter(url));
            fqlQuery.append('"');
        }
        fqlQuery.append(')');
        return fqlQuery.toString();
    }

    /**
     * Check for error (see <a
     * href="https://developers.facebook.com/docs/graph-api/using-graph-api/v2.0#errors">here</a> for error codes).
     * 
     * @param content The result content from the API invocation.
     * @throws RankingServiceException In case an error was returned in the JSON.
     */
    private void checkError(String content) throws RankingServiceException {
        try {
            JsonObject jsonObject = new JsonObject(content);
            Integer errorCode = jsonObject.tryGetInt("error_code");
            String errorMessage = jsonObject.tryGetString("error_msg");
            if (errorCode != null || errorMessage != null) {
                throw new RankingServiceException("Error from API: " + errorMessage + "(" + errorCode + ")");
            }
        } catch (JsonException e) {
            // ignore
        }
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

    public static void main(String[] args) throws RankingServiceException {
        FacebookLinkStats facebookLinkStats = new FacebookLinkStats();
        StopWatch stopWatch = new StopWatch();
        // System.out
        // .println(facebookLinkStats
        // .getRanking("http://www.cinefreaks.com/news/698/Schau-10-Minuten-von-John-Carter-an---im-Kino-ab-08-M%C3%A4rz"));
        List<String> urls = Arrays.asList(
                "http://www.cinefreaks.com/news/698/Schau-10-Minuten-von-John-Carter-an---im-Kino-ab-08-M%C3%A4rz",
                "http://wickedweasel.com/");
//        System.out.println(facebookLinkStats.getRanking("http://wickedweasel.com/"));
        System.out.println(facebookLinkStats.getRanking(urls));
        System.out.println(stopWatch.getElapsedTimeString());
    }
}
