package ws.palladian.retrieval.ranking.services;

import java.sql.Timestamp;
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
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * RankingService implementation for shares of a given url on Google Buzz.<br/>
 * http://www.google.com/buzz
 * <br/><br/>
 * Courtesy limit: 1,000,000 requests/day & 100,000 requests/second/user
 * 
 * @author Julien Schmehl
 * @author Philipp Katz
 *
 */
public class GoogleBuzzShares implements RankingService{

	/** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(GoogleBuzzShares.class);
    
	/** The config values. */
    private String apiKey;
    
    /** Crawler for downloading purposes. */
    private DocumentRetriever crawler = new DocumentRetriever();
    
    /** The id of this service. */
    private static final String SERVICE_ID = "buzz";
    
    /** The ranking value types of this service **/
    /** 
     * The number of times users have shared the page on Google Buzz.
     * Commitment value is 1.0840
     * Max. Ranking value is 47
     */
    static RankingType SHARES = new RankingType("buzz_shares", "Google Buzz Shares", "The number of times users have " +
    		"shared the page on Google Buzz", 1.0840f, 47, new int[]{0,1,1,2,3,5,9,18,47});

    /** The topic weighting coefficients for this service **/
    @SuppressWarnings("serial")
  	private static Map<String, Float> topicWeighting = new HashMap<String, Float>() {
        {
            put("business", 0.8877f);
            put("politics", 1.3110f);
            put("entertainment", 1.1418f);
            put("lifestyle", 1.0032f);
            put("sports", 0.9684f);
            put("technology", 0.8863f);
            put("science", 1.1542f);
        }
    };

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000*60*1;
    
	public GoogleBuzzShares() {
		
		PropertiesConfiguration configuration = ConfigHolder.getInstance().getConfig();

        if (configuration != null) {
        	setApiKey(configuration.getString("api.google.buzz.key"));
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
	        JSONObject json = crawler.getJSONDocument("https://www.googleapis.com/buzz/v1/activities/count?key="+getApiKey()+"&alt=json&url="+ encUrl);
	        ranking.setRetrieved(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
            if (json != null) {
	        	int result = json.getJSONObject("data").getJSONObject("counts").getJSONArray(url).getJSONObject(0).getInt("count");
	        	results.put(SHARES, SHARES.normalize(result));
	            LOGGER.trace("Google Buzz shares for " + url + " : " + result);
	        } else {
            	results.put(SHARES, null);
            	LOGGER.trace("Google Buzz shares for " + url + "could not be fetched");
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
		
		// iterate through urls in batches of 10, since this is the maximum number we
		// can send to bit.ly at once
		for(int index=0; index<urls.size()/10+(urls.size()%10>0?1:0); index++) {
			
			List<String> subUrls = urls.subList(index*10, Math.min(index*10+10, urls.size()));
			String encUrls = "";
	
			try {
		    	for(int i=0; i<subUrls.size(); i++){
		    		encUrls += "&url="+StringHelper.urlEncode(subUrls.get(i));
		    	}
		        
		    	JSONObject json = crawler.getJSONDocument("https://www.googleapis.com/buzz/v1/activities/count?key="+getApiKey()+"&alt=json"+ encUrls);
		    	Timestamp retrieved = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
		    	if (json != null) {
		        	
		        	float count = -1;
		
		        	for(String u : subUrls){
		        		count = json.getJSONObject("data").getJSONObject("counts").getJSONArray(u).getJSONObject(0).getInt("count");
		        		Map<RankingType, Float> result = new HashMap<RankingType, Float>();
		            	result.put(SHARES, SHARES.normalize(count));
		        		results.put(u, new Ranking(this, u, result, retrieved));
		            	LOGGER.trace("Google Buzz shares for " + u + " : " + count);
		            }
		        } else {
		        	for(String u:subUrls) {
		        		Map<RankingType, Float> result = new HashMap<RankingType, Float>();
		            	result.put(SHARES, null);
		        		results.put(u, new Ranking(this, u, result, retrieved));
		        	}
	            	LOGGER.trace("Google Buzz shares for " + subUrls + "could not be fetched");
	                checkBlocked();
	            }
			} catch (JSONException e) {
	            LOGGER.error("JSONException " + e.getMessage());
	            checkBlocked();
	        }
		}
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
		int status = -1;
		try {
	        status = crawler.httpGet("https://www.googleapis.com/buzz/v1/activities/count?key="+getApiKey()+"&alt=json&url=http://www.google.com/").getStatusCode();
		} catch (HttpException e) {
			LOGGER.error("HttpException " + e.getMessage());
		}
		if(status == 200) {
			blocked = false;
			lastCheckBlocked = new Date().getTime();
			return false;
		}
		blocked = true;
		lastCheckBlocked = new Date().getTime();
		LOGGER.error("Google Buzz Ranking Service is momentarily blocked. Will check again in 1min. Try changing your IP-address.");
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
	 * Returns SHARES.
	 * 
	 * @return The ranking type SHARES
	 */
	public RankingType getRankingType(String id) {
		return SHARES;
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

}
