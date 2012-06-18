package ws.palladian.retrieval.ranking.services;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
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
 * RankingService implementation for diggs and comments containing a given url on digg.com.
 * </p>
 * <p>
 * No information about request limits yet.
 * </p>
 * TODO implement limt status correctly -> check API doc
 * 
 * @author Julien Schmehl
 * @author Philipp Katz
 * @see http://digg.com/
 * @see http://digg.com/api/docs/overview
 */
public final class DiggStats extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(DiggStats.class);

    private static final String GET_STORY_INFO = "http://services.digg.com/2.0/story.getInfo?links=";

    /** No config values. */

    /** The id of this service. */
    private static final String SERVICE_ID = "digg";

    /** The ranking value types of this service **/
    public static final RankingType DIGGS = new RankingType("digg_diggs", "Digg.com diggs",
            "The number of times users have \"dugg\" this url on digg.com.");

    public static final RankingType COMMENTS = new RankingType("digg_comments", "Digg.com comments",
            "The number of comments users have left for this digged url on digg.com.");

    /** All available ranking types by {@link DiggStats}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(DIGGS, COMMENTS);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 1;

    public DiggStats() {
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
            HttpResult httpResult = retriever.httpGet(GET_STORY_INFO + encUrl);
            JSONObject json = new JSONObject(HttpHelper.getStringContent(httpResult));
            float diggs = 0;
            float comments = 0;
            if (json.getJSONArray("stories").length() > 0) {
                diggs = json.getJSONArray("stories").getJSONObject(0).getInt("diggs");
                comments = json.getJSONArray("stories").getJSONObject(0).getInt("comments");
            }
            results.put(DIGGS, diggs);
            results.put(COMMENTS, comments);
            LOGGER.trace("Digg stats for " + url + " : " + results);

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
    public Map<String, Ranking> getRanking(List<String> urls) {

        Map<String, Ranking> results = new HashMap<String, Ranking>();
        if (isBlocked()) {
            return results;
        }
        List<String> tempUrls = new ArrayList<String>(urls);
        String encUrls = "";

        try {
            for (int i = 0; i < urls.size(); i++) {
                encUrls += UrlHelper.urlEncode(urls.get(i));
                if (i < urls.size() - 1) {
                    encUrls += ",";
                }
            }

            HttpResult httpResult = retriever.httpGet(GET_STORY_INFO + encUrls);
            JSONObject json = new JSONObject(HttpHelper.getStringContent(httpResult));

            Timestamp retrieved = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());

            JSONArray stories = json.getJSONArray("stories");

            String url = "";
            float diggs = -1;
            float comments = -1;

            // iterate through "stories" and add rankings to results map
            // delete every URL found in the response from tempUrls
            for (int i = 0; i < stories.length(); i++) {
                url = stories.getJSONObject(i).getString("url");
                diggs = stories.getJSONObject(i).getInt("diggs");
                comments = stories.getJSONObject(i).getInt("comments");
                Map<RankingType, Float> result = new HashMap<RankingType, Float>();
                result.put(DIGGS, diggs);
                result.put(COMMENTS, comments);
                results.put(url, new Ranking(this, url, result, retrieved));
                tempUrls.remove(url);
                LOGGER.trace("Digg stats for " + url + " : " + result);
            }
            // add the remaining URLs (which were not in the list of "stories") with a ranking of 0
            for (String u : tempUrls) {
                Map<RankingType, Float> result = new HashMap<RankingType, Float>();
                result.put(DIGGS, 0f);
                result.put(COMMENTS, 0f);
                results.put(u, new Ranking(this, u, result, retrieved));
            }

        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
            checkBlocked();
        } catch (HttpException e) {
            LOGGER.error("HttpException " + e.getMessage());
            checkBlocked();
        }

        return results;

    }

    @Override
    public boolean checkBlocked() {
        int status = -1;
        try {
            status = retriever.httpGet(GET_STORY_INFO + "http://www.google.com/").getStatusCode();
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
        LOGGER.error("Digg Ranking Service is momentarily blocked. Will check again in 1min.");
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
