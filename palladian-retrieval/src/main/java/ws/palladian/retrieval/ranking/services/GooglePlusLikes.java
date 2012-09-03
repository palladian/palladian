package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation to find the number of Google Plus likes of a Web page.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public final class GooglePlusLikes extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(GooglePlusLikes.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "googleplus";

    /** The ranking value types of this service **/
    public static final RankingType LIKES = new RankingType("googlepluslikes", "Google Plus Likes",
            "The Number of Likes on Google Plus");
    
    /** All available ranking types by {@link GooglePlusLikes}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(LIKES);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static long checkBlockedIntervall = TimeUnit.MINUTES.toMillis(1);

    @Override
    public Ranking getRanking(String url) {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);
        if (isBlocked()) {
            return ranking;
        }

        Integer googlePlusLikes = null;
        try {
            String requestUrl = buildRequestUrl(url);
            HttpResult httpResult = retriever.httpGet(requestUrl);
            String response = HttpHelper.getStringContent(httpResult);

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
            LOGGER.error("Exception " + e.getMessage());
            checkBlocked();
        }
        results.put(LIKES, (float)googlePlusLikes);
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
        String requestUrl = "https://plusone.google.com/u/0/_/+1/fastbutton?url=" + UrlHelper.urlEncode(url);
        return requestUrl;
    }

    @Override
    public boolean checkBlocked() {
        int status = -1;
        try {
            String requestUrl = buildRequestUrl("http://www.google.com");
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
        LOGGER.error("Google Plus Likes Ranking Service is momentarily blocked. Will check again in "
                + checkBlockedIntervall + " minute(s). Try changing your IP-address.");
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

    public static void main(String[] a) {
        GooglePlusLikes gpl = new GooglePlusLikes();
        Ranking ranking = null;
        ranking = gpl.getRanking("http://facebook.com");
        System.out.println(ranking);

        ranking = gpl
                .getRanking("http://www.cinefreaks.com/news/704/Sex-Beherrscht-den-April-im-Kino%3A-Die-Beste-Filme-zum-Fr%C3%BChling");
        System.out.println(ranking);
        
        ranking = gpl.getRanking("http://google.com");
        System.out.println(ranking);
    }

}
