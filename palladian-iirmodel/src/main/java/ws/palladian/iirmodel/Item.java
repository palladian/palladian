/**
 * Created on: 29.07.2009 17:52:47
 */
package ws.palladian.iirmodel;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * <p>
 * One entry in an item stream. Items are RSS messages, forum posts, twitter updates or E-Mails.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @since 1.0
 * @version 3.1
 */
@Entity
@Table(name = "Item")
public class Item implements Serializable {

    /**
     * <p>
     * Used to serialize and deserialize objects of this class.
     * </p>
     */
    private static final long serialVersionUID = -7680493016832753262L;

    /**
     * <p>
     * A worldwide unique identifier for this {@link Item}. This identifier is usually created by the database itself.
     * </p>
     */
    @Id
    @GeneratedValue
    private Integer identifier;

    /**
     * <p>
     * The identifier used to identify the item inside the item stream. It might not be world wide unique and only
     * servers as identifier within the stream. This identifier is usually assigned by the item stream and extracted
     * while reading on the stream.
     * </p>
     */
    private String sourceInternalIdentifier;

    /**
     * <p>
     * The item stream that produced this item.
     * </p>
     */
    @ManyToOne
    private ItemStream parent;

    /**
     * <p>
     * The user profile of the author, who created this item.
     * </p>
     */
    @ManyToOne
    private Author author;

    /**
     * <p>
     * The URL used to access this item.
     * </p>
     */
    private String link;

    /**
     * <p>
     * The title of this item.
     * </p>
     */
    private String title;

    /**
     * <p>
     * The date on which this item was initially published.
     * </p>
     */
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date publicationDate;

    /**
     * <p>
     * The date on which this item was last updated.
     * </p>
     */
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updateDate;

    /**
     * <p>
     * The main body text forming the content of this item.
     * </p>
     */
    @Lob
    private String text;

    /**
     * <p>
     * The item occurring as a parent or direct predecessor of this item. This might be {@code null} if this is the
     * first item in a stream. In some cases an item might be the predecessor of multiple other items. This happens if a
     * stream is not linear but forms a tree structure.
     * </p>
     */
    @ManyToOne
    private Item predecessor;

    /**
     * <p>
     * Creates a new {@code Item} with no values. This is used by the ORM.
     * </p>
     */
    protected Item() {
        super();
    }

    /**
     * <p>
     * Creates a new completely initialized {@link Item}.
     * </p>
     * 
     * @param sourceInternalIdentifier The identifier used to identify the item inside the item stream. It might not be
     *            world wide unique and only servers as identifier within the stream. This identifier is usually
     *            assigned by the item stream and extracted while reading on the stream.
     * @param author The user profile of the author, who created this item.
     * @param link The URL used to access this item.
     * @param title The title of this item.
     * @param publicationDate The date on which this item was initially published.
     * @param updateDate The date on which this item was last updated.
     * @param text The main body text forming the content of this item.
     */
    public Item(String sourceInternalIdentifier, Author author, String link, String title, Date publicationDate,
            Date updateDate, String text) {
        this(sourceInternalIdentifier, author, link, title, publicationDate, updateDate, text, null);
    }

    /**
     * <p>
     * Creates a new completely initialized {@link Item} with a predecessor {@link Item}.
     * </p>
     * 
     * @param sourceInternalIdentifier The identifier used to identify the item inside the item stream. It might not be
     *            world wide unique and only servers as identifier within the stream. This identifier is usually
     *            assigned by the item stream and extracted while reading on the stream.
     * @param parent The item stream that produced this item.
     * @param author The user profile of the author, who created this item.
     * @param link The URL used to access this item.
     * @param title The title of this item.
     * @param publicationDate The date on which this item was initially published.
     * @param updateDate The date on which this item was last updated.
     * @param text The main body text forming the content of this item.
     * @param predecessor The item occurring as a parent or direct predecessor of this item. This might be {@code null}
     *            if this is the first item in a stream. In some cases an item might be the predecessor of multiple
     *            other items. This happens if a stream is not linear but forms a tree structure.
     */
    // TODO wouldnt it be better to supply the parent stream also via constructor?
    public Item(String sourceInternalIdentifier, Author author, String link, String title, Date publicationDate,
            Date updateDate, String text, Item predecessor) {
        this();
        this.sourceInternalIdentifier = sourceInternalIdentifier;
        this.author = author;
        this.link = link;
        this.title = title;
        this.publicationDate = publicationDate;
        this.updateDate = updateDate;
        this.text = text;
        this.predecessor = predecessor;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public Integer getIdentifier() {
        return identifier;
    }

    public void setIdentifier(Integer identifier) {
        this.identifier = identifier;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public ItemStream getParent() {
        return parent;
    }

    public void setParent(ItemStream parent) {
        this.parent = parent;
    }

    public Item getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(Item predecessor) {
        this.predecessor = predecessor;
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(Date publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getSourceInternalIdentifier() {
        return sourceInternalIdentifier;
    }

    public void setSourceInternalIdentifier(String sourceInternalIdentifier) {
        this.sourceInternalIdentifier = sourceInternalIdentifier;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    /**
     * <p>
     * Checks whether this {@code Item} and another one are equal. This is the case if they share the same
     * {@link #parent} item stream and the same {@link #sourceInternalIdentifier}.
     * </p>
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     * @see #getParent()
     * @see #getStreamSourceInternalIdentifier()
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
        Item other = (Item)obj;

        // try to check equality by Pk
        Integer otherIdentifier = other.identifier;
        if (identifier != null && otherIdentifier != null) {
            return identifier.equals(otherIdentifier);
        }

        // check equality by fields
        if (sourceInternalIdentifier == null) {
            if (other.sourceInternalIdentifier != null) {
                return false;
            }
        } else if (!sourceInternalIdentifier.equals(other.sourceInternalIdentifier)) {
            return false;
        }
        if (parent == null) {
            if (other.parent != null) {
                return false;
            }
        } else if (!parent.equals(other.parent)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sourceInternalIdentifier == null) ? 0 : sourceInternalIdentifier.hashCode());
        result = prime * result + ((parent == null) ? 0 : parent.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Item [identifier=");
        builder.append(identifier);
        builder.append(", sourceInternalIdentifier=");
        builder.append(sourceInternalIdentifier);
        builder.append(", author=");
        builder.append(author);
        builder.append(", link=");
        builder.append(link);
        builder.append(", title=");
        builder.append(title);
        builder.append(", publicationDate=");
        builder.append(publicationDate);
        builder.append(", updateDate=");
        builder.append(updateDate);
        // builder.append(", text=");
        // builder.append(text);
        builder.append(", predecessor=");
        builder.append(predecessor);
        builder.append("]");
        return builder.toString();
    }

}
