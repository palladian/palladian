package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * RankingService implementation to count entries containing a given url, aggregated
 * on Friendfeed, excluding internal posts and services that have their own 
 * RankingService class.<br/>
 * http://www.friendfeed.com/
 * <br/><br/>
 * Entries for services already in Rankify (e.g. Twitter,...) are not counted, we should
 * exclude other services if they get their own RankingService implementation<br/>
 * http://friendfeed.com/api/services
 * <br/><br/>
 * No specifics on rate limiting
 * 
 * 
 * @author Julien Schmehl
 *
 */
public class FriendfeedAggregatedStats implements RankingService{

	/** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(FriendfeedAggregatedStats.class);
    
    private static final String GET_ENTRIES = "http://friendfeed.com/api/feed/url?url=";
    
    /** 
     * The external services users can have in their feed that we don't want to
     * count since we have seperate RankingService classes for them. 
     * */
    private static final String[] EXCLUDE_SERVICES = {"internal","feed","blog","delicious","digg","facebook","plurk","reddit","twitter"};
    
	/** No config values. */
    
    /** Crawler for downloading purposes. */
    private DocumentRetriever crawler = new DocumentRetriever();
    
    /** The id of this service. */
    private static final String SERVICE_ID = "friendfeed_external";
    
    /** The ranking value types of this service **/
    /** 
     * The number of entries from varying services containing the given url on FriendFeed.
     * Commitment value is 0.9
     * Max. Ranking value is 10
     */
    static RankingType ENTRIES = new RankingType("friendfeed_ext_entries", "FriendFeed entries for external services", "The number of entries from " +
    		"varying services containing the given url on FriendFeed.", 0.9f, 10);
    /** 
     * The number of likes on entries from varying services containing the given url on FriendFeed.
     * Commitment value is 0.6
     * Max. Ranking value is 10
     */
    static RankingType LIKES = new RankingType("friendfeed_ext_likes", "FriendFeed likes for external services", "The number of likes on " +
    		"entries from varying services containing the given url on FriendFeed.", 0.6f, 10);
    /** 
     * The number of comments on entries from varying services containing the given url on FriendFeed.
     * Commitment value is 1.0
     * Max. Ranking value is 10
     */
    static RankingType COMMENTS = new RankingType("friendfeed_ext_comments", "FriendFeed comments for external services", "The number of comments on " +
    		"entries from varying services containing the given url on FriendFeed.", 1.0f, 10);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000*60*1;
    
	public FriendfeedAggregatedStats() {
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
	        JSONObject json = crawler.getJSONDocument(GET_ENTRIES + encUrl);
	        ranking.setRetrieved(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
            if (json != null) {
	        	JSONArray entriesArray = json.getJSONArray("entries");
	        	int entries = 0;
	        	int likes = 0;
	        	int comments = 0;
	        	for(int i=0; i < entriesArray.length(); i++){
	        		JSONObject post = entriesArray.getJSONObject(i);
	        		if(!Arrays.asList(EXCLUDE_SERVICES).contains(post.getJSONObject("service").getString("id"))){
	        			entries++;
	        			likes += post.getJSONArray("likes").length();
	        			comments += post.getJSONArray("comments").length();
	        		}
	        	}
	        	results.put(ENTRIES, (float) entries);
	        	results.put(LIKES, (float) likes);
	        	results.put(COMMENTS, (float) comments);
	            LOGGER.trace("FriendFeed stats for " + url + " : " + results);
	        } else {
            	results.put(ENTRIES, null);
            	results.put(LIKES, null);
            	results.put(COMMENTS, null);
            	LOGGER.trace("FriendFeed stats for " + url + "could not be fetched");
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
			JSONObject json = crawler.getJSONDocument(GET_ENTRIES + "http://www.google.com/");
			if(json.has("errorCode"))
				if(json.get("errorCode").equals("limit-exceeded")) error = true;
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
		LOGGER.error("FriendFeed Aggregated Stats Ranking Service is momentarily blocked. Will check again in 1min.");
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
		types.add(ENTRIES);
		types.add(LIKES);
		types.add(COMMENTS);
		return types;
	}
	/**
	 * Get the ranking type for this id.
	 * 
	 * @return The ranking type for the given id
	 */
	public RankingType getRankingType(String id) {
		if(id.equals("friendfeed_ext_entries")) return ENTRIES;
		else if(id.equals("friendfeed_ext_likes")) return LIKES;
		else if(id.equals("friendfeed_ext_comments")) return COMMENTS;
		return null;
	}

}
