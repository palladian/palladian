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
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * RankingService implementation for votes and comments on a 
 * given url on reddit.com.
 * <br/><br/>
 * Not more than 1 request every 2 seconds
 * 
 * @author Julien Schmehl
 * @author Philipp Katz
 *
 */
public class RedditStats implements RankingService{

	/** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(RedditStats.class);
    
    private static final String GET_INFO = "http://www.reddit.com/api/info.json?url=";
    
	/** No config values. */
    
    /** Crawler for downloading purposes. */
    private DocumentRetriever crawler = new DocumentRetriever();
    
    /** The id of this service. */
    private static final String SERVICE_ID = "reddit";
    
    /** The ranking value types of this service **/
    /** 
     * The number of up-votes minus down-votes for this url on reddit.com.
     * Commitment value is 0.6
     * Max. Ranking value is 40
     */
    static RankingType VOTES = new RankingType("reddit_votes", "Reddit.com votes", "The number of " +
    		"up-votes minus down-votes for this url on reddit.com.", 0.6f, 40);
    /** 
     * The number of comments users have left for this url on reddit.com.
     * Commitment value is 1.0
     * Max. Ranking value is 40
     */
    static RankingType COMMENTS = new RankingType("reddit_comments", "Reddit.com comments", "The number of " +
    		"comments users have left for this url on reddit.com.", 1.0f, 40);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000*60*1;
    
	public RedditStats() {
		// we use a rather short timeout here, as responses are short.
        crawler.setConnectionTimeout(5000);
        
        // we could use proxies here to circumvent request limitations (1req/2sec)
        
	}

	@Override
	public Ranking getRanking(String url) {
		Map<RankingType, Float> results = new HashMap<RankingType, Float>();
		Ranking ranking = new Ranking(this, url);
		if(isBlocked()) return ranking;

        try {

            String encUrl = StringHelper.urlEncode(url);
            JSONObject json = crawler.getJSONDocument(GET_INFO + encUrl);
            ranking.setRetrieved(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
            if (json != null) {
            	
            	JSONArray children = json.getJSONObject("data").getJSONArray("children");
                int votes = 0;
                int comments = 0;
                for (int i = 0; i < children.length(); i++) {
                    JSONObject child = children.getJSONObject(i);
                    // all post have "kind" : "t3" -- there is no documentation, what this means,
                    // but for robustness sake we check here
                    if (child.getString("kind").equals("t3")) {
                        votes += child.getJSONObject("data").getInt("score");
                        comments += child.getJSONObject("data").getInt("num_comments");
                    }
                }
            	results.put(VOTES, (float) votes);
            	results.put(COMMENTS, (float) comments);
                LOGGER.trace("Reddit stats for " + url + " : " + results);
            } else {
            	results.put(VOTES, null);
            	results.put(COMMENTS, null);
            	LOGGER.trace("Reddit stats for " + url + "could not be fetched");
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
	        status = crawler.httpGet(GET_INFO+"http://www.google.com/").getStatusCode();
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
		LOGGER.error("Reddit Ranking Service is momentarily blocked. Will check again in 1min.");
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
		types.add(VOTES);
		types.add(COMMENTS);
		return types;
	}
	/**
	 * Get the ranking type for this id.
	 * 
	 * @return The ranking type for the given id
	 */
	public RankingType getRankingType(String id) {
		if(id.equals(VOTES.getId())) return VOTES;
		else if(id.equals(COMMENTS.getId())) return COMMENTS;
		return null;
	}

}
