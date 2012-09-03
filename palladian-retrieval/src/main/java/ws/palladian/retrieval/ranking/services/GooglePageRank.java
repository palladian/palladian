package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation for PageRank value from <a href="http://www.google.com/">Google</a>. Courtesy limit:
 * 1,000,000 requests/day & 100,000 requests/second/user.
 * </p>
 * 
 * @author Julien Schmehl
 * @author Philipp Katz
 */
public final class GooglePageRank extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(GooglePageRank.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "pagerank";

    /** The ranking value types of this service **/
    public static final RankingType PAGERANK = new RankingType("pagerank", "Google PageRank",
            "The PageRank value from Google");
    /** All available ranking types by {@link GooglePageRank}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(PAGERANK);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 1;

    @Override
    public Ranking getRanking(String url) {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);
        if (isBlocked()) {
            return ranking;
        }

        Integer pageRank = null;
        try {
            String requestUrl = buildRequestUrl(url);
            HttpResult httpResult = retriever.httpGet(requestUrl);
            String response = HttpHelper.getStringContent(httpResult);
            if (response != null) {
                pageRank = 0;
                // result stays 0 if response empty -> url not found
                if (response.contains(":")) {
                    response = response.split(":")[2].trim();
                    pageRank = Integer.valueOf(response);
                }
                LOGGER.trace("Google PageRank for " + url + " : " + pageRank);
            }
        } catch (Exception e) {
            LOGGER.error("Exception " + e.getMessage());
            checkBlocked();
        }
        results.put(PAGERANK, (float)pageRank);
        return ranking;
    }

    /**
     * @param url
     * @return
     */
    private String buildRequestUrl(String url) {
        String encUrl = UrlHelper.urlEncode(url);
        JenkinsHash jHash = new JenkinsHash();
        long urlHash = jHash.hash(("info:" + url).getBytes());
        String requestUrl = "http://toolbarqueries.google.com/tbr?client=navclient-auto&hl=en&ch=6" + urlHash
                + "&ie=UTF-8&oe=UTF-8&features=Rank&q=info:" + encUrl;
        return requestUrl;
    }

    @Override
    public boolean checkBlocked() {
        int status = -1;
        try {
            String requestUrl = buildRequestUrl("http://www.google.com/");
            status = retriever.httpGet(requestUrl).getStatusCode();
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
        LOGGER.error("Google PageRank Ranking Service is momentarily blocked. Will check again in 1min. Try changing your IP-address.");
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
