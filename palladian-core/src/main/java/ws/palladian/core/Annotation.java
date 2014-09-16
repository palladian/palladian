package ws.palladian.core;

import ws.palladian.helper.functional.Function;

/**
 * <p>
 * Interface defining some annotated entity in a text. The annotation is characterized by its start and end position,
 * provides a tag (e.g. a POS tag, entity tag, etc.) and its value. <b>Important:</b> Implementations of this interface
 * should be providing suitable {@link #hashCode()} and {@link #equals(Object)} methods. Usually, you want to resort to
 * {@link AbstractAnnotation} which already provides common functionality.
 * </p>
 * 
 * <p>
 * The {@link Comparable} interface should be implemented, such that {@link Annotation}s are sorted by the their start
 * offset, and in case start offsets are equal, to put longer annotations first.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface Annotation extends Token {

    /** Function to convert an {@link Annotation} to its tag. */
    Function<Annotation, String> TAG_CONVERTER = new Function<Annotation, String>() {
        @Override
        public String compute(Annotation input) {
            return input.getTag();
        }
    };

    /**
     * @return The tag assigned to this annotation (like POS, entity, etc.).
     */
    String getTag();

    /**
     * <p>
     * Determine, whether this and the given annotation have the same tags. Tags are compared case insensitively.
     * </p>
     * 
     * @param other The other annotation, not <code>null</code>.
     * @return <code>true</code> in case tags are equal, <code>false</code> otherwise.
     */
    boolean sameTag(Annotation other);

}
