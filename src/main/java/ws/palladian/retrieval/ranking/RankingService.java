package ws.palladian.retrieval.ranking;

import java.util.List;
import java.util.Map;

/**
 * Interface for ranking service implementations.
 * 
 * @author Julien Schmehl
 *
 */
public interface RankingService {
	

	
	/**
	 * Get ranking values for a single url.
	 * 
	 * @param url
	 * @return A map of ranking values for each type
	 */
	public Ranking getRanking(String url);
	
	/**
	 * Get ranking values for a batch of urls.
	 * 
	 * @param urls A list of urls
	 * @return A map of ranking values per url
	 */
	public Map<String, Ranking> getRanking(List<String> urls);
	
	/**
	 * Get the id of this ranking service.
	 * 
	 * @return The id-string of this service
	 */
	public String getServiceId();
	
	/**
	 * Get all ranking types of this ranking service.
	 * 
	 * @return A list of ranking types
	 */
	public List<RankingType> getRankingTypes();
	
	/**
	 * Get the ranking type for this id.
	 * 
	 * @return The ranking type for the given id
	 */
	public RankingType getRankingType(String id);
	
	/**
	 * Retrieve this service topic weighting coefficient
	 * for a given topic
	 * 
	 * @return Weighting coefficient if topic is known, 1 otherwise
	 */
	public float getTopicWeighting(String topic);
	
	/**
	 * Force a new check if this service is blocked due to excess
	 * of request limits. This updates the blocked-attribute
	 * of this service.
	 * 
	 * @return True if the service is momentarily blocked, false otherwise
	 */
	public boolean checkBlocked();
	
	/**
	 * Returns if this service is momentarily blocked or not.
	 * 
	 * @return True if the service is momentarily blocked, false otherwise
	 */
	public boolean isBlocked();

	/**
	 * Sets this service blocked status to unblocked and resets the
	 * time of the last check to now.
	 * 
	 * @return True if reset was successful, false otherwise
	 */
	public boolean resetBlocked();

}
