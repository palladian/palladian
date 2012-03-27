/**
 * Created on: 14.05.2010 17:42:21
 */
package ws.palladian.iirmodel;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * <p>
 * An abstract relation between two {@link Item}s.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "ItemRelation")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class ItemRelation implements Serializable {

    private static final long serialVersionUID = 9163914602749435760L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    /**
     * <p>
     * The type of the relation between {@link #firstItem} and {@link #secondItem}.
     * </p>
     */
    @ManyToOne
    private RelationType type;

    /**
     * <p>
     * One {@link Item} in the relation.
     * </p>
     */
    @OneToOne
    private Item firstItem;

    /**
     * <p>
     * One {@link Item} in the relation.
     * </p>
     */
    @OneToOne
    private Item secondItem;

    /**
     * <p>
     * An optional comment describing why {@link #firstItem} is related to {@link #secondItem}.
     * </p>
     */
    @Lob
    private String comment;

    /**
     * <p>
     * An optional confidence value for the relation.
     * </p>
     */
    private Double confidence;

    /**
     * <p>
     * Protected default constructor. This should only be used by the JPA persistence layer and never invoked directly.
     * </p>
     */
    protected ItemRelation() {
        super();
    }

    /**
     * <p>
     * Creates a completely initialized {@code ItemRelation}.
     * </p>
     * 
     * @param firstEntry One {@link Item} in the relation.
     * @param secondEntry One {@link Item} in the relation.
     * @param type The type of the relation between {@code firstItem} and {@code secondItem}.
     * @param comment An optional comment describing why {@link #firstItem} is related to {@link #secondItem}.
     */
    public ItemRelation(Item firstEntry, Item secondEntry, RelationType type, String comment) {
        this(firstEntry, secondEntry, type, comment, null);
    }

    /**
     * <p>
     * Creates a completely initialized {@code ItemRelation}.
     * </p>
     * 
     * @param firstEntry One {@link Item} in the relation.
     * @param secondEntry One {@link Item} in the relation.
     * @param type The type of the relation between {@code firstItem} and {@code secondItem}.
     * @param comment An optional comment describing why {@link #firstItem} is related to {@link #secondItem}.
     * @param confidence An optional confidence value for the relation, may be <code>null</code>.
     */
    public ItemRelation(Item firstEntry, Item secondEntry, RelationType type, String comment, Double confidence) {
        this();
        this.firstItem = firstEntry;
        this.secondItem = secondEntry;
        this.type = type;
        this.comment = comment;
        this.confidence = confidence;
    }

    /**
     * @return An optional comment describing why {@link #firstItem} is related to {@link #secondItem}.
     */
    public final String getComment() {
        return comment;
    }

    /**
     * <p>
     * Sets and overwrites the old {@code comment} property with a new value.
     * </p>
     * 
     * @param comment An optional comment describing why {@link #firstItem} is related to {@link #secondItem}.
     */
    public final void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * @return One {@link Item} in the relation.
     */
    public Item getFirstItem() {
        return firstItem;
    }

    /**
     * @param firstItem One {@link Item} in the relation.
     */
    public void setFirstItem(Item firstItem) {
        this.firstItem = firstItem;
    }

    /**
     * @return The database identifier of this {@code ItemRelation}.
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id The database identifier of this {@code ItemRelation}.
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return One {@link Item} in the relation.
     */
    public Item getSecondItem() {
        return secondItem;
    }

    /**
     * @param secondItem One {@link Item} in the relation.
     */
    public void setSecondItem(Item secondItem) {
        this.secondItem = secondItem;
    }

    /**
     * @return The type of the relation between {@code firstItem} and {@code secondItem}.
     */
    public RelationType getType() {
        return type;
    }

    /**
     * @param type The type of the relation between {@code firstItem} and {@code secondItem}.
     */
    public void setType(RelationType type) {
        this.type = type;
    }

    /**
     * @return The confidence value for the relation between the two items, or <code>null</code> if no confidence
     *         specified.
     */
    public Double getConfidence() {
        return confidence;
    }

    /**
     * @param confidence The confidence value for the relation between the two items, or <code>null</code> if no
     *            confidence specified.
     */
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    /**
     * <p>
     * Must be implemented by subclasses individually.
     * </p>
     */
    @Override
    public abstract int hashCode();

    /**
     * <p>
     * Must be implemented by subclasses individually.
     * </p>
     */
    @Override
    public abstract boolean equals(Object obj);

    /**
     * <p>
     * Prints this relation and all its properties to a {@code String}.
     * </p>
     * 
     * @return The {@code String} representation of this object.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ItemRelation [id=");
        builder.append(id);
        builder.append(", type=");
        builder.append(type);
        builder.append(", firstItem=");
        builder.append(firstItem);
        builder.append(", secondItem=");
        builder.append(secondItem);
        builder.append(", comment=");
        builder.append(comment);
        builder.append("]");
        return builder.toString();
    }

}
