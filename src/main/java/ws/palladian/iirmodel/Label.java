/**
 * Created on: 27.09.2011 07:30:49
 */
package ws.palladian.iirmodel;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * <p>
 * A label on an {@link Item} assigned by a person or some program. Labels give the semantics of the {@code Item}.
 * Examples are question, answer, etc.
 * </p>
 * 
 * @author Klemens Muthmann
 * @since 0.0.1
 * @version 1.0.0
 */
@Entity
@Table(name = "Label")
public final class Label {
    @Id
    @GeneratedValue
    private final Integer identifier;

    @ManyToOne
    private final Item labeledItem;

    @ManyToOne
    private final LabelType type;

    private final String comment;

    /**
     * 
     */
    protected Label() {
        this(null, null, null);
    }

    public Label(final Item labeledItem, final LabelType type, final String comment) {
        super();
        this.identifier = null;
        this.labeledItem = labeledItem;
        this.type = type;
        this.comment = comment;
    }

    /**
     * @return the annotatedItem
     */
    public final Item getLabeledItem() {
        return labeledItem;
    }

    /**
     * @return the annotation
     */
    public final LabelType getType() {
        return type;
    }

    /**
     * @return the comment
     */
    public final String getComment() {
        return comment;
    }

    /**
     * @return the identifier
     */
    public final Integer getIdentifier() {
        return identifier;
    }
}
