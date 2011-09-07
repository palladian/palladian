package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.json.JSONArray;
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
 * RankingService implementation to get the number of posts containing 
 * a given url on plurk.com.<br/>
 * http://www.plurk.com - microblogging service
 * <br/><br/>
 * Does fulltext search, e.g. it finds also posts that have 
 * parts of the url - only usable for longer urls
 * <br/><br/>
 * Current limit is 50.000 calls pr. day
 * <br/><br/>
 * TODO implement follow up request if has_more:true
 * 
 * @author Julien Schmehl
 *
 */
public class PlurkPosts implements RankingService {

	/** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(PlurkPosts.class);
    
    /** The config values. */
    private String apiKey;
    
    /** Crawler for downloading purposes. */
    private DocumentRetriever crawler = new DocumentRetriever();
    
    /** The id of this service. */
    private static final String SERVICE_ID = "plurk";
    
    /** The ranking value types of this service **/
    /** 
     * The number of bookmarks users have created for this url.
     * Commitment value is 0.9
     * Max. Ranking value is 30
     */
    static RankingType POSTS = new RankingType("plurk_posts", "Plurk.com posts", "The number of " +
    		"posts on plurk.com mentioning this url.", 0.9f, 30);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000*60*1;
    
	public PlurkPosts() {

		PropertiesConfiguration configuration = ConfigHolder.getInstance().getConfig();

        if (configuration != null) {
        	setApiKey(configuration.getString("api.plurk.key"));
        } else {
        	LOGGER.warn("could not load configuration, ranking retrieval won't work");
        }
        
		// we use a rather short timeout here, as responses are short.
        crawler.setConnectionTimeout(5000);
        
	}

	
	public Ranking getRanking(String url) {
		Map<RankingType, Float> results = new HashMap<RankingType, Float>();
		Ranking ranking = new Ranking(this, url);
		if(isBlocked()) return ranking;

        try {

        	String encUrl = StringHelper.urlEncode(url);
            JSONObject json = crawler.getJSONDocument("http://www.plurk.com/API/PlurkSearch/search?api_key="+ getApiKey() +"&query=" + encUrl);
            ranking.setRetrieved(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
            
            if (json != null) {
                JSONArray plurks = json.getJSONArray("plurks");
                int result = plurks.length();
                results.put(POSTS, (float) result);
                LOGGER.trace("Plurk.com posts for " + url + " : " + result);
            } else {
            	results.put(POSTS, null);
            	LOGGER.trace("Plurk.com posts for " + url + "could not be fetched");
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
		int status = -1;
		try {
	        status = crawler.httpGet("http://www.plurk.com/API/PlurkSearch/search?api_key="+ getApiKey() +"&query=http://www.google.com/").getStatusCode();
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
		LOGGER.error("Plurk Ranking Service is momentarily blocked. Will check again in 1min.");
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
		types.add(POSTS);
		return types;
	}
	/**
	 * Returns POSTS.
	 * 
	 * @return The ranking type POSTS
	 */
	public RankingType getRankingType(String id) {
		return POSTS;
	}
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getApiKey() {
		return apiKey;
	}
	
}
