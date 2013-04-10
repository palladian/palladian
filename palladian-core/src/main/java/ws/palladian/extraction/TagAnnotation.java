package ws.palladian.extraction;

import ws.palladian.processing.features.Annotated;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * TODO merge this class with {@link PositionAnnotation}.
 * 
 * @author Martin Wunderwald
 */
@Deprecated
public class TagAnnotation implements Annotated {

    /** The start index of the annotation in the annotated text. */
    private final int offset;

    /** The tag of the annotation. */
    private final String tag;

    /** The token of the annotation. */
    private final String chunk;

    /**
     * Constructor.
     * 
     * @param offset
     * @param tag
     * @param value
     */
    public TagAnnotation(int offset, String tag, String value) {
        this.offset = offset;
        this.chunk = value;
        this.tag = tag;
    }

    @Override
    public final String getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return chunk + "/" + tag;
    }

    @Override
    public int getStartPosition() {
        return offset;
    }

    @Override
    public int getEndPosition() {
        return offset + chunk.length();
    }

    @Override
    public int getIndex() {
        return -1; // XXX
    }

    @Override
    public String getValue() {
        return chunk;
    }

    @Override
    public int compareTo(Annotated o) {
        return getStartPosition() - o.getStartPosition();
    }

}
