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

import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * RankingService implementation to get the number of bookmarks of 
 * a given url on Delicious.<br/>
 * http://www.delicious.com - general bookmark service
 * <br/><br/>
 * Wait at least 1 second between requests<br/>
 * Feeds only updated 1-2 times per hour
 * <br/><br/>
 * TODO use proxies to vercome limits
 * 
 * @author Julien Schmehl
 * @author Philipp Katz
 *
 */
public class DeliciousBookmarks implements RankingService {	

	/** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(DeliciousBookmarks.class);
    
	/** No config values. */
    
    /** Crawler for downloading purposes. */
    private DocumentRetriever crawler = new DocumentRetriever();
    
    /** The id of this service. */
    private static final String SERVICE_ID = "delicious";
    
    /** The ranking value types of this service **/
    /** 
     * The number of bookmarks users have created for this url.
     * Commitment value is 1.4154
     * Max. Ranking value is 10
     */
    static RankingType BOOKMARKS = new RankingType("delicious_bookmarks", "Delicious Bookmarks", "The number of " +
    		"bookmarks users have created for this url.", 1.4154f, 10);

    /** The topic weighting coefficients for this service **/
    @SuppressWarnings("serial")
	private static Map<String, Float> topicWeighting = new HashMap<String, Float>() {
        {
            put("business", 0.9129f);
            put("politics", 1.4894f);
            put("entertainment", 1.8891f);
            put("lifestyle", 1.3397f);
            put("sports", 0.9935f);
            put("technology", 1.0110f);
            put("science", 1.4930f);
        }
    };
    
    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000*60*1;
    
	public DeliciousBookmarks() {

		// we use a rather short timeout here, as responses are short.
        crawler.setConnectionTimeout(5000);
        
        // using different proxies for each request to avoid 1 second request limit
        // doesn't work with every proxy
        	//crawler.setSwitchProxyRequests(1);
        	//crawler.setProxyList(configuration.getList("documentRetriever.proxyList"));
        
	}

	@Override
	public Ranking getRanking(String url) {
		Map<RankingType, Float> results = new HashMap<RankingType, Float>();
		Ranking ranking = new Ranking(this, url);
		if(isBlocked()) return ranking;

        try {

        	String encUrl = StringHelper.urlEncode(url);
            JSONArray json = crawler.getJSONArray("http://feeds.delicious.com/v2/json/urlinfo?url=" + encUrl);
            ranking.setRetrieved(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
            
            if (json != null) {
                int result = 0;
                if(json.length() > 0) result = json.getJSONObject(0).getInt("total_posts");
                results.put(BOOKMARKS, (float) result);
                LOGGER.trace("Delicious bookmarks for " + url + " : " + result);
            } else {
            	results.put(BOOKMARKS, null);
            	LOGGER.trace("Delicious bookmarks for " + url + "could not be fetched");
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
	        status = crawler.httpGet("http://feeds.delicious.com/v2/json/urlinfo?url=http://www.google.com/").getStatusCode();
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
		LOGGER.error("Delicious Ranking Service is momentarily blocked. Will check again in 1min. Try changing your IP-Address.");
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
}
