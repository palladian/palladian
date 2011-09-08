package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
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
 * RankingService implementation for posts containing a given url on FriendFeed.<br/>
 * http://www.friendfeed.com/
 * <br/><br/>
 * Only entries with the service id "internal" are not counted, these are FriendFeed posts<br/>
 * http://friendfeed.com/api/services
 * <br/><br/>
 * No specifics on rate limiting
 * 
 * @author Julien Schmehl
 *
 */
public class FriendfeedStats implements RankingService{

	/** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(FriendfeedStats.class);
    
    private static final String GET_ENTRIES = "http://friendfeed.com/api/feed/url?url=";
    
	/** No config values. */
    
    /** Crawler for downloading purposes. */
    private DocumentRetriever crawler = new DocumentRetriever();
    
    /** The id of this service. */
    private static final String SERVICE_ID = "friendfeed";
    
    /** The ranking value types of this service **/
    /** 
     * The number of entries posted on FriendFeed containing the given url.
     * Commitment value is 0.9
     * Max. Ranking value is 10
     */
    static RankingType POSTS = new RankingType("friendfeed_int_posts", "FriendFeed posts", "The number of entries posted " +
    		"on FriendFeed containing the given url.", 0.9f, 10);
    /** 
     * The number of likes for entries posted on FriendFeed containing the given url.
     * Commitment value is 0.6
     * Max. Ranking value is 20
     */
    static RankingType LIKES = new RankingType("friendfeed_int_likes", "FriendFeed likes", "The number of likes for entries " +
    		"posted on FriendFeed containing the given url.", 0.6f, 20);
    /** 
     * The number of comments for entries posted on FriendFeed containing the given url.
     * Commitment value is 1.0
     * Max. Ranking value is 20
     */
    static RankingType COMMENTS = new RankingType("friendfeed_int_comments", "FriendFeed comments", "The number of comments for " +
    		"entries posted on FriendFeed containing the given url.", 1.0f, 20);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000*60*1;
    
	public FriendfeedStats() {
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
	        	JSONArray entries = json.getJSONArray("entries");
	        	int posts = 0;
	        	int likes = 0;
	        	int comments = 0;
	        	for(int i=0; i < entries.length(); i++){
	        		JSONObject post = entries.getJSONObject(i);
	        		if(post.getJSONObject("service").getString("id").equals("internal")){
	        			posts++;
	        			likes += post.getJSONArray("likes").length();
	        			comments += post.getJSONArray("comments").length();
	        		}
	        	}
	        	results.put(POSTS, (float) posts);
	        	results.put(LIKES, (float) likes);
	        	results.put(COMMENTS, (float) comments);
	            LOGGER.trace("FriendFeed stats for " + url + " : " + results);
	        } else {
            	results.put(POSTS, null);
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
			if(json != null)
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
		LOGGER.error("FriendFeed Ranking Service is momentarily blocked. Will check again in 1min.");
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
		if(id.equals(POSTS.getId())) return POSTS;
		else if(id.equals(LIKES.getId())) return LIKES;
		else if(id.equals(COMMENTS.getId())) return COMMENTS;
		return null;
	}

}
