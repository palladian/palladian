package ws.palladian.iirmodel;

/**
 * Created on: 13.09.2009 21:22:49
 */

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import ws.palladian.iirmodel.helper.StreamVisitor;

/**
 * <p>
 * Represents a thread from a web forum or discussion board.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @since 1.0
 * @version 3.1
 */
@Entity
@DiscriminatorValue(value = "surface.ItemStream")
public final class ItemStream extends StreamSource {

    /**
     * <p>
     * Used for serializing this object to a file via java API.
     * </p>
     */
    private static final long serialVersionUID = 9194871722956364875L;

    /**
     * <p>
     * The {@link Item}s available within this item stream.
     * </p>
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    // @OrderBy("publicationDate ASC")
    private List<Item> items;

    /**
     * <p>
     * Creates a new {@link ItemStream} with no initial values. Use the provided setter methods to initialize the
     * instance. This should only be called by the JPA persistence layer and never directly. Please use
     * {@link #ItemStream(String, String)} instead.
     * </p>
     */
    protected ItemStream() {
        super();
        this.items = new LinkedList<Item>();
    }

    /**
     * <p>
     * Creates a new {@link ItemStream} with no items but all other values initialized.
     * </p>
     * 
     * @param sourceName A human-readable and understandable name for this source.
     * @param sourceAddress The address to access this stream. This usually is an URL but might be a file system path
     *            (in URL form or not) as well. This attribute is used as unique identifier for the source.
     */
    public ItemStream(String sourceName, String sourceAddress) {
        super(sourceName, sourceAddress);
        this.items = new LinkedList<Item>();
    }

    /**
     * @return The {@link Item}s available within this item stream.
     */
    public List<Item> getItems() {
        return items;
    }

    /**
     * @param items The {@link Item}s available within this item stream.
     */
    public void setItems(List<Item> items) {
        this.items = items;
    }

    /**
     * <p>
     * Adds a new {@link Item} to the end of this {@link ItemStream}s list of items. If same item already exists, it is
     * overwritten by the new one.
     * </p>
     * 
     * @param item The new item to add.
     */
    public void addItem(Item item) {
        if (items.contains(item)) {
            items.remove(item);
        }
        items.add(item);
        item.setParent(this);
    }

    /**
     * <p>
     * Adds a {@link Collection} of items to the end of this {@link ItemStream}'s list of items. If some item already
     * exists, it is overwritten by the new one.
     * </p>
     * 
     * @param items
     */
    public void addItems(Collection<Item> items) {
        for (Item item : items) {
            addItem(item);
        }
    }

    @Override
    protected void accept(StreamVisitor visitor, int depth) {
        visitor.visitItemStream(this, depth);
        for (Item item : getItems()) {
            visitor.visitItem(item, depth + 1);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ItemStream [identifier=");
        builder.append(getIdentifier());
        builder.append(", streamSource=");
        builder.append(getSourceName());
        builder.append(", items=");
        builder.append(items);
        builder.append(", sourceAddress=");
        builder.append(getSourceAddress());
        builder.append("]");
        return builder.toString();
    }

    //
    // Attention: do not auto-generate the following methods,
    // they have been manually changed to consider the super#getSourceAddress()
    //

    @Override
    public boolean equals(Object itemStream) {
        if (this == itemStream) {
            return true;
        }
        if (itemStream == null) {
            return false;
        }
        if (getClass() != itemStream.getClass()) {
            return false;
        }

        StreamSource other = (StreamSource)itemStream;
        if (getSourceAddress() == null) {
            if (other.getSourceAddress() != null) {
                return false;
            }
        } else if (!getSourceAddress().equals(other.getSourceAddress())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getSourceAddress() == null) ? 0 : getSourceAddress().hashCode());
        return result;
    }
}
