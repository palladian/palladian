package ws.palladian.core;

/**
 * <p>
 * Common functionality for {@link Annotation} interface.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class AbstractAnnotation extends AbstractToken implements Annotation {

    @Override
    public final boolean sameTag(Annotation other) {
        return getTag().equalsIgnoreCase(other.getTag());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Annotation [value=");
        builder.append(getValue());
        builder.append(", tag=");
        builder.append(getTag());
        builder.append(", span=");
        builder.append(getStartPosition());
        builder.append("-");
        builder.append(getEndPosition());
        builder.append("]");
        return builder.toString();
    }

}
