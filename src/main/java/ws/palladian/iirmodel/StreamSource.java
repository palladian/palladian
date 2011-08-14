package ws.palladian.iirmodel;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * <p>
 * Abstract representation for an information stream.
 * </p>
 * 
 * @author Philipp Katz
 * @version 3.0
 * @since 3.0
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "SOURCEADDRESS"))
public abstract class StreamSource implements Serializable {

    /**
     * <p>
     * Used for serializing this object to a file via java API.
     * </p>
     */
    private static final long serialVersionUID = -4700473034518941820L;

    /**
     * <p>
     * The separator character used when building a fully qualified name for the StreamSource.
     * </p>
     */
    private static final char QUALIFIED_NAME_SEPARATOR = '.';

    /**
     * <p>
     * The unique identifier of this {@link ItemStream}. This value is generated automatically and internally by the ORM
     * implementation.
     * </p>
     */
    @Id
    @GeneratedValue
    private Integer identifier;

    /**
     * <p>
     * A human-readable and understandable name for this source. For web forum threads this might be the name of the
     * thread. For <a href="http://www.facebook.com">Facebook</a> it might be "Facebook". When a service with multiple
     * sources is considered, e. g. <a href="http://sourceforge.net/">SourceForge.net</a>, each source can have its own
     * name, like "Forum", "Mailing List", etc. and be bundeled together using a {@link StreamGroup}, which might have
     * the parent's source's name like "SourceForge".
     * </p>
     */
    private String sourceName;

    /**
     * <p>
     * The address to access this stream. This is usually a URL but might be a file system path (in URL form or not) as
     * well. The source address is a identifier for the corresponding stream, e.g. each source address is unique.
     * </p>
     */
    private String sourceAddress;

    /**
     * <p>
     * The parent source, if this {@link StreamSource} is a child.
     * </p>
     */
    @ManyToOne
    private StreamSource parentSource;

    /**
     * <p>
     * The authors contributing to this {@link StreamSource}.
     * </p>
     */
    @OneToMany
    private Set<Author> authors;

    //
    // Constructors
    //

    /**
     * <p>
     * Creates a new {@code StreamSource} with no initial values. Use the provided setter methods to initialize the
     * instance.
     * </p>
     */
    protected StreamSource() {
        super();
        authors = new HashSet<Author>();
    }

    /**
     * <p>
     * Creates a new {@code StreamSource} with no items but all other values initialized.
     * </p>
     * 
     * @param sourceName A human-readable and understandable name for this source.
     * @param sourceAddress The address to access this stream. This usually is an URL but might be a file system path
     *            (in URL form or not) as well. This attribute is used as unique identifier for the source.
     */
    protected StreamSource(String sourceName, String sourceAddress) {
        this();
        this.sourceName = sourceName;
        this.sourceAddress = sourceAddress;
    }

    //
    // Getters and setters
    //

    public final Integer getIdentifier() {
        return identifier;
    }

    public final void setIdentifier(Integer identifier) {
        this.identifier = identifier;
    }

    /**
     * <p>
     * The stream source is a unique name identifying the source. It might be its name as long as no other source with
     * the same name exists or the URL of the source.
     * </p>
     * 
     * @return the unique forum type.
     */
    public final String getSourceName() {
        return sourceName;
    }

    /**
     * <p>
     * The type of a source is a unique name used for identification. It might be its name as long as no other source
     * with the same name exists or the URL of the source.
     * </p>
     * 
     * @param sourceName
     *            the unique source type
     */
    public final void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public final String getSourceAddress() {
        return sourceAddress;
    }

    public final void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    /**
     * <p>
     * Return the parent {@link StreamSource} of this instance.
     * <p>
     * 
     * @return Parent StreamSource, or <code>null</code>, if no parent exists.
     */
    public final StreamSource getParentSource() {
        return parentSource;
    }

    protected final void setParentSource(StreamSource parentSource) {
        this.parentSource = parentSource;
    }

    /**
     * <p>
     * Returns a fully qualified name for this StreamSource.
     * </p>
     * 
     * @return
     */
    public final String getQualifiedSourceName() {
        StringBuilder result = new StringBuilder();
        if (parentSource != null) {
            result.append(parentSource.getQualifiedSourceName());
            result.append(QUALIFIED_NAME_SEPARATOR);
        }
        result.append(getSourceName());
        return result.toString();
    }

    public final Set<Author> getAuthors() {
        return authors;
    }

    public final void setAuthors(Collection<Author> authors) {
        this.authors = new HashSet<Author>(authors);
    }

    public final void addAuthor(Author author) {
        authors.add(author);
    }

    //
    // Iterators which allow convenient traversal of the composite structure:
    //

    /**
     * <p>
     * Obtain an Iterator with {@link StreamSource}s for deep-traversing this StreamSource. The StreamSource itself is
     * also part of the iterator.
     * </p>
     * 
     * @return
     */
    public abstract Iterator<StreamSource> streamSourceIterator();

    /**
     * <p>
     * Obtain an Iterator with {@link ItemStream}s for deep-traversing this StreamSource. When invoked on an ItemStream,
     * the ItemStream itself is also part of the iterator.
     * </p>
     * 
     * @return
     */
    public abstract Iterator<ItemStream> itemStreamIterator();

    /**
     * <p>
     * Obtain an Iterator with {@link StreamGroup}s for deep-traversing this StreamSource. When invoked on a
     * StreamGroup, the StreamGroup itself is also part of the iterator.
     * </p>
     * 
     * @return
     */
    public abstract Iterator<StreamGroup> streamGroupIterator();

    /**
     * <p>
     * Obtain an iterator with {@link Item}s for deep-traversing this StreamSource.
     * </p>
     * 
     * @return
     */
    public abstract Iterator<Item> itemIterator();

    //
    // Also force subclasses to implement the following methods:
    //

    @Override
    public abstract String toString();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

}
