/**
 * Created on: 14.05.2010 17:42:21
 */
package ws.palladian.iirmodel;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * A generic relation between two {@link Item}s. This relation is an unordered pair of items, see
 * {@link #equals(Object)} and {@link #hashCode()}.
 * 
 * FIXME At the moment, relations are undirected, which is fine for relation types like "duplicate", but does not work
 * for ordered relations like "caused-by".
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "ItemRelation")
public class ItemRelation implements Serializable {

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
        this();
        this.firstItem = firstEntry;
        this.secondItem = secondEntry;
        this.type = type;
        this.comment = comment;
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

    /*
     * ATTENTION: custom implementation, do not overwrite/generate!
     */
    /**
     * <p>
     * Compares two {@code ItemRelation}s on whether they are equal or not. This depends only on whether both are
     * relations between the same {@link Item}s and have the same {@link RelationType}.
     * </p>
     * 
     * @param obj The object to compare this object to
     * @return {@code true} if both objects are equal; {@code false} otherwise.
     */
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
        ItemRelation other = (ItemRelation)obj;
        if (firstItem == null) {
            if (other.firstItem != null && other.secondItem != null) {
                return false;
            }
        } else if (!(firstItem.equals(other.firstItem) || firstItem.equals(other.secondItem))) {
            return false;
        }
        if (secondItem == null) {
            if (other.secondItem != null && firstItem != null) {
                return false;
            }
        } else if (!(secondItem.equals(other.secondItem) || secondItem.equals(other.firstItem))) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }

        return true;
    }

    /*
     * ATTENTION: custom implementation, do not overwrite/generate!
     */
    /**
     * <p>
     * Calculates a hash value for objects of this class. Look for the documentation of {@link #equals(Object)} for the
     * properties used in this calculation.
     * </p>
     * 
     * @return A hash code for this object.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((firstItem == null) ? 0 : firstItem.hashCode());
        result = result + ((secondItem == null) ? 0 : secondItem.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());

        return result;
    }

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
