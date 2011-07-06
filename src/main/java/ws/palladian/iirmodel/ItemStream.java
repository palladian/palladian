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
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

// TODO role of Comparable unclear. Comparison is done based on # of items in the Stream.

/**
 * <p>
 * Represents a thread from a web forum or discussion board.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @since 1.0
 * @version 2.0
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "SOURCEADDRESS"))
public class ItemStream implements Comparable<ItemStream>, Serializable {
    /**
     * <p>
     * Used for serializing this object to a file via java API.
     * </p>
     */
    private static final long serialVersionUID = 9194871722956364875L;

    /**
     * <p>
     * The unique identifier of this {@code ItemStream}. This value is generated automatically and internally by the ORM
     * implementation.
     * </p>
     */
    @Id
    @GeneratedValue
    private String identifier;

    /**
     * <p>
     * The stream source is a system wide unique name identifying the source for a set of generated item streams. It
     * might be the sources name as long as no other stream with the same name exists or the sources URL otherwise. For
     * web forum threads this might be the forum name. For <a href="http://www.facebook.com">Facebook</a> it might be
     * "facebook" or "http://facebook.com".
     * </p>
     */
    private String streamSource;

    /**
     * <p>
     * The items available within this item stream.
     * </p>
     */
    @OneToMany(cascade = CascadeType.ALL)
    @OrderBy("publicationDate ASC")
    private List<Item> items;

    /**
     * <p>
     * The address to access this stream. This is usually an URL but might be a file system path (in URL form or not) as
     * well. The source address is a identifier for the corresponding stream, e.g. each source address is unique.
     * </p>
     */
    private String sourceAddress;

    /**
     * <p>
     * Streams with similar content are often presented together under common name. This property provides the name of
     * the stream channel the current item stream belongs to. In case of a web forum, channel names might correspond to
     * different sub-forums.
     * </p>
     */
    private String channelName;

    /**
     * <p>
     * Creates a new {@code ItemStream} with no initial values. Only used by the ORM implementation, therefore
     * protected.
     * </p>
     */
    protected ItemStream() {
        super();
        this.items = new LinkedList<Item>();
    }

    /**
     * <p>
     * Creates a new {@code ItemStream} with no items but all other values initialized.
     * </p>
     * 
     * @param streamSource The stream source is a system wide unique name identifying the source for a set of generated
     *            item streams. It might be the sources name as long as no other stream with the same name exists or the
     *            sources URL otherwise. For web forum threads this might be the forum name. For <a
     *            href="http://www.facebook.com">Facebook</a> it might be "facebook" or "http://facebook.com".
     * @param sourceAddress The address to access this stream. This usually is an URL but might be a file system path
     *            (in URL form or not) as well.
     * @param channelName Streams with similar content are often presented together under common name. This property
     *            provides the name of the stream channel the current item stream belongs to.
     */
    public ItemStream(String streamSource, String sourceAddress, String channelName) {
        this();
        // this.identifier = streamIdentifier + "@" + streamSource;
        this.streamSource = streamSource;
        this.sourceAddress = sourceAddress;
        this.channelName = channelName;
    }

    /**
     * <p>
     * Adds a new item to the end of this {@code ItemStream}s list of items.
     * </p>
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
     * <p>
     * The type of a forum is a unique name identifying the forum. It might be its name as long as no other forum with
     * the same name exists or the URL of the forum.
     * </p>
     * 
     * @return the unique forum type.
     */
    public String getStreamSource() {
        return streamSource;
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
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((streamSource == null) ? 0 : streamSource.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ItemStream [identifier=");
        builder.append(identifier);
        builder.append(", streamSource=");
        builder.append(streamSource);
        builder.append(", items=");
        builder.append(items);
        builder.append(", sourceAddress=");
        builder.append(sourceAddress);
        builder.append(", channelName=");
        builder.append(channelName);
        builder.append("]");
        return builder.toString();
    }

    // public final void setChannelName(String channelName) {
    // this.channelName = channelName;
    // }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    // public void setItems(List<Item> items) {
    // this.items = items;
    // }

    // public void setSourceAddress(String sourceAddress) {
    // this.sourceAddress = sourceAddress;
    // }

    // /**
    // * The type of a forum is a unique name identifying the forum. It might be its name as long as no other forum with
    // * the same name exists or the URL of the forum.
    // *
    // * @param streamSource
    // * the unique forum type
    // */
    // public void setStreamSource(String streamSource) {
    // this.streamSource = streamSource;
    // }
}
