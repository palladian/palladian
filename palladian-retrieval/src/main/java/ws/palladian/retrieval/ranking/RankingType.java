package ws.palladian.retrieval.ranking;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * <p>
 * A ranking value type for a RankingService, since every service can have more than one distinct ranking values. A
 * RankingType is defined by a unique id-String. It holds a name, description and the commitment value.
 * </p>
 * 
 * @author Julien Schmehl
 * @author Philipp Katz
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
        Validate.notEmpty(id, "id must not be empty");
        Validate.notEmpty(name, "name must not be empty");
        this.id = id.replace(" ", "");
        this.name = name;
        this.description = description;
    }

    /**
     * <p>
     * Initialize a new RankingType with the specified parameters.
     * </p>
     * 
     * @param id A unique id for this type, max. 31 chars, without whitespaces, e.g. 'bitly_clicks'
     * @param name A human readable name for this type
     */
    public RankingType(String id, String name) {
        this(id, name, StringUtils.EMPTY);
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id.hashCode();
        result = prime * result + name.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        RankingType other = (RankingType)obj;
        if (!id.equals(other.id)) {
            return false;
        }
        if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

}
