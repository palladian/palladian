package ws.palladian.retrieval.ranking.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * RankingService implementation to find the number of Google Plus likes of a Web page.
 * </p>
 *
 * @author David Urbansky
 */
public final class GooglePlusLikes extends AbstractRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GooglePlusLikes.class);

    /** The id of this service. */
    public static final String SERVICE_ID = "googleplus";

    /** The ranking value types of this service **/
    public static final RankingType LIKES = new RankingType("googlepluslikes", "Google Plus Likes", "The Number of Likes on Google Plus");

    /** All available ranking types by {@link GooglePlusLikes}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(LIKES);

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Ranking.Builder builder = new Ranking.Builder(this, url);

        Integer googlePlusLikes = null;
        try {
            String requestUrl = buildRequestUrl(url);
            HttpResult httpResult = retriever.httpGet(requestUrl);
            String response = httpResult.getStringContent();

            if (response != null) {
                googlePlusLikes = 0;
                // result stays 0 if response empty -> url not found
                String googleLikes = StringHelper.getSubstringBetween(response, "__SSR = {c: ", " ,").trim();

                googleLikes = googleLikes.replaceAll("\\..*", "");

                if (!googleLikes.isEmpty()) {
                    googlePlusLikes = Integer.valueOf(googleLikes);
                }

                LOGGER.trace("Google Plus Likes for " + url + " : " + googlePlusLikes);
            }

        } catch (Exception e) {
            throw new RankingServiceException("Exception " + e.getMessage(), e);
        }
        return builder.add(LIKES, googlePlusLikes).create();
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
        return "https://plusone.google.com/u/0/_/+1/fastbutton?url=" + UrlHelper.encodeParameter(url);
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
        GooglePlusLikes gpl = new GooglePlusLikes();
        Ranking ranking = null;
        ranking = gpl.getRanking("http://facebook.com");
        System.out.println(ranking);

        ranking = gpl.getRanking("http://www.cinefreaks.com/news/704/Sex-Beherrscht-den-April-im-Kino%3A-Die-Beste-Filme-zum-Fr%C3%BChling");
        System.out.println(ranking);

        ranking = gpl.getRanking("http://google.com");
        System.out.println(ranking);
    }

}
