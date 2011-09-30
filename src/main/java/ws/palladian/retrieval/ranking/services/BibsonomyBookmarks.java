package ws.palladian.retrieval.ranking.services;

import java.io.IOException;
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
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * RankingService implementation to get the number of bookmarks of 
 * a given url on BibSonomy.<br/>
 * http://www.bibsonomy.org - academic bookmark and publication service
 * <br/><br/>
 * At the moment it returns number for all bookmarks containing the url
 * or a longer version - e.g. www.google.com will give number for all
 * bookmarks containing www.google.com/...
 * <br/><br/>
 * No information about request limits
 * 
 * @author Julien Schmehl
 * @author Philipp Katz
 *
 */
public class BibsonomyBookmarks implements RankingService{

	/** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(BibsonomyBookmarks.class);
    
	/** The config values. */
    private String login;
    private String apiKey;
    
    /** Crawler for downloading purposes. */
    private DocumentRetriever crawler = new DocumentRetriever();
    
    /** The id of this service. */
    private static final String SERVICE_ID = "bibsonomy";
    
    /** The ranking value types of this service **/
    /** 
     * The number of bookmarks users have created for this url.
     * Commitment value is 1.0
     * Max. Ranking value is 2
     */
    private static RankingType BOOKMARKS = new RankingType("bibsonomy_bookmarks", "Bibsonomy Bookmarks", "The number of " +
    		"bookmarks users have created for this url.", 1.0f, 2, new int[]{0,0,0,0,1,1,1,1,2});
    
    /** The topic weighting coefficients for this service **/
    @SuppressWarnings("serial")
	private static Map<String, Float> topicWeighting = new HashMap<String, Float>() {
        {
            put("business", 1.0f);
            put("politics", 1.0f);
            put("entertainment", 1.0f);
            put("lifestyle", 1.0f);
            put("sports", 1.0f);
            put("technology", 1.0f);
            put("science", 1.0f);
        }
    };
    
    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000*60*1;
    
	public BibsonomyBookmarks() {
		
		PropertiesConfiguration configuration = ConfigHolder.getInstance().getConfig();

        if (configuration != null) {
        	setLogin(configuration.getString("api.bibsonomy.login"));
        	setApiKey(configuration.getString("api.bibsonomy.key"));
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
        	// authenticate via HTTP Auth and send GET request
        	String pass = getLogin()+":"+getApiKey();
        	Map<String, String> headerParams = new HashMap<String, String>();
            headerParams.put("Authorization", "Basic " + StringHelper.encodeBase64(pass));
            HttpResult getResult = crawler.httpGet("http://www.bibsonomy.org/api/posts?format=json&resourcetype=bookmark&start=0&end=999999&search="+ encUrl, headerParams);
            String response = new String(getResult.getContent());
            ranking.setRetrieved(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
            
            // create JSON-Object from response
            JSONObject json = null;
            if (response.length() > 0) json = new JSONObject(response);
        	
            if (json != null) {
            	int result = json.getJSONObject("posts").getInt("end");
            	results.put(BOOKMARKS, BOOKMARKS.normalize(result));
                LOGGER.trace("Bibsonomy bookmarks for " + url + " : " + result);
            } else {
            	results.put(BOOKMARKS, null);
            	LOGGER.trace("Bibsonomy bookmarks for " + url + "could not be fetched");
            }

        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
            checkBlocked();
        } catch (IOException e) {
			LOGGER.error("IOException " + e.getMessage());
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
		int status = -1;
		try {
			// authenticate via HTTP Auth and send GET request
	    	String pass = getLogin()+":"+getApiKey();
	    	Map<String, String> headerParams = new HashMap<String, String>();
	        headerParams.put("Authorization", "Basic " + StringHelper.encodeBase64(pass));
	        HttpResult getResult;
			getResult = crawler.httpGet("http://www.bibsonomy.org/api/posts?format=json&resourcetype=bookmark&start=0&end=999999&search=http://www.google.com/", headerParams);
			status = getResult.getStatusCode();
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
		LOGGER.error("Bibsonomy Ranking Service is momentarily blocked. Will check again in 1min.");
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
		types.add(BOOKMARKS);
		return types;
	}
	/**
	 * Returns BOOKMARKS.
	 * 
	 * @return The ranking type BOOKMARKS
	 */
	public RankingType getRankingType(String id) {
		return BOOKMARKS;
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
	public void setLogin(String login) {
		this.login = login;
	}
	public String getLogin() {
		return login;
	}
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	public String getApiKey() {
		return apiKey;
	}

}
