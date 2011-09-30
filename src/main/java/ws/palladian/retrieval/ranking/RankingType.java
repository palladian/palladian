package ws.palladian.retrieval.ranking;


/**
 *  A ranking value type for a RankingService, since every service
 *  can have more than one distinct ranking values
 *  <br/><br/>
 *  A RankingType is defined by a unique id-String<br/>
 *  It holds a name, description and the commitment value
 *  
 *  @author Julien Schmehl
 *  
 */
public class RankingType {
	
	private final String id;
	private final String name;
	private final String description;
	private final float commitment;
	private final float maxRanking;
	private final int[] percentiles;
	
	/**
	 * Get ranking values for a single url.
	 * 
	 * @param id A unique id for this type, max. 31 chars, without whitespaces, e.g. 'bitly_clicks'
	 * @param name A human readable name for this type
	 * @param description A short description of this type
	 * @param commitment The commitment value, between 0.5 and 1.0
	 */
	public RankingType(String id, String name, String description, float commitment, float maxRanking, int[] percentiles) {
		this.id = id.replace(" ", "");
		this.name = name;
		this.description = description;
		this.commitment = commitment;
		this.maxRanking = maxRanking;
		this.percentiles = percentiles;
	}
	
	
	public String getId() {
		return this.id;
	}
	public String getName() {
		return this.name;
	}
	public String getDescription() {
		return this.description;
	}
	public float getCommittment() {
		return this.commitment;
	}
	public float getMaxRanking() {
		return this.maxRanking;
	}
	public int[] getPercentiles() {
		return this.percentiles;
	}
	public String toString() {
		return this.name;
	}

	public float normalize(float value) {
		if(value>this.percentiles[8]) return 1.0f;
		else if(value>this.percentiles[7]) return 0.9f;
		else if(value>this.percentiles[6]) return 0.8f;
		else if(value>this.percentiles[5]) return 0.7f;
		else if(value>this.percentiles[4]) return 0.6f;
		else if(value>this.percentiles[3]) return 0.5f;
		else if(value>this.percentiles[2]) return 0.4f;
		else if(value>this.percentiles[1]) return 0.3f;
		else if(value>this.percentiles[0]) return 0.2f;
		else if(value<=0) return 0.0f;
		else return 0.1f;
	}
	
}
