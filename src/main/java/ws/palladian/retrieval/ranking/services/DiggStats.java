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
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * RankingService implementation for diggs and comments containing a 
 * given url on digg.com.<br/>
 * <br/><br/>
 * No information about request limits yet<br/>
 * <br/><br/>
 * TODO implement limt status correctly -> check API doc
 * 
 * @author Julien Schmehl
 * @author Philipp Katz
 *
 */
public class DiggStats implements RankingService{

	/** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(DiggStats.class);
    
    private static final String GET_STORY_INFO = "http://services.digg.com/2.0/story.getInfo?links=";
    
	/** No config values. */
    
    /** Crawler for downloading purposes. */
    private DocumentRetriever crawler = new DocumentRetriever();
    
    /** The id of this service. */
    private static final String SERVICE_ID = "digg";
    
    /** The ranking value types of this service **/
    /** 
     * The number of times users have "dugg" this url on digg.com.
     * Commitment value is 1.0529
     * Max. Ranking value is 30
     */
    static RankingType DIGGS = new RankingType("digg_diggs", "Digg.com diggs", "The number of " +
    		"times users have \"dugg\" this url on digg.com.", 1.0529f, 30, new int[]{0,0,0,1,2,4,6,10,30});
    /** 
     * The number of comments users have left for this digged url on digg.com.
     * Commitment value is 1.0980
     * Max. Ranking value is 18
     */
    static RankingType COMMENTS = new RankingType("digg_comments", "Digg.com comments", "The number of " +
    		"comments users have left for this digged url on digg.com.", 1.0980f, 18, new int[]{0,0,0,1,1,2,4,6,18});

    
    /** The topic weighting coefficients for this service **/
    @SuppressWarnings("serial")
    private static Map<String, Float> topicWeighting = new HashMap<String, Float>() {
        {
            put("business", 2.3915f);
            put("politics", 1.8302f);
            put("entertainment", 3.0389f);
            put("lifestyle", 4.8621f);
            put("sports", 18.8886f);
            put("technology", 1.8797f);
            put("science", 1.7209f);
        }
    };
    
    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000*60*1;
    
	public DiggStats() {
		
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
            JSONObject json = crawler.getJSONDocument(GET_STORY_INFO + encUrl);
            ranking.setRetrieved(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
            if (json != null) {
            	int diggs = 0;
            	int comments = 0;
            	if(json.getJSONArray("stories").length() > 0) {
            		diggs = json.getJSONArray("stories").getJSONObject(0).getInt("diggs");
            		comments = json.getJSONArray("stories").getJSONObject(0).getInt("comments");
            	}
            	results.put(DIGGS, DIGGS.normalize(diggs));
            	results.put(COMMENTS, COMMENTS.normalize(comments));
                LOGGER.trace("Digg stats for " + url + " : " + results);
            } else {
            	results.put(DIGGS, null);
            	results.put(COMMENTS, null);
            	LOGGER.trace("Digg stats for " + url + "could not be fetched");
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
		List<String> tempUrls = new ArrayList<String>(urls);
		String encUrls = "";

        try {
        	for(int i=0; i<urls.size(); i++){
        		encUrls += StringHelper.urlEncode(urls.get(i));
        		if(i < urls.size()-1) encUrls += ",";
        	}
            
            JSONObject json = crawler.getJSONDocument(GET_STORY_INFO + encUrls);
            Timestamp retrieved = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
            if (json != null) {
            	
            	JSONArray stories = json.getJSONArray("stories");
            	
            	String url = "";
            	int diggs = -1;
            	int comments = -1;

            	// iterate through "stories" and add rankings to results map
            	// delete every URL found in the response from tempUrls
                for (int i = 0; i < stories.length(); i++) {
                    url = stories.getJSONObject(i).getString("url");
                    diggs = stories.getJSONObject(i).getInt("diggs");
                    comments = stories.getJSONObject(i).getInt("comments");
                    Map<RankingType, Float> result = new HashMap<RankingType, Float>();
	            	result.put(DIGGS, DIGGS.normalize(diggs));
	            	result.put(COMMENTS, COMMENTS.normalize(comments));
                    results.put(url, new Ranking(this, url, result, retrieved));
                    tempUrls.remove(url);
                    LOGGER.trace("Digg stats for " + url + " : " + result);
                }
                // add the remaining URLs (which were not in the list of "stories") with a ranking of 0
                for(String u:tempUrls){
                	Map<RankingType, Float> result = new HashMap<RankingType, Float>();
	            	result.put(DIGGS, 0f);
	            	result.put(COMMENTS, 0f);
	        		results.put(u, new Ranking(this, u, result, retrieved));
                }
            } else {
	        	for(String u:tempUrls) {
	        		Map<RankingType, Float> result = new HashMap<RankingType, Float>();
	            	result.put(DIGGS, null);
	            	result.put(COMMENTS, null);
	        		results.put(u, new Ranking(this, u, result, retrieved));
	        	}
            	LOGGER.trace("Digg stats for " + urls + "could not be fetched");
                checkBlocked();
            }

        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
            checkBlocked();
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
	        status = crawler.httpGet(GET_STORY_INFO+"http://www.google.com/").getStatusCode();
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
		LOGGER.error("Digg Ranking Service is momentarily blocked. Will check again in 1min.");
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
		types.add(DIGGS);
		types.add(COMMENTS);
		return types;
	}
	/**
	 * Get the ranking type for this id.
	 * 
	 * @return The ranking type for the given id
	 */
	public RankingType getRankingType(String id) {
		if(id.equals("digg_diggs")) return DIGGS;
		else if(id.equals("digg_comments")) return COMMENTS;
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

}
