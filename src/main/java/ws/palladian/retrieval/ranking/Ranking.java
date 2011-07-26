package ws.palladian.retrieval.ranking;

import java.sql.Timestamp;
import java.util.Map;

import com.ibm.icu.util.Calendar;

/**
 * Represents a ranking value retrieved at a given moment for a given RankingService.
 * 
 * @author Julien Schmehl
 * 
 */
public class Ranking {
	
	
	/** The ranking service producing this ranking */
	private RankingService service;
	/** The ranking values */
	private Map<RankingType, Float> values;
	/** The url these ranking values are for */
	private String url;
	/** The time when the ranking was retrieved */
	private Timestamp retrieved;
	/** The topic this URL was classified in */
	private String topic;
	
	
	
	
	public Ranking(){
		super();
	}
	
	public Ranking(RankingService service){
		this.setService(service);
	}
	
	public Ranking(RankingService service, String url){
		this.setService(service);
		this.setUrl(url);
	}
	
	public Ranking(RankingService service, String url, Map<RankingType, Float> values){
		this.setService(service);
		this.setValues(values);
		this.setUrl(url);
		this.setRetrieved(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
	}
	
	public Ranking(RankingService service, String url, Map<RankingType, Float> values, Timestamp retrieved){
		this.setService(service);
		this.setValues(values);
		this.setUrl(url);
		this.setRetrieved(retrieved);
	}
	
	public Ranking(RankingService service, String url, Map<RankingType, Float> values, Timestamp retrieved, String topic){
		this.setService(service);
		this.setValues(values);
		this.setUrl(url);
		this.setRetrieved(retrieved);
		this.setTopic(topic);
	}

	/**
	 * Get the total of all ranking values
	 * associated with this ranking
	 * 
	 * @return the total sum of all ranking values
	 * 
	 */
	public float getRankingValueSum() {
		float sum = 0;
		for(Float v:values.values()) sum += v;
		return sum;
	}
	/**
	 * Get the total of all ranking values
	 * associated with this ranking weighted 
	 * by their commitment factors
	 * 
	 * @return the total sum of all ranking values weighted 
	 * by their commitment factors
	 * 
	 */
	public float getWeightedRankingValueSum() {
		float weightedSum = 0;
		for(RankingType rt:values.keySet()) {
			weightedSum += rt.getCommittment()*values.get(rt);
		}
		return weightedSum;
	}
	
	
	public void setService(RankingService service) {
		this.service = service;
	}
	public RankingService getService() {
		return service;
	}
	/**
	 * Set a map of all ranking values
	 * associated with this ranking and their
	 * corresponding ranking type
	 * 
	 * @param pairs of ranking type and ranking value
	 * 
	 */
	public void setValues(Map<RankingType, Float> values) {
		this.values = values;
	}
	/**
	 * Get a map of all ranking values
	 * associated with this ranking and their
	 * corresponding ranking type
	 * 
	 * @return pairs of ranking type and ranking value
	 * 
	 */
	public Map<RankingType, Float> getValues() {
		return values;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUrl() {
		return url;
	}
	public void setRetrieved(Timestamp retrieved) {
		this.retrieved = retrieved;
	}
	public Timestamp getRetrieved() {
		return retrieved;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}
	public String getTopic() {
		return topic;
	}



}
