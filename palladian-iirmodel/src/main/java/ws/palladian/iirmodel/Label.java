/**
 * Created on: 27.09.2011 07:30:49
 */
package ws.palladian.iirmodel;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
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
    /**
     * <p>
     * The automatically generated, system wide unique database identifier.
     * </p>
     */
    @Id
    @GeneratedValue
    private final Integer identifier;

    /**
     * <p>
     * The {@link Item} labeled by this label.
     * </p>
     */
    @ManyToOne
    private final Item labeledItem;

    /**
     * <p>
     * The type of this {@code Label}.
     * </p>
     */
    @ManyToOne
    private final LabelType type;

    /**
     * <p>
     * An optional comment explaining why the {@code Label} was created.
     * </p>
     */
    @Lob
    private final String comment;

    /**
     * <p>
     * Default constructor, that should only be called by the JPA persistence layer. Please use
     * {@link #Label(Item, LabelType, String)} instead.
     * </p>
     */
    protected Label() {
        this(null, null, null);
    }

    /**
     * <p>
     * Creates a fully initialized {@code Label} object.
     * </p>
     * 
     * @param labeledItem The {@link Item} labeled by this label.
     * @param type The type of this {@code Label}.
     * @param comment An optional comment explaining why the {@code Label} was created.
     */
    public Label(final Item labeledItem, final LabelType type, final String comment) {
        super();
        this.identifier = null;
        this.labeledItem = labeledItem;
        this.type = type;
        this.comment = comment;
    }

    /**
     * @return The {@link Item} labeled by this label.
     */
    public final Item getLabeledItem() {
        return labeledItem;
    }

    /**
     * @return The type of this {@code Label}.
     */
    public final LabelType getType() {
        return type;
    }

    /**
     * @return An optional comment explaining why the {@code Label} was created.
     */
    public final String getComment() {
        return comment;
    }

    /**
     * @return The automatically generated, system wide unique database identifier.
     */
    public final Integer getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Label [identifier=");
        builder.append(identifier);
        builder.append(", labeledItem=");
        builder.append(labeledItem);
        builder.append(", type=");
        builder.append(type);
        builder.append(", comment=");
        builder.append(comment);
        builder.append("]");
        return builder.toString();
    }
}
