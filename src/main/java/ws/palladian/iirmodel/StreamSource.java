package ws.palladian.iirmodel;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

import ws.palladian.iirmodel.helper.StreamVisitor;

/**
 * <p>
 * Abstract representation for an information stream.
 * </p>
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @version 3.1
 * @since 3.0
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "SOURCEADDRESS"), name = "StreamSource")
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
    // private static final char QUALIFIED_NAME_SEPARATOR = '.';

    /**
     * <p>
     * The unique identifier of this {@link ItemStream}. This value is generated automatically and internally by the ORM
     * implementation.
     * </p>
     */
    @Id
    @GeneratedValue(generator = "gentable")
    @TableGenerator(name = "gentable", table = "SEQUENCE", pkColumnName = "SEQ_COUNT", valueColumnName = "SEQ_NAME", pkColumnValue = "SEQ_GEN")
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
    @ManyToMany
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

    public final void setParentSource(StreamSource parentSource) {
        this.parentSource = parentSource;
    }

    public final Set<Author> getAuthors() {
        return authors;
    }

    /**
     * <p>
     * Get an {@link Author} by his username.
     * </p>
     * 
     * @param username
     * @return The author with the specified username, or <code>null</code>, if no such author exists.
     */
    public final Author getAuthor(String username) {
        Author ret = null;
        for (Author author : authors) {
            if (author.getUsername().equals(username)) {
                ret = author;
                break;
            }
        }
        return ret;
    }

    /**
     * <p>
     * Sets a new set of {@link Author}s writing {@link Item}s for this {@code StreamSource} and overwrites the old set
     * of {@code Author}s.
     * </p>
     * 
     * @param authors The new {@code Author}s of this {@code StreamSource}.
     */
    public final void setAuthors(Collection<Author> authors) {
        this.authors = new HashSet<Author>(authors);
    }

    /**
     * <p>
     * Adds an {@link Author} who writes {@link Item}s for this {@code StreamSource}.
     * </p>
     * 
     * @param author The {@code Author} to add.
     */
    public final void addAuthor(Author author) {
        authors.add(author);
    }

    //
    // Visitor interface, which allow even more convenient traversal of the composite structure.
    //

    /**
     * <p>
     * Method for traversing this {@link StreamSource} with a {@link StreamVisitor}. The visitor works depth-first and
     * traverses the whole structure, including {@link Item}s.
     * </p>
     * 
     * @param visitor
     */
    public final void accept(StreamVisitor visitor) {
        accept(visitor, 0);
    }

    protected abstract void accept(StreamVisitor visitor, int depth);

    //
    // Also force subclasses to implement the following methods:
    //

    @Override
    public abstract String toString();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    /**
     * <p>
     * Provides the root element of a {@code StreamSource} hierarchy. In a Web Forum for example this is the whole
     * Forum.
     * </p>
     * 
     * @return The root {@code StreamSource}.
     */
    public StreamSource getRootSource() {
        StreamSource parentSource = getParentSource();
        if (parentSource == null) {
            return this;
        } else {
            return parentSource.getRootSource();
        }
    }

}
