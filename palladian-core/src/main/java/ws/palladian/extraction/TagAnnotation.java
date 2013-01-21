package ws.palladian.extraction;

import ws.palladian.processing.features.Annotated;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * TODO merge this class with {@link PositionAnnotation}.
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
     * @param chunk
     */
    public TagAnnotation(int offset, String tag, String chunk) {
        this.offset = offset;
        this.chunk = chunk;
        this.tag = tag;
    }

//    /**
//     * @return the offset
//     */
//    public final int getOffset() {
//        return offset;
//    }

    /**
     * @return the tag
     */
    @Override
    public final String getTag() {
        return tag;
    }

//    /**
//     * @return the chunk
//     */
//    public final String getChunk() {
//        return chunk;
//    }

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
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getValue() {
        return chunk;
    }

}
