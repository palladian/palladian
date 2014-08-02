package ws.palladian.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Default, immutable implementation for {@link Annotation} interface. Fields are immutable, but the class is not final.
 * Make sure to preserve immutability in subclasses. If you need to implement mutable subclasses, do <b>not</b> inherit
 * from here, to avoid confusion!
 * </p>
 * 
 * @author Philipp Katz
 */
public class ImmutableAnnotation extends AbstractAnnotation {

    /** The position of the first character of this {@code Annotation}. */
    private final int startPosition;

    /** The {@link String} value marked by this annotation. */
    private final String value;

    /** The tag assigned to this annotation. */
    private final String tag;

    /**
     * <p>
     * Create a new {@link Annotation} at the given position, with the specified value and tag.
     * </p>
     * 
     * @param startPosition The start offset in the text, greater or equal zero.
     * @param value The value of the annotation, not <code>null</code> or empty.
     * @param tag An (optional) tag.
     */
    public ImmutableAnnotation(int startPosition, String value, String tag) {
        Validate.isTrue(startPosition >= 0, "startPosition cannot be negative.");
        Validate.notEmpty(value, "value must not be empty");
        this.startPosition = startPosition;
        this.value = value;
        this.tag = tag;
    }

    /**
     * <p>
     * Create a new {@link Annotation} at the given position, with the specified value and tag.
     * </p>
     * 
     * @param startPosition The start offset in the text, greater or equal zero.
     * @param value The value of the annotation, not <code>null</code> or empty.
     */
    public ImmutableAnnotation(int startPosition, String value) {
        this(startPosition, value, StringUtils.EMPTY);

    }

    @Override
    public final int getStartPosition() {
        return startPosition;
    }

    @Override
    public final String getTag() {
        return tag;
    }

    @Override
    public final String getValue() {
        return value;
    }

}
