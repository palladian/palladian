package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.ThreadHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation to find whether a certain URL has been cached by Google.
 * </p>
 * 
 * @author David Urbansky
 */
public final class GoogleCachedPage extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCachedPage.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "Google Cache";

    /** The ranking value types of this service **/
    public static final RankingType GOOGLE_CACHED = new RankingType("googlecached", "Google Indexed",
            "Whether the page is in Google's Cache");

    /** All available ranking types by {@link GoogleCachedPage}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(GOOGLE_CACHED);

    /** Fields to check the service availability. */
    private long sleepTime = TimeUnit.SECONDS.toMillis(10);

    /** The time in milliseconds we wait between two requests. */
    private static final int THROTTLING_INTERVAL_MS = 1000;

    /** The timestamp of the last request. */
    private Long lastRequestTimestamp;

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);

        throttleQuery();

        double indexed = 0.;
        String requestUrl = buildRequestUrl(url);

        try {

            boolean success = false;

            while (!success) {

                HttpResult httpHead = retriever.httpHead(requestUrl);

                success = true;

                if (httpHead.getStatusCode() < 400) {
                    indexed = 1;
                } else if (httpHead.getStatusCode() >= 500) {
                    LOGGER.error("too many frequent requests, we're blocked");
                    success = false;
                    ThreadHelper.deepSleep(TimeUnit.SECONDS.toMillis(sleepTime));
                    sleepTime += Math.random() * 10;
                }

                if (success) {
                    sleepTime = TimeUnit.SECONDS.toMillis(10);
                }

            }

        } catch (HttpException e) {
            throw new RankingServiceException(e);
        }

        results.put(GOOGLE_CACHED, (float)indexed);
        return ranking;
    }

    private synchronized void throttleQuery() {
        if (lastRequestTimestamp != null) {
            long millisSinceLastRequest = System.currentTimeMillis() - lastRequestTimestamp;
            if (millisSinceLastRequest < THROTTLING_INTERVAL_MS) {
                try {
                    long millisToSleep = THROTTLING_INTERVAL_MS - millisSinceLastRequest;
                    Thread.sleep(millisToSleep);
                } catch (InterruptedException e) {
                    LOGGER.warn("InterruptedException");
                }
            }
        }
        lastRequestTimestamp = System.currentTimeMillis();
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
        return "http://webcache.googleusercontent.com/search?q=cache:" + url;
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
        GoogleCachedPage gpl = new GoogleCachedPage();
        Ranking ranking = null;

        ranking = gpl.getRanking("http://webknox.com/p/best-funny-comic-strips");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(GoogleCachedPage.GOOGLE_CACHED) + " -> indexed");
    }

}
