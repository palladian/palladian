package ws.palladian.core;

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
public interface Annotation extends Comparable<Annotation> {

    /**
     * @return The start offset of this annotation in the text (first character in text is zero).
     */
    int getStartPosition();

    /**
     * @return The end offset of this annotation in the text (this is startPosition + value.length()).
     */
    int getEndPosition();

    /**
     * @return The tag assigned to this annotation (like POS, entity, etc.).
     */
    String getTag();

    /**
     * @return The string value of this annotation.
     */
    String getValue();

    /**
     * <p>
     * Determine, whether this annotation overlaps another given annotation (i.e. start/end boundaries are within/on the
     * on the other annotation).
     * </p>
     * 
     * @param other The other annotation, not <code>null</code>.
     * @return <code>true</code> in case this annotation overlaps the given one, <code>false</code> otherwise.
     */
    boolean overlaps(Annotation other);

    /**
     * <p>
     * Determine, whether this and the given annotation are congruent (i.e. start and end position are the same).
     * <p>
     * 
     * @param other The other annotation, not <code>null</code>.
     * @return <code>true</code> in case this annotation and the given are congruent, <code>false</code> otherwise.
     */
    boolean congruent(Annotation other);

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
