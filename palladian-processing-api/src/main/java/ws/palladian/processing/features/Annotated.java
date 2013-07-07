package ws.palladian.processing.features;

/**
 * <p>
 * Interface defining some annotated entity in a text. The annotation is characterized by its start and end position,
 * provides a tag (e.g. a POS tag, entity tag, etc.) and its value. <b>Important:</b> Implementations of this interface
 * should be providing suitable {@link #hashCode()} and {@link #equals(Object)} methods.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface Annotated extends Comparable<Annotated> {

    int getStartPosition();

    int getEndPosition();

    String getTag();

    String getValue();

    boolean overlaps(Annotated annotated);

    // XXX add commented methods below, but introduce common base class first

    // boolean congruent(Annotated annotated);

    // boolean sameTag(Annotated annotated);

    // XXX force implementation of hashCode/equals via base class

}
