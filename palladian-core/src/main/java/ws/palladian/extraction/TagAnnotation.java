package ws.palladian.extraction;

import ws.palladian.processing.features.PositionAnnotation;

/**
 * TODO merge this class with {@link PositionAnnotation}.
 * @author Martin Wunderwald
 */
public class TagAnnotation {

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

    /**
     * @return the offset
     */
    public final int getOffset() {
        return offset;
    }

    /**
     * @return the tag
     */
    public final String getTag() {
        return tag;
    }

    /**
     * @return the chunk
     */
    public final String getChunk() {
        return chunk;
    }

    @Override
    public String toString() {
        return chunk + "/" + tag;
    }

}
