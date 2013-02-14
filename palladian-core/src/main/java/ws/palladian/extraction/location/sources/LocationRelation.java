package ws.palladian.extraction.location.sources;

/**
 * <p>
 * A relation between two locations. This class implements {@link Comparable}, where the comparison takes place via the
 * specified priority, sorting higher prioritized items (lower priority value) first.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class LocationRelation implements Comparable<LocationRelation> {

    private final int parentId;
    private final int childId;
    private final int priority;

    public LocationRelation(int parentId, int childId, int priority) {
        this.parentId = parentId;
        this.childId = childId;
        this.priority = priority;
    }

    public int getParentId() {
        return parentId;
    }

    public int getChildId() {
        return childId;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(LocationRelation o) {
        return Integer.valueOf(priority).compareTo(o.priority);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + childId;
        result = prime * result + parentId;
        result = prime * result + priority;
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
        LocationRelation other = (LocationRelation)obj;
        if (childId != other.childId)
            return false;
        if (parentId != other.parentId)
            return false;
        if (priority != other.priority)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("(%s,%s,%s)", parentId, childId, priority);
    }

}