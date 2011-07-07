/**
 * Created on: 14.05.2010 17:42:21
 */
package ws.palladian.iirmodel;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

/**
 * A generic relation between two {@link Item}s.
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 1.0
 * @since 1.0
 */
@Entity
public class ItemRelation implements Serializable {

    private static final long serialVersionUID = 9163914602749435760L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne
    private RelationType type;

    @OneToOne
    private Item firstItem;

    @OneToOne
    private Item secondItem;

    private String comment;

    protected ItemRelation() {
        super();
    }

    public ItemRelation(Item firstEntry, Item secondEntry, RelationType type, String comment) {
        this();
        this.firstItem = firstEntry;
        this.secondItem = secondEntry;
        this.type = type;
        this.comment = comment;
    }

    public final String getComment() {
        return comment;
    }

    public final void setComment(String comment) {
        this.comment = comment;
    }

    public Item getFirstItem() {
        return firstItem;
    }

    public void setFirstItem(Item firstItem) {
        this.firstItem = firstItem;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Item getSecondItem() {
        return secondItem;
    }

    public void setSecondItem(Item secondItem) {
        this.secondItem = secondItem;
    }

    public RelationType getType() {
        return type;
    }

    public void setType(RelationType type) {
        this.type = type;
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
        ItemRelation other = (ItemRelation) obj;
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

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((firstItem == null) ? 0 : firstItem.hashCode());
        result = prime * result + ((secondItem == null) ? 0 : secondItem.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
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
