package ws.palladian.retrieval.ranking.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * RankingService implementation for PageRank value from <a href="http://www.google.com/">Google</a>. Courtesy limit:
 * 1,000,000 requests/day & 100,000 requests/second/user.
 * </p>
 *
 * @author Julien Schmehl
 * @author Philipp Katz
 */
public final class GooglePageRank extends AbstractRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GooglePageRank.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "pagerank";

    /** The ranking value types of this service **/
    public static final RankingType PAGERANK = new RankingType("pagerank", "Google PageRank", "The PageRank value from Google");
    /** All available ranking types by {@link GooglePageRank}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(PAGERANK);

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Ranking.Builder builder = new Ranking.Builder(this, url);

        Integer pageRank = null;
        try {
            String requestUrl = buildRequestUrl(url);
            HttpResult httpResult = retriever.httpGet(requestUrl);
            String response = httpResult.getStringContent();
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
            throw new RankingServiceException("Exception " + e.getMessage(), e);
        }
        return builder.add(PAGERANK, pageRank).create();
    }

    /**
     * @param url
     * @return
     */
    private String buildRequestUrl(String url) {
        String encUrl = UrlHelper.encodeParameter(url);
        JenkinsHash jHash = new JenkinsHash();
        long urlHash = jHash.hash(("info:" + url).getBytes());
        String requestUrl = "http://toolbarqueries.google.com/tbr?client=navclient-auto&hl=en&ch=6" + urlHash + "&ie=UTF-8&oe=UTF-8&features=Rank&q=info:" + encUrl;
        return requestUrl;
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
