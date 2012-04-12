package ws.palladian.retrieval.ranking;

/**
 * <p>
 * A ranking value type for a RankingService, since every service can have more than one distinct ranking values. A
 * RankingType is defined by a unique id-String. It holds a name, description and the commitment value.
 * </p>
 * 
 * @author Julien Schmehl
 * 
 */
public class RankingType {

    private final String id;
    private final String name;
    private final String description;

    /**
     * <p>
     * Initialize a new RankingType with the specified parameters.
     * </p>
     * 
     * @param id A unique id for this type, max. 31 chars, without whitespaces, e.g. 'bitly_clicks'
     * @param name A human readable name for this type
     * @param description A short description of this type
     */
    public RankingType(String id, String name, String description) {
        this.id = id.replace(" ", "");
        this.name = name;
        this.description = description;
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

    public String toString() {
        return this.name;
    }
}
