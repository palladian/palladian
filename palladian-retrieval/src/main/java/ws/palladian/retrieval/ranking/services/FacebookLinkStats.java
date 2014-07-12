package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.UrlHelper;
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

//    private static final String FQL_QUERY = "https://api.facebook.com/method/fql.query?format=json&query=select+total_count,like_count,comment_count,share_count+from+link_stat+where+";

    // alternatively
    // private static final String GRAPH_FQL_QUERY =
    // "https://graph.facebook.com/fql?q=SELECT+total_count,like_count,comment_count,share_count+FROM+link_stat+WHERE+";

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

//    /** Fields to check the service availability. */
//    private static boolean blocked = false;
//    private static long lastCheckBlocked;
//    private final static int checkBlockedIntervall = 1000 * 60 * 1;
    
    /**
     * Facebook allows 600 calls per 600 seconds; see:
     * http://stackoverflow.com/questions/9272391/facebook-application-request-limit-reached
     */
    private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(600, TimeUnit.SECONDS, 550);

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {

//        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
//        Ranking ranking = new Ranking(this, url, results);
//        if (isBlocked()) {
//            return ranking;
//        }
//
//        try {
//
//            String encUrl = UrlHelper.encodeParameter(url);
//            JsonObject json = null;
//            String requestUrl = FQL_QUERY + "url='" + encUrl + "'";
//            try {
//                HttpResult httpResult = retriever.httpGet(requestUrl);
//
//                JsonArray jsonArray = new JsonArray(httpResult.getStringContent());
//                if (jsonArray.size() == 1) {
//                    json = jsonArray.getJsonObject(0);
//                }
//            } catch (HttpException e) {
//                LOGGER.error("HttpException for {}", requestUrl, e);
//            }
//            if (json != null) {
//                results.put(LIKES, (float)json.getInt("like_count"));
//                results.put(SHARES, (float)json.getInt("share_count"));
//                results.put(COMMENTS, (float)json.getInt("comment_count"));
//                LOGGER.trace("Facebook link stats for " + url + " : " + results);
//            } else {
//                results.put(LIKES, null);
//                results.put(SHARES, null);
//                results.put(COMMENTS, null);
//                LOGGER.trace("Facebook link stats for " + url + "could not be fetched");
//                checkBlocked();
//            }
//        } catch (JsonException e) {
//            checkBlocked();
//            throw new RankingServiceException("JSONException (URL: " + url + ") " + e.getMessage(), e);
//        }
//        return ranking;
        
        return getRanking(Collections.singletonList(url)).values().iterator().next();
    }

    @Override
    public Map<String, Ranking> getRanking(List<String> urls) throws RankingServiceException {
        
        THROTTLE.hold();

        Map<String, Ranking> results = new HashMap<String, Ranking>();
//        if (isBlocked()) {
//            return results;
//        }
//        String encUrls = "";

        try {

//            for (int i = 0; i < urls.size(); i++) {
//                if (i == urls.size() - 1) {
//                    encUrls += "url='" + UrlHelper.encodeParameter(urls.get(i)) + "'";
//                } else {
//                    encUrls += "url='" + UrlHelper.encodeParameter(urls.get(i)) + "' or ";
//                }
//            }
            
            StringBuilder encUrls = new StringBuilder();
            boolean first = true;
            for (String url : urls) {
                if (first) {
                    first = false;
                } else {
                    encUrls.append(',');
                }
                encUrls.append('"');
                encUrls.append(UrlHelper.encodeParameter(url));
                encUrls.append('"');
            }

            HttpRequest postRequest = new HttpRequest(HttpMethod.POST, "https://api.facebook.com/method/fql.query");
            postRequest.addParameter("format", "json");
            postRequest.addParameter("query",
//                    "select total_count,like_count,comment_count,share_count from link_stat where " + encUrls);
            "select total_count,like_count,comment_count,share_count from link_stat where url in (" + encUrls + ")");

            HttpResult response = retriever.execute(postRequest);
            String content = response.getStringContent();
            LOGGER.trace("JSON response = {}", content);
            checkError(content);
            JsonArray json = null;
//            if (content.length() > 0) {
                try {
                    json = new JsonArray(content);
                } catch (JsonException e) {
//                    LOGGER.error("JSONException: " + e.getMessage());
                    throw new RankingServiceException("Error while parsing JSON response (" + content + ")", e);
                }
//            }
                

//            Timestamp retrieved = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());

//            if (json != null) {

//                float likeCount = -1;
//                float shareCount = -1;
//                float commentCount = -1;

                for (int i = 0; i < urls.size(); i++) {
                    float likeCount = json.getJsonObject(i).getInt("like_count");
                    float shareCount = json.getJsonObject(i).getInt("share_count");
                    float commentCount = json.getJsonObject(i).getInt("comment_count");
                    Map<RankingType, Float> result = new HashMap<RankingType, Float>();
                    result.put(LIKES, likeCount);
                    result.put(SHARES, shareCount);
                    result.put(COMMENTS, commentCount);
//                    results.put(urls.get(i), new Ranking(this, urls.get(i), result, retrieved));
                    results.put(urls.get(i), new Ranking(this, urls.get(i), result));
                    LOGGER.trace("Facebook link stats for {}: {}", urls.get(i), result);
                }
//            } else {
//                for (String u : urls) {
//                    Map<RankingType, Float> result = new HashMap<RankingType, Float>();
//                    result.put(LIKES, null);
//                    result.put(SHARES, null);
//                    result.put(COMMENTS, null);
//                    results.put(u, new Ranking(this, u, result, retrieved));
//                }
//                LOGGER.trace("Facebook link stats for " + urls + "could not be fetched");
//                checkBlocked();
//            }
        } catch (JsonException e) {
            checkBlocked();
            throw new RankingServiceException("JSONException " + e.getMessage(), e);
        } catch (HttpException e) {
            throw new RankingServiceException("HttpException " + e.getMessage(), e);
        }

        return results;
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

//    @Override
//    public boolean checkBlocked() {
//        int status = -1;
//        try {
//            status = retriever.httpGet(FQL_QUERY + "url='http://www.google.com/'").getStatusCode();
//        } catch (HttpException e) {
//            LOGGER.error("HttpException " + e.getMessage());
//        }
//        if (status == 200) {
//            blocked = false;
//            lastCheckBlocked = new Date().getTime();
//            return false;
//        }
//        blocked = true;
//        lastCheckBlocked = new Date().getTime();
//        LOGGER.error("Facebook Ranking Service is momentarily blocked. Will check again in 1min.");
//        return true;
//    }

//    @Override
//    public boolean isBlocked() {
//        if (new Date().getTime() - lastCheckBlocked < checkBlockedIntervall) {
//            return blocked;
//        } else {
//            return checkBlocked();
//        }
//    }
//
//    @Override
//    public void resetBlocked() {
//        blocked = false;
//        lastCheckBlocked = new Date().getTime();
//    }

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
