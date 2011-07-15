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
     * Commitment value is 0.6
     */
    static RankingType DIGGS = new RankingType("digg_diggs", "Digg.com diggs", "The number of " +
    		"times users have \"dugg\" this url on digg.com.", 0.6f);
    /** 
     * The number of comments users have left for this digged url on digg.com.
     * Commitment value is 1.0
     */
    static RankingType COMMENTS = new RankingType("digg_comments", "Digg.com comments", "The number of " +
    		"comments users have left for this digged url on digg.com.", 1.0f);

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
            	results.put(DIGGS, (float) diggs);
            	results.put(COMMENTS, (float) comments);
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
	            	result.put(DIGGS, (float) diggs);
	            	result.put(COMMENTS, (float) comments);
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
		else if(id.equals("digg_comments")) return DIGGS;
		return null;
	}

}
