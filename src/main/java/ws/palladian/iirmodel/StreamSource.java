package ws.palladian.iirmodel;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
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
     * The stream source is a system wide unique name identifying the source for a set of generated item streams. It
     * might be the source's name as long as no other stream with the same name exists or the source's URL otherwise.
     * For web forum threads this might be the forum name. For <a href="http://www.facebook.com">Facebook</a> it might
     * be "facebook" or "http://facebook.com". When a service with multiple sources is considered, e. g. <a
     * href="http://sourceforge.net/">SourceForge.net</a>, each source must have its own name, like "phpMyAdmin forum",
     * "phpMyAdmin mailing list", etc.
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
     * @param sourceName The stream source is a system wide unique name identifying the source for a set of generated
     *            item streams. It might be the sources name as long as no other stream with the same name exists or the
     *            sources URL otherwise. For web forum threads this might be the forum name. For <a
     *            href="http://www.facebook.com">Facebook</a> it might be "facebook" or "http://facebook.com".
     * @param sourceAddress The address to access this stream. This usually is an URL but might be a file system path
     *            (in URL form or not) as well.
     * @param channelName Streams with similar content are often presented together under common name. This property
     *            provides the name of the stream channel the current item stream belongs to.
     */
    public StreamSource(String sourceName, String sourceAddress) {
        this();
        this.sourceName = sourceName;
        this.sourceAddress = sourceAddress;
    }

    //
    // Getters and setters
    //

    public Integer getIdentifier() {
        return identifier;
    }

    public void setIdentifier(Integer identifier) {
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
    public String getSourceName() {
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
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
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

    public Collection<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(Set<Author> authors) {
        this.authors = authors;
    }

    public void addAuthor(Author author) {
        authors.add(author);
    }

    //
    // Force subclasses to implement the following methods:
    //

    @Override
    public abstract String toString();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

}
