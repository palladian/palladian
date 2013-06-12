package ws.palladian.processing.features;


/**
 * <p>
 * Interface defining some annotated entity in a text. The annotation is characterized by its start and end position,
 * provides a tag (e.g. a POS tag, entity tag, etc.), its value and a running index.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface Annotated extends Comparable<Annotated> {

    int getStartPosition();

    int getEndPosition();
    
    int getIndex();

    String getTag();

    String getValue();

    boolean overlaps(Annotated annotated);

}
