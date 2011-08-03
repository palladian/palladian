package ws.palladian.iirmodel;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

/**
 * <p>
 * A group of {@link StreamSource}s. Following the composite pattern, a StreamSource itself can either contain
 * {@link ItemStream}s or other {@link StreamSource}s.
 * </p>
 * 
 * @author Philipp Katz
 * @version 3.0
 * @since 3.0
 */
@Entity
public final class StreamGroup extends StreamSource {

    /**
     * <p>
     * Used for serializing this object to a file via java API.
     * </p>
     */
    private static final long serialVersionUID = -8868599334273751877L;

    /**
     * <p>
     * The child elements of this group.
     * </p>
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parentSource")
    private final List<StreamSource> children;

    /**
     * <p>
     * Creates a new, empty {@link StreamGroup} with no initial values.
     * </p>
     */
    protected StreamGroup() {
        children = new ArrayList<StreamSource>();
    }

    /**
     * <p>
     * Creates a new, empty {@link StreamGroup} with the specified values.
     * </p>
     * 
     * @param streamSource
     * @param sourceAddress
     * @param channelName
     */
    public StreamGroup(String streamSource, String sourceAddress) {
        super(streamSource, sourceAddress);
        children = new ArrayList<StreamSource>();
    }

    /**
     * <p>
     * Get all direct children of the {@link StreamGroup}.
     * </p>
     * 
     * @return Children of this StreamGroup.
     */
    public List<StreamSource> getChildren() {
        return children;
    }

    /**
     * <p>
     * Adds a new {@link StreamSource} to the end of the list of this {@link StreamGroup}. If the supplied StreamSource
     * already exists in this group, it is replaced by the supplied one.
     * </p>
     * 
     * @param child The StreamSource to add.
     */
    public void addChild(StreamSource child) {
        if (children.contains(child)) {
            children.remove(child);
        }
        children.add(child);
        child.setParentSource(this);
    }

    @Override
    public String toString() {
        return "StreamGroup [children=" + children + "]";
    }

    //
    // Attention: do not auto-generate the following methods,
    // they have been manually changed to consider the super#getSourceAddress()
    //

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((children == null) ? 0 : children.hashCode());
        result = prime * result + ((getSourceAddress() == null) ? 0 : getSourceAddress().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StreamGroup other = (StreamGroup)obj;
        if (children == null) {
            if (other.children != null)
                return false;
        } else if (!children.equals(other.children))
            return false;
        if (getSourceAddress() == null) {
            if (other.getSourceAddress() != null)
                return false;
        } else if (!getSourceAddress().equals(other.getSourceAddress()))
            return false;
        return true;
    }

}
