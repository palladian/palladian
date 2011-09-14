package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * RankingService implementation sharing statistics gathered from sharethis.com<br/>
 * http://www.sharethis.com/
 * <br/><br/>
 * Total value is counted, this includes also services that may already be in Rankify!<br/>
 * That's why commitment is 0.5<br/>
 * http://help.sharethis.com/api/sharing-api#social-destinations
 * <br/><br/>
 * Limit at 150 requests/hour, whitelisting possible
 * <br/><br/>
 * TODO also use inbound value? (users that clicked on the shared link)
 * 
 * @author Julien Schmehl
 *
 */
public class SharethisStats implements RankingService{

	/** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(SharethisStats.class);
      
    /** The config values. */
    private String apiKey;
    private String secret;
    
    /** Crawler for downloading purposes. */
    private DocumentRetriever crawler = new DocumentRetriever();
    
    /** The id of this service. */
    private static final String SERVICE_ID = "sharethis";
    
    /** The ranking value types of this service **/
    /** 
     * The number of shares via multiple services measured on sharethis.com.
     * Commitment value is 2.9046
     * Max. Ranking value is 30
     */
    static RankingType SHARES = new RankingType("sharethis_stats", "ShareThis stats", "The number of shares via  " +
    		"multiple services measured on sharethis.com.", 2.9046f, 30);

    /** The topic weighting coefficients for this service **/
    @SuppressWarnings("serial")
  	private static Map<String, Float> topicWeighting = new HashMap<String, Float>() {
        {
            put("business", 3.1186f);
            put("politics", 3.0604f);
            put("entertainment", 1.3234f);
            put("lifestyle", 1.2927f);
            put("sports", 3.2678f);
            put("technology", 0.9955f);
            put("science", 2.0863f);
        }
    };

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000*60*60;
    
	public SharethisStats() {
		PropertiesConfiguration configuration = ConfigHolder.getInstance().getConfig();

        if (configuration != null) {
        	setApiKey(configuration.getString("api.sharethis.key"));
        	setSecret(configuration.getString("api.sharethis.secret"));
        } else {
        	LOGGER.warn("could not load configuration, ranking retrieval won't work");
        }
        
        // we use a rather short timeout here, as responses are short.
        crawler.setConnectionTimeout(5000);
	}

	@Override
	public Ranking getRanking(String url) {
		Map<RankingType, Float> results = new HashMap<RankingType, Float>();
		Ranking ranking = new Ranking(this, url);
		if(isBlocked()) return ranking;
		
		try {
	        String encUrl = StringHelper.urlEncode(url);
	        JSONObject json = crawler.getJSONDocument("http://rest.sharethis.com/reach/getUrlInfo.php?pub_key="+getApiKey()+"&access_key="+getSecret()+"&url="+encUrl);
	        ranking.setRetrieved(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
            if (json != null) {
	        	int total = json.getJSONObject("total").getInt("outbound");
	        	results.put(SHARES, (float) total);
	            LOGGER.trace("ShareThis stats for " + url + " : " + total);
	        } else {
            	results.put(SHARES, null);
            	LOGGER.trace("ShareThis stats for " + url + "could not be fetched");
                checkBlocked();
            }
        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
            checkBlocked();
        }

        ranking.setValues(results);
        return ranking;
	}
	
	@Override
	public Map<String, Ranking> getRanking(List<String> urls) {
		Map<String, Ranking> results = new HashMap<String, Ranking>();
		if(isBlocked()) return results;
		 
		// iterate through urls and get ranking for each
		for(String u:urls) results.put(u, getRanking(u));

        return results;
        
	}


	/**
	 * Force a new check if this service is blocked due to excess
	 * of request limits. This updates the blocked-attribute
	 * of this service.
	 * 
	 * @return True if the service is momentarily blocked, false otherwise
	 */
	public boolean checkBlocked() {
		boolean error = false;
		try {
			JSONObject json = crawler.getJSONDocument("http://rest.sharethis.com/reach/getUrlInfo.php?pub_key="+getApiKey()+"&access_key="+getSecret()+"&url=http://www.google.com/");
			if(json.has("statusMessage"))
				if(json.get("statusMessage").equals("LIMIT_REACHED")) error = true;
		} catch (JSONException e) {
			LOGGER.error("JSONException " + e.getMessage());
		}
		if(!error) {
			blocked = false;
			lastCheckBlocked = new Date().getTime();
			return false;
		}
		blocked = true;
		lastCheckBlocked = new Date().getTime();
		LOGGER.error("ShareThis Ranking Service is momentarily blocked. Will check again in 1h. Try resetting your IP-Address.");
		return true;
	}
	/**
	 * Returns if this service is momentarily blocked or not.
	 * 
	 * @return True if the service is momentarily blocked, false otherwise
	 */
	public boolean isBlocked() {
		if(new Date().getTime()-lastCheckBlocked < checkBlockedIntervall) return blocked;
		else return checkBlocked();
	}
	/**
	 * Sets this service blocked status to unblocked and resets the
	 * time of the last check to now.
	 * 
	 * @return True if reset was successful, false otherwise
	 */
	public boolean resetBlocked() {
		blocked = false;
		lastCheckBlocked = new Date().getTime();
		return true;
	}
	/**
	 * Get the id of this ranking service.
	 * 
	 * @return The id-string of this service
	 */
	public String getServiceId() {
		return SERVICE_ID;
	}
	/**
	 * Get all ranking types of this ranking service.
	 * 
	 * @return A list of ranking types
	 */
	public List<RankingType> getRankingTypes() {
		ArrayList<RankingType> types = new ArrayList<RankingType>();
		types.add(SHARES);
		return types;
	}
	/**
	 * Get the ranking type for this id.
	 * 
	 * @return The ranking type for the given id
	 */
	public RankingType getRankingType(String id) {
		if(id.equals(SHARES.getId())) return SHARES;
		return null;
	}
	/**
	 * Retrieve this service topic weighting coefficient
	 * for a given topic
	 * 
	 * @return Weighting coefficient if topic is known, 1 otherwise
	 */
	public float getTopicWeighting(String topic) {
		if(topicWeighting.containsKey(topic)) return topicWeighting.get(topic);
		else return 1.0f;
	}
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	public String getApiKey() {
		return apiKey;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}
	public String getSecret() {
		return secret;
	}


}
