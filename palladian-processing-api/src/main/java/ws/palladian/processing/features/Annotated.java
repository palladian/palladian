package ws.palladian.processing.features;

/**
 * <p>
 * Interface defining some annotated entity in a text. The annotation is characterized by its start and end position,
 * provides a tag (e.g. a POS tag, entity tag, etc.), its value and a running index.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface Annotated {

    public abstract int getStartPosition();

    public abstract int getEndPosition();

    public abstract int getIndex();

    public abstract String getTag();

    public abstract String getValue();

}
