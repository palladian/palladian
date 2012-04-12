package ws.palladian.iirmodel;

import javax.persistence.Entity;

/**
 * <p>
 * An undirected {@link ItemRelation}, i.e. relation(Item1, Item2) equals relation(Item2, Item1). In other words, this
 * relation is an <b>unordered</b> pair of {@link Item}s, see {@link #equals(Object)} and {@link #hashCode()} for
 * implementation specific details.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 1.0
 * @since 3.0
 */
@Entity
public final class UndirectedItemRelation extends ItemRelation {

    private static final long serialVersionUID = 933415576462536133L;

    /**
     * <p>
     * JPA specific constructor, not to be used by human beings.
     * </p>
     */
    protected UndirectedItemRelation() {
        super();
    }

    /**
     * <p>
     * Creates a completely initialized {@link UndirectedItemRelation}.
     * </p>
     * 
     * @param firstEntry One {@link Item} in the relation.
     * @param secondEntry One {@link Item} in the relation.
     * @param type The type of the relation between {@code firstItem} and {@code secondItem}.
     * @param comment An optional comment describing why {@link #firstItem} is related to {@link #secondItem}.
     */
    public UndirectedItemRelation(Item firstEntry, Item secondEntry, RelationType type, String comment) {
        super(firstEntry, secondEntry, type, comment);
    }

    /**
     * <p>
     * Creates a completely initialized {@link UndirectedItemRelation}.
     * </p>
     * 
     * @param firstEntry One {@link Item} in the relation.
     * @param secondEntry One {@link Item} in the relation.
     * @param type The type of the relation between {@code firstItem} and {@code secondItem}.
     * @param comment An optional comment describing why {@link #firstItem} is related to {@link #secondItem}.
     * @param confidence An optional confidence value for the relation, may be <code>null</code>.
     */
    public UndirectedItemRelation(Item firstEntry, Item secondEntry, RelationType type, String comment, Double confidence) {
        super(firstEntry, secondEntry, type, comment, confidence);
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
        if (getFirstItem() == null) {
            if (other.getFirstItem() != null && other.getSecondItem() != null) {
                return false;
            }
        } else if (!(getFirstItem().equals(other.getFirstItem()) || getFirstItem().equals(other.getSecondItem()))) {
            return false;
        }
        if (getSecondItem() == null) {
            if (other.getSecondItem() != null && getFirstItem() != null) {
                return false;
            }
        } else if (!(getSecondItem().equals(other.getSecondItem()) || getSecondItem().equals(other.getFirstItem()))) {
            return false;
        }
        if (getType() == null) {
            if (other.getType() != null) {
                return false;
            }
        } else if (!getType().equals(other.getType())) {
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
        result = prime * result + ((getFirstItem() == null) ? 0 : getFirstItem().hashCode());
        result = result + ((getSecondItem() == null) ? 0 : getSecondItem().hashCode());
        result = prime * result + ((getType() == null) ? 0 : getType().hashCode());

        return result;
    }

}
