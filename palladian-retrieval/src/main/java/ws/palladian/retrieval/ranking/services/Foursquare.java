package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.Validate;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * {@link RankingService} implementation to find the number of checkins and likes for a location on <a
 * href="https://foursquare.com>foursquare</a>. <b>Important:</b> This does not conform to the other ranking
 * implementations as it does not reveive URLs, but vanue IDs as parameter.
 * </p>
 * 
 * @author David Urbansky
 * @see <a href="https://developer.foursquare.com">foursquare for Developers</a>
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
     * Create a new {@link Foursquare} ranking service.
     * </p>
     * 
     * @param configuration The configuration which must provide {@value #CONFIG_CLIENT_ID} and
     *            {@value #CONFIG_CLIENT_SECRET} for accessing the service.
     */
    public Foursquare(Configuration configuration) {
        this(configuration.getString(CONFIG_CLIENT_ID), configuration.getString(CONFIG_CLIENT_SECRET));
    }

    /**
     * <p>
     * Create a new {@link Foursquare} ranking service.
     * </p>
     * 
     * @param clientId The required client key for accessing the service, not <code>null</code> or empty.
     * @param clientSecret The required client secret for accessing the service, not <code>null</code> or empty.
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

        double checkins = 0.;
        double likes = 0.;
        String requestUrl = buildRequestUrl(venueId);

        try {

            HttpResult httpGet = retriever.httpGet(requestUrl);
            JsonObject json = new JsonObject(httpGet.getStringContent());

            JsonObject venue = json.queryJsonObject("response/venue");
            checkins = venue.queryDouble("stats/checkinsCount");
            likes = venue.queryDouble("likes/count");

        } catch (Exception e) {
            throw new RankingServiceException(e);
        }

        results.put(FOURSQUARE_CHECKINS, (float)checkins);
        results.put(FOURSQUARE_LIKES, (float)likes);
        return new Ranking(this, venueId, results);
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
        return String.format("https://api.foursquare.com/v2/venues/%s?v=20120321&client_id=%s&client_secret=%s",
                venueId, clientId, clientSecret);
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
