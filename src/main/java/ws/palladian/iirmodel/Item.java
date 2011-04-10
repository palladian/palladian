/**
 * Created on: 29.07.2009 17:52:47
 */
package ws.palladian.iirmodel;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * One entry in an item stream. Items are RSS messages, forum posts, twitter updates or E-Mails.
 * 
 * @author Klemens Muthmann
 * @since 1.0
 * @version 2.0
 * 
 */
@Entity
public class Item implements Serializable {
    /**
     * Used to serialize and deserialize objects of this class.
     */
    private static final long serialVersionUID = -7680493016832753262L;

    /**
     * A worldwide unique identifier for this message stream item. This identifier is usually created by the database
     * itself.
     */
    @Id
    @GeneratedValue
    private String identifier;

    /**
     * The identifier used to identify the item inside the item stream. It might not be world wide unique and only
     * servers as identifier within the stream. This identifier is usually assigned by the item stream and extracted
     * while reading on the stream.
     */
    private String streamSourceInternalIdentifier;

    /**
     * The item stream that produced this item.
     */
    @ManyToOne
    private ItemStream parent;

    /**
     * The user profile of the author, who created this item.
     */
    @ManyToOne
    private Author author;
    /**
     * The URL used to access this item.
     */
    private String link;
    /**
     * The title of this item.
     */
    private String title;

    /**
     * The date on which this item was initially published.
     */
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date publicationDate;
    /**
     * The date on which this item was last updated.
     */
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updateDate;
    /**
     * The main body text forming the content of this item.
     */
    @Lob
    private String text;

    /**
     * The item occurring as a parent or direct predecessor of this item. This might be {@code null} if this is the
     * first item in a stream. In some cases an item might be the predecessor of multiple other items. This happens if a
     * stream is not linear but forms a tree structure.
     */
    private Item predecessor;

    /**
     * A type giving the semantics of this items content. It defines for example if the entry is a question an
     * answer or something completely different.
     */
    @Enumerated(EnumType.STRING)
    private ItemType type;

    /**
     * Creates a new {@code Item} with no values. Call all setters to initialize this {@code Item}.
     */
    public Item() {
        super();
    }

    /**
     * Creates a new completely initialized item.
     * 
     * @param forumInternalIdentifier The identifier used to identify the item inside the item stream. It might not be
     *            world wide unique and only servers as identifier within the stream. This identifier is usually
     *            assigned by the item stream and
     *            extracted while reading on the stream.
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
     * @param type A type giving the semantics of this items content. It defines for example if the entry is a question
     *            an answer or something completely different.
     */
    public Item(String forumInternalIdentifier, ItemStream parent, Author author, String link, String title,
            Date publicationDate, Date updateDate, String text, Item predecessor, ItemType type) {
        this();
        this.streamSourceInternalIdentifier = forumInternalIdentifier;
        this.parent = parent;
        this.author = author;
        this.link = link;
        this.title = title;
        this.publicationDate = publicationDate;
        this.updateDate = updateDate;
        this.text = text;
        this.predecessor = predecessor;
        this.type = type;
    }

    /**
     * Checks whether this {@code Item} and another one are equal. This is the case if they share the same {@code
     * parent} item stream and the same {@code streamSourceInternalIdentifier}.
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
        Item other = (Item) obj;
        if (streamSourceInternalIdentifier == null) {
            if (other.streamSourceInternalIdentifier != null) {
                return false;
            }
        } else if (!streamSourceInternalIdentifier.equals(other.streamSourceInternalIdentifier)) {
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

    public Author getAuthor() {
        return author;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getLink() {
        return link;
    }

    public ItemStream getParent() {
        return parent;
    }

    public Item getPredecessor() {
        return predecessor;
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public String getStreamSourceInternalIdentifier() {
        return streamSourceInternalIdentifier;
    }

    public String getText() {
        return text;
    }

    public String getTitle() {
        return title;
    }

    public final ItemType getType() {
        return type;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((streamSourceInternalIdentifier == null) ? 0 : streamSourceInternalIdentifier.hashCode());
        result = prime * result + ((parent == null) ? 0 : parent.hashCode());
        return result;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public void setForumInternalIdentifier(String forumInternalIdentifier) {
        this.streamSourceInternalIdentifier = forumInternalIdentifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setPredecessor(Item predecessor) {
        this.predecessor = predecessor;
    }

    public void setPublicationDate(Date publicationDate) {
        this.publicationDate = publicationDate;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public final void setType(ItemType type) {
        this.type = type;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

}
