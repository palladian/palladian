/**
 * Created on: 21.05.2010 17:18:35
 */
package ws.palladian.iirmodel;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "NAME"))
public final class RelationType implements Serializable {

    private static final long serialVersionUID = 4461664486685564343L;

    @Id
    @GeneratedValue
    private Integer identifier;

    private String name;

    protected RelationType() {
        super();
    }

    public RelationType(String name) {
        this();
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public Integer getIdentifier() {
        return identifier;
    }

    public void setIdentifier(Integer identifier) {
        this.identifier = identifier;
    }

    // FIXME
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
        RelationType other = (RelationType) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    // FIXME
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

}
