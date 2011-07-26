package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import com.temesoft.google.pr.JenkinsHash;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * RankingService implementation for PageRank value from Google.<br/>
 * http://www.google.com/
 * <br/><br/>
 * Courtesy limit: 1,000,000 requests/day & 100,000 requests/second/user
 * 
 * @author Julien Schmehl
 * @author Christopher Friedrich
 *
 */
public class GooglePageRank implements RankingService{

	/** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(GooglePageRank.class);
    
	/** The config values. */
    private String apiKey;
    
    /** Crawler for downloading purposes. */
    private DocumentRetriever crawler = new DocumentRetriever();
    
    /** The id of this service. */
    private static final String SERVICE_ID = "pagerank";
    
    /** The ranking value types of this service **/
    /** 
     * The PageRank from Google.
     * Commitment value is 1.0
     */
    static RankingType PAGERANK = new RankingType("pagerank", "Google PageRank", "The PageRank value from Google", 1.0f);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000*60*1;
    
	public GooglePageRank() {
		
		PropertiesConfiguration configuration = ConfigHolder.getInstance().getConfig();

        if (configuration != null) {
        	setApiKey(configuration.getString("api.google.key"));
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
	        
	        // original code from ws.palladian.retrieval.ranking.RankingRetriever
	        JenkinsHash jHash = new JenkinsHash();
	        long urlHash = jHash.hash(("info:" + url).getBytes());
	        String response = crawler
	                .getTextDocument("http://toolbarqueries.google.com/search?client=navclient-auto&hl=en&" + "ch=6"
	                        + urlHash + "&ie=UTF-8&oe=UTF-8&features=Rank&q=info:" + encUrl);
	        
	        ranking.setRetrieved(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
            if (response != null) {
            	int result = 0;
            	// result stays 0 if response empty -> url not found
            	if (response.contains(":")) {
            		response = response.split(":")[2].trim();
	                result = Integer.valueOf(response);
            	}
                results.put(PAGERANK, (float) result);
        		LOGGER.trace("Google PageRank for " + url + " : " + result);
	        } else {
            	results.put(PAGERANK, null);
            	LOGGER.trace("Google PageRank for " + url + "could not be fetched");
                checkBlocked();
            }
        } catch (Exception e) {
            LOGGER.error("Exception " + e.getMessage());
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
			JenkinsHash jHash = new JenkinsHash();
	        long urlHash = jHash.hash(("info:http://www.google.com/").getBytes());
	        status = crawler.httpGet("http://toolbarqueries.google.com/search?client=navclient-auto&hl=en&" + "ch=6"
                    + urlHash + "&ie=UTF-8&oe=UTF-8&features=Rank&q=info:http://www.google.com/").getStatusCode();
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
		LOGGER.error("Google PageRank Ranking Service is momentarily blocked. Will check again in 1min. Try changing your IP-address.");
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
		types.add(PAGERANK);
		return types;
	}
	/**
	 * Returns PAGERANK.
	 * 
	 * @return The ranking type PAGERANK
	 */
	public RankingType getRankingType(String id) {
		return PAGERANK;
	}
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getApiKey() {
		return apiKey;
	}

}
