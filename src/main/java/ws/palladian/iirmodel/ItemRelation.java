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
 * A generic relation between two forum entries.
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 1.0
 */
@Entity
public class ItemRelation implements Serializable {

    private static final long serialVersionUID = 9163914602749435760L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private String id;

    @ManyToOne
    private RelationType type;

    @OneToOne
    private Item firstEntry;

    @OneToOne
    private Item secondEntry;

    private String comment;

    protected ItemRelation() {
        super();
    }

    public ItemRelation(Item firstEntry, Item secondEntry, RelationType type, String comment) {
        this();
        this.firstEntry = firstEntry;
        this.secondEntry = secondEntry;
        this.type = type;
        this.comment = comment;
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
        ItemRelation other = (ItemRelation)obj;
        if (firstEntry == null) {
            if (other.firstEntry != null && other.secondEntry != null) {
                return false;
            }
        } else if (!(firstEntry.equals(other.firstEntry) || firstEntry.equals(other.secondEntry))) {
            return false;
        }
        if (secondEntry == null) {
            if (other.secondEntry != null && firstEntry != null) {
                return false;
            }
        } else if (!(secondEntry.equals(other.secondEntry) || secondEntry.equals(other.firstEntry))) {
            return false;
        }

        return true;
    }

    public final String getComment() {
        return comment;
    }

    public Item getFirstItem() {
        return firstEntry;
    }

    public String getId() {
        return id;
    }

    public Item getSecondItem() {
        return secondEntry;
    }

    public RelationType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((firstEntry == null) ? 0 : firstEntry.hashCode());
        result = prime * result + ((secondEntry == null) ? 0 : secondEntry.hashCode());
        return result;
    }

    public final void setComment(String comment) {
        this.comment = comment;
    }

    public void setFirstEntry(Item firstEntry) {
        this.firstEntry = firstEntry;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSecondEntry(Item secondEntry) {
        this.secondEntry = secondEntry;
    }

    public void setType(RelationType type) {
        this.type = type;
    }

}
