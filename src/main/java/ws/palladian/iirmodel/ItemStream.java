package ws.palladian.iirmodel;

/**
 * Created on: 13.09.2009 21:22:49
 */

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

/**
 * Represents a thread from a web forum or discussion board.
 * 
 * @author Klemens Muthmann
 * @since 1.0
 * @version 2.0
 */
@Entity
public class ItemStream implements Comparable<ItemStream>, Serializable {
    /**
     * Used for serializing this object to a file via java API.
     */
    private static final long serialVersionUID = 9194871722956364875L;
    /**
     * The unique identifier of this {@code ItemStream}. This value is generated automatically by the ORM
     * implementation.
     */
    @Id
    @GeneratedValue
    private String identifier;

    /**
     * The identifier used to identify this {@code ItemStream} within its {@code streamSource}.
     */
    private String streamIdentifier;
    /**
     * The stream source is a system wide unique name identifying the source for a set of generated item streams. It
     * might be the sources name as long as no other stream with the same name exists or the sources URL otherwise. For
     * web forum threads this might be the forum name. For <a href="http://www.facebook.com">Facebook</a> it might be
     * "facebook" or "http://facebook.com".
     */
    private String streamSource;

    /**
     * The items available over this item stream.
     */
    @OneToMany(cascade = CascadeType.ALL)
    @OrderBy("publicationDate ASC")
    private List<Item> items;

    /**
     * The address to access this stream. This usually is an URL but might be a file system path (in URL form or not) as
     * well.
     */
    private String sourceAddress;

    /**
     * Streams with similar content are often presented together under common name. This property provides the name of
     * the stream channel the current item stream belongs to.
     */
    private String channelName;

    /**
     * Creates a new {@code ItemStream} with no initial values. To initialize this new {@code ItemStream} call its
     * setter methods with approriate values.
     */
    public ItemStream() {
        super();
        this.items = new LinkedList<Item>();
    }

    /**
     * Creates a new {@code ItemStream} with no items but all other values initialized.
     * 
     * @param streamIdentifier The identifier used to identify this {@code ItemStream} within its {@code streamSource}.
     * @param streamSource The stream source is a system wide unique name identifying the source for a set of generated
     *            item streams. It might be the sources name as long as no other stream with the same name exists or the
     *            sources URL otherwise. For web forum threads this might be the forum name. For <a
     *            href="http://www.facebook.com">Facebook</a> it might be "facebook" or "http://facebook.com".
     * @param sourceAddress The address to access this stream. This usually is an URL but might be a file system path
     *            (in URL form or not) as well.
     * @param channelName Streams with similar content are often presented together under common name. This property
     *            provides the name of the stream channel the current item stream belongs to.
     */
    public ItemStream(String streamIdentifier, String streamSource, String sourceAddress, String channelName) {
        this();
        this.streamIdentifier = streamIdentifier;
        this.streamSource = streamSource;
        this.sourceAddress = sourceAddress;
        this.channelName = channelName;
    }

    /**
     * Adds a new item to the end of this {@code ItemStream}s list of items.
     * 
     * @param contribution The new contribution to add.
     */
    public void addItem(Item item) {
        if (items.contains(item)) {
            items.remove(item);
        }
        this.items.add(item);
    }

    public int compareTo(ItemStream itemStream) {
        if (itemStream == null) {
            throw new IllegalArgumentException("Object to compare to was null.");
        }
        return this.items.size() - itemStream.items.size();
    }

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
        ItemStream other = (ItemStream) itemStream;
        if (streamSource == null) {
            if (other.streamSource != null) {
                return false;
            }
        } else if (!streamSource.equals(other.streamSource)) {
            return false;
        }
        if (streamIdentifier == null) {
            if (other.streamIdentifier != null) {
                return false;
            }
        } else if (!streamIdentifier.equals(other.streamIdentifier)) {
            return false;
        }
        return true;
    }

    public final String getChannelName() {
        return channelName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public List<Item> getItems() {
        return items;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    /**
     * @return The identifier used to identify this thread within its forum.
     */
    public String getStreamIdentifier() {
        return streamIdentifier;
    }

    /**
     * The type of a forum is a unique name identifying the forum. It might be its name as long as no other forum with
     * the same name exists or the URL of the forum.
     * 
     * @return the unique forum type.
     */
    public String getStreamSource() {
        return streamSource;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((streamSource == null) ? 0 : streamSource.hashCode());
        result = prime * result + ((streamIdentifier == null) ? 0 : streamIdentifier.hashCode());
        return result;
    }

    public final void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    /**
     * @param streamIdentifier
     *            The identifier used to identify this thread within its forum.
     */
    public void setStreamIdentifier(String streamIdentifier) {
        this.streamIdentifier = streamIdentifier;
    }

    /**
     * The type of a forum is a unique name identifying the forum. It might be its name as long as no other forum with
     * the same name exists or the URL of the forum.
     * 
     * @param streamSource
     *            the unique forum type
     */
    public void setStreamSource(String streamSource) {
        this.streamSource = streamSource;
    }
}
