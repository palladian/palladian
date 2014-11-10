package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.List;

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
 * RankingService implementation to find the number of shares of a Web page on Linked In.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public final class LinkedInShares extends AbstractRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkedInShares.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "linkedin";

    /** The ranking value types of this service **/
    public static final RankingType SHARES = new RankingType("linkedinshares", "Linked In Shares",
            "The Number of Shares on Linked In");

    /** All available ranking types by {@link LinkedInShares}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(SHARES);

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Ranking.Builder builder = new Ranking.Builder(this, url);
        if (isBlocked()) {
            return builder.create();
        }

        Integer shares = null;
        String requestUrl = buildRequestUrl(url);

        try {
            HttpResult httpResult = retriever.httpGet(requestUrl);
            String response = httpResult.getStringContent();

            if (response != null) {
                JsonObject jsonObject = new JsonObject(response);

                shares = jsonObject.getInt("count");

                LOGGER.trace("Linked In Shares for " + url + " : " + shares);
            }
        } catch (Exception e) {
            throw new RankingServiceException(e);
        }
        return builder.add(SHARES, shares).create();
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
        return "http://www.linkedin.com/countserv/count/share?format=json&url=" + UrlHelper.encodeParameter(url);
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
        LinkedInShares gpl = new LinkedInShares();
        Ranking ranking = null;

        ranking = gpl.getRanking("http://google.com");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(LinkedInShares.SHARES) + " shares");
    }

}
