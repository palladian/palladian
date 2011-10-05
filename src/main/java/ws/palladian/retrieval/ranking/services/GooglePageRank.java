package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

import com.temesoft.google.pr.JenkinsHash;

/**
 * <p>
 * RankingService implementation for PageRank value from Google.
 * </p>
 * <p>
 * Courtesy limit: 1,000,000 requests/day & 100,000 requests/second/user.
 * </p>
 * 
 * @author Julien Schmehl
 * @author Christopher Friedrich
 * @see http://www.google.com/
 * 
 */
public class GooglePageRank extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(GooglePageRank.class);

    /** The config values. */
    private String apiKey;

    /** The id of this service. */
    private static final String SERVICE_ID = "pagerank";

    /** The ranking value types of this service **/
    static RankingType PAGERANK = new RankingType("pagerank", "Google PageRank", "The PageRank value from Google");
    static List<RankingType> RANKING_TYPES = new ArrayList<RankingType>();
    static {
        RANKING_TYPES.add(PAGERANK);
    }

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 1;

    public GooglePageRank() {
        super();

        PropertiesConfiguration configuration = ConfigHolder.getInstance().getConfig();

        if (configuration != null) {
            setApiKey(configuration.getString("api.google.key"));
        } else {
            LOGGER.warn("could not load configuration, ranking retrieval won't work");
        }

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

            // original code from ws.palladian.retrieval.ranking.RankingRetriever
            JenkinsHash jHash = new JenkinsHash();
            long urlHash = jHash.hash(("info:" + url).getBytes());
            String response = retriever
                    .getTextDocument("http://toolbarqueries.google.com/search?client=navclient-auto&hl=en&" + "ch=6"
                            + urlHash + "&ie=UTF-8&oe=UTF-8&features=Rank&q=info:" + encUrl);

            if (response != null) {
                int result = 0;
                // result stays 0 if response empty -> url not found
                if (response.contains(":")) {
                    response = response.split(":")[2].trim();
                    result = Integer.valueOf(response);
                }
                results.put(PAGERANK, (float) result / 10);
                LOGGER.trace("Google PageRank for " + url + " : " + result);
            } else {
                results.put(PAGERANK, null);
                LOGGER.trace("Google PageRank for " + url + "could not be fetched");
                checkBlocked();
            }
        } catch (Exception e) {
            LOGGER.error("Exception " + e.getMessage());
            checkBlocked();
        }
        return ranking;
    }

    @Override
    public boolean checkBlocked() {
        int status = -1;
        try {
            JenkinsHash jHash = new JenkinsHash();
            long urlHash = jHash.hash("info:http://www.google.com/".getBytes());
            status = retriever.httpGet(
                    "http://toolbarqueries.google.com/search?client=navclient-auto&hl=en&" + "ch=6" + urlHash
                            + "&ie=UTF-8&oe=UTF-8&features=Rank&q=info:http://www.google.com/").getStatusCode();
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

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

}
