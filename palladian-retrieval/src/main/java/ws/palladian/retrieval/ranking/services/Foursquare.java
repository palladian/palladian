package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.Validate;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.JsonObjectWrapper;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation to find the number of checkins and likes for a location on foursquare.
 * </p>
 * 
 * @author David Urbansky
 */
public final class Foursquare extends BaseRankingService implements RankingService {

    /** {@link Configuration} key for the client id. */
    public static final String CONFIG_CLIENT_ID = "api.foursquare.clientId";

    /** {@link Configuration} key for the client secret. */
    public static final String CONFIG_CLIENT_SECRET = "api.foursquare.clientSecret";

    /** The id of this service. */
    private static final String SERVICE_ID = "foursquare";

    /** The ranking value types of this service **/
    public static final RankingType FOURSQUARE_CHECKINS = new RankingType("checkins", "Foursquare Checkins",
            "The number of foursquare checkins of the location.");
    
    public static final RankingType FOURSQUARE_LIKES = new RankingType("likes", "Foursquare Likes",
            "The number of foursquare likes of the location.");

    /** All available ranking types by {@link Foursquare}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(FOURSQUARE_CHECKINS, FOURSQUARE_LIKES);

    private final String clientId;
    private final String clientSecret;

    /**
     * <p>
     * Create a new {@link MajesticSeo} ranking service.
     * </p>
     * 
     * @param configuration The configuration which must provide an API key (<tt>api.majestic.key</tt>) for accessing
     *            the
     *            service.
     */
    public Foursquare(Configuration configuration) {
        this(configuration.getString(CONFIG_CLIENT_ID), configuration.getString(CONFIG_CLIENT_SECRET));
    }

    /**
     * <p>
     * Create a new {@link MajesticSeo} ranking service.
     * </p>
     * 
     * @param clientId The required API key for accessing the service.
     */
    public Foursquare(String clientId, String clientSecret) {
        Validate.notEmpty(clientId);
        Validate.notEmpty(clientSecret);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }
    
    @Override
    public Ranking getRanking(String venueId) throws RankingServiceException {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, venueId, results);

        double checkins = 0.;
        double likes = 0.;
        String requestUrl = buildRequestUrl(venueId);

        try {

            HttpResult httpGet = retriever.httpGet(requestUrl);
            JsonObjectWrapper json = new JsonObjectWrapper(new String(httpGet.getContent()));

            JsonObjectWrapper venue = json.getJSONObject("response").getJSONObject("venue");
            checkins = venue.getJSONObject("stats").getDouble("checkinsCount");
            likes = venue.getJSONObject("likes").getDouble("count");
            

        } catch (Exception e) {
            throw new RankingServiceException(e);
        }

        results.put(FOURSQUARE_CHECKINS, (float)checkins);
        results.put(FOURSQUARE_LIKES, (float)likes);
        return ranking;
    }

    /**
     * <p>
     * Build the request URL.
     * </p>
     * 
     * @param venueId The id of the venue to search for.
     * @return The request URL.
     */
    private String buildRequestUrl(String venueId) {
        String requestUrl = "https://api.foursquare.com/v2/venues/";
        requestUrl += venueId;
        requestUrl += "?v=20120321";
        requestUrl += "&client_id=" + clientId;
        requestUrl += "&client_secret=" + clientSecret;
        
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

    public static void main(String[] a) throws RankingServiceException {
        Foursquare gpl = new Foursquare(ConfigHolder.getInstance().getConfig());
        Ranking ranking = null;

        ranking = gpl.getRanking("4d5314d8169bcbff48131cf9");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(Foursquare.FOURSQUARE_CHECKINS) + " -> Foursquare checkins");
        System.out.println(ranking.getValues().get(Foursquare.FOURSQUARE_LIKES) + " -> Foursquare likes");
    }

}
