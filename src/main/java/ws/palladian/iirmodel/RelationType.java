/**
 * Created on: 21.05.2010 17:18:35
 */
package ws.palladian.iirmodel;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 1.0
 */
@Entity
public final class RelationType implements Serializable {

    private static final long serialVersionUID = 4461664486685564343L;

    @Id
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

}
