/**
 * Created on: 21.05.2010 17:18:35
 */
package ws.palladian.iirmodel;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * <p>
 * A type for relations defining an {@link ItemRelation}'s semantical meaning.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "NAME"), name = "RelationType")
public final class RelationType implements Serializable, Comparable<RelationType> {

    private static final long serialVersionUID = 4461664486685564343L;

    @Id
    @GeneratedValue
    private Integer identifier;

    private String name;

    @Lob
    private String explanation;

    protected RelationType() {
        super();
    }

    public RelationType(String name, String explanation) {
        this();
        this.name = name;
        this.explanation = explanation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getIdentifier() {
        return identifier;
    }

    public void setIdentifier(Integer identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the explanation
     */
    public String getExplanation() {
        return explanation;
    }

    /**
     * @param explanation the explanation to set
     */
    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RelationType other = (RelationType)obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RelationType [identifier=");
        builder.append(identifier);
        builder.append(", name=");
        builder.append(name);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int compareTo(RelationType otherRelationType) {
        return this.name.compareTo(otherRelationType.name);
    }

}
