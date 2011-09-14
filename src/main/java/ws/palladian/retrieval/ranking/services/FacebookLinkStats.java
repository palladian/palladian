package ws.palladian.retrieval.ranking.services;

import java.sql.Timestamp;
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
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * RankingService implementation for likes, shares and comments on Facebook.
 * 
 * @author Julien Schmehl
 * @author Philipp Katz
 *
 */
public class FacebookLinkStats implements RankingService {
	
	/** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(FacebookLinkStats.class);
    
    private static final String FQL_QUERY = "https://api.facebook.com/method/fql.query?format=json&query=select+total_count,like_count,comment_count,share_count+from+link_stat+where+";
    
    /** No config values. */
    
    /** Crawler for downloading purposes. */
    private DocumentRetriever crawler = new DocumentRetriever();
    
    /** The id of this service. */
    private static final String SERVICE_ID = "facebook";
    
    /** The ranking value types of this service **/
    /** 
     * The number of times Facebook users have "Liked" the page, or liked any comments or re-shares of this page.
     * Commitment value is 0.6
     * Max. Ranking value is 120
     */
    static RankingType LIKES = new RankingType("facebook_likes", "Facebook Likes", "The number of times Facebook users " +
    		"have \"Liked\" the page, or liked any comments or re-shares of this page.", 0.6f, 120);
    /** 
     * The number of times users have shared the page on Facebook.
     * Commitment value is 0.7
     * Max. Ranking value is 130
     */
    static RankingType SHARES = new RankingType("facebook_shares", "Facebook Shares", "The number of times users have " +
    		"shared the page on Facebook.", 0.7f, 130);
    /** 
     * The number of comments users have made on the shared story.
     * Commitment value is 1.0
     * Max. Ranking value is 150
     */
    static RankingType COMMENTS = new RankingType("facebook_comments", "Facebook Comments", "The number of comments users " +
    		"have made on the shared story.", 1.0f, 150);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000*60*1;
    
	public FacebookLinkStats() {
        // we use a rather short timeout here, as responses are short.
        crawler.setConnectionTimeout(5000);
	}

	
	public Ranking getRanking(String url) {
		
		Map<RankingType, Float> results = new HashMap<RankingType, Float>();
		Ranking ranking = new Ranking(this, url);
		if(isBlocked()) return ranking;
		
        try {

        	String encUrl = StringHelper.urlEncode(url);
            JSONObject json = crawler.getJSONDocument(FQL_QUERY+"url='"+encUrl+"'");
            ranking.setRetrieved(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
            
            if (json != null) {
            	results.put(LIKES, (float) json.getInt("like_count"));
            	results.put(SHARES, (float) json.getInt("share_count"));
            	results.put(COMMENTS, (float) json.getInt("comment_count"));
            	LOGGER.trace("Facebook link stats for " + url + " : " + results);
            } else {
            	results.put(LIKES, null);
            	results.put(SHARES, null);
            	results.put(COMMENTS, null);
            	LOGGER.trace("Facebook link stats for " + url + "could not be fetched");
                checkBlocked();
            }

        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
            checkBlocked();
        }

        ranking.setValues(results);
        return ranking;
	}

	
	public Map<String, Ranking> getRanking(List<String> urls) {
		
		Map<String, Ranking> results = new HashMap<String, Ranking>();
		if(isBlocked()) return results;
		String encUrls = "";

		try {
			
	    	for(int i=0; i<urls.size(); i++){
	    		if(i == urls.size()-1) encUrls += "url='"+StringHelper.urlEncode(urls.get(i))+"'";
	    		else encUrls += "url='"+StringHelper.urlEncode(urls.get(i))+"' or ";
	    	}
	    	
	    	HashMap<String, String> postData = new HashMap<String, String>();
	    	postData.put("format", "json");
	    	postData.put("query", "select total_count,like_count,comment_count,share_count from link_stat where "+encUrls);

	    	HttpResult response = crawler.httpPost("https://api.facebook.com/method/fql.query", postData);
	    	String content = new String(response.getContent());
	    	JSONArray json = null;
            if (content.length() > 0) {
                try {
                    json = new JSONArray(content);
                } catch (JSONException e) {
                    LOGGER.error("JSONException: " + e.getMessage());
                }
            }
	    	
	    	Timestamp retrieved = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
            
	        if (json != null) {
	        	
	        	float likeCount = -1;
	        	float shareCount = -1;
	        	float commentCount = -1;
	
	        	for(int i=0; i<urls.size(); i++){
	        		likeCount = json.getJSONObject(i).getInt("like_count");
	        		shareCount = json.getJSONObject(i).getInt("share_count");
	        		commentCount = json.getJSONObject(i).getInt("comment_count");
	        		Map<RankingType, Float> result = new HashMap<RankingType, Float>();
	            	result.put(LIKES, likeCount);
	            	result.put(SHARES, shareCount);
	            	result.put(COMMENTS, commentCount);
	        		results.put(urls.get(i), new Ranking(this, urls.get(i), result, retrieved));
	            	LOGGER.trace("Facebook link stats for " + urls.get(i) + " : " + result);
	            }
	        } else {
	        	for(String u:urls) {
	        		Map<RankingType, Float> result = new HashMap<RankingType, Float>();
	            	result.put(LIKES, null);
	            	result.put(SHARES, null);
	            	result.put(COMMENTS, null);
	        		results.put(u, new Ranking(this, u, result, retrieved));
	        	}
            	LOGGER.trace("Facebook link stats for " + urls + "could not be fetched");
                checkBlocked();
            }
		} catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
            checkBlocked();
        } catch (HttpException e) {
        	LOGGER.error("HttpException " + e.getMessage());
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
	        status = crawler.httpGet(FQL_QUERY+"url='http://www.google.com/'").getStatusCode();
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
		LOGGER.error("Facebook Ranking Service is momentarily blocked. Will check again in 1min.");
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
		types.add(LIKES);
		types.add(SHARES);
		types.add(COMMENTS);
		return types;
	}
	/**
	 * Get the ranking type for this id.
	 * 
	 * @return The ranking type for the given id
	 */
	public RankingType getRankingType(String id) {
		if(id.equals("facebook_likes")) return LIKES;
		else if(id.equals("facebook_shares")) return SHARES;
		else if(id.equals("facebook_comments")) return COMMENTS;
		return null;
	}

}
