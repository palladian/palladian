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
public class ItemStream implements Serializable {

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
    private Integer identifier;

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
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent")
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
     * Creates a new {@code ItemStream} with no initial values. Use the provided setter methods to initialize the
     * instance.
     * </p>
     */
    public ItemStream() {
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
        this.streamSource = streamSource;
        this.sourceAddress = sourceAddress;
        this.channelName = channelName;
    }

    public  String getChannelName() {
        return channelName;
    }

    public  void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public Integer getIdentifier() {
        return identifier;
    }

    public void setIdentifier(Integer identifier) {
        this.identifier = identifier;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    /**
     * <p>
     * Adds a new {@link Item} to the end of this {@code ItemStream}s list of items. If same item already exists, the
     * existing item is removed.
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

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    /**
     * <p>
     * The stream source is a unique name identifying the source. It might be its name as long as no other source with
     * the same name exists or the URL of the forum.
     * </p>
     * 
     * @return the unique forum type.
     */
    public String getStreamSource() {
        return streamSource;
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
        if (sourceAddress == null) {
            if (other.sourceAddress != null) {
                return false;
            }
        } else if (!sourceAddress.equals(other.sourceAddress)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sourceAddress == null) ? 0 : sourceAddress.hashCode());
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

}
