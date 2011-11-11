/**
 * Created on: 27.09.2011 08:13:55
 */
package ws.palladian.iirmodel;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @since 0.0.1
 * @version 1.0.0
 */
@Entity
@Table(name = "ANNOTATIONTYPE")
public final class LabelType {
    @Id
    @GeneratedValue
    private final Integer identifier;
    private final String typeName;

    /**
     * 
     */
    protected LabelType() {
        super();
        identifier = null;
        typeName = null;
    }

    public LabelType(final String typeName) {
        super();
        this.identifier = null;
        this.typeName = typeName;
    }

    /**
     * @return the identifier
     */
    public final Integer getIdentifier() {
        return identifier;
    }

    /**
     * @return the typeName
     */
    public final String getTypeName() {
        return typeName;
    }

    @Override
    public String toString() {
        return typeName;
    }
}
