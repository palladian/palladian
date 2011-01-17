package tud.iir.preprocessing.nlp;

public class TagAnnotation {

    /** The start index of the annotation in the annotated text. */
    private int offset = -1;

    /** The length of the annotation. */
    private int length = -1;

    /** The tag of the annotation. */
    private String tag = "";

    /** The token of the annotation. */
    private String chunk = "";

    /**
     * Constructor.
     * 
     * @param tagAnnotation
     */
    public TagAnnotation(TagAnnotation tagAnnotation) {
        offset = tagAnnotation.getOffset();
        length = tagAnnotation.getLength();
        tag = tagAnnotation.getTag();
        chunk = tagAnnotation.getChunk();
    }

    /**
     * Constructor.
     * 
     * @param offset
     * @param tag
     * @param chunk
     */
    public TagAnnotation(int offset, String tag, String chunk) {
        this.offset = offset;
        length = chunk.length();
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
     * @param offset
     *            the offset to set
     */
    public final void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * @return the length
     */
    public final int getLength() {
        return length;
    }

    /**
     * @param length
     *            the length to set
     */
    public final void setLength(int length) {
        this.length = length;
    }

    /**
     * @return the tag
     */
    public final String getTag() {
        return tag;
    }

    /**
     * @param tag
     *            the tag to set
     */
    public final void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * @return the chunk
     */
    public final String getChunk() {
        return chunk;
    }

    /**
     * @param chunk
     *            the chunk to set
     */
    public final void setChunk(String chunk) {
        this.chunk = chunk;
    }

    @Override
    public String toString() {
        return chunk + "/" + tag;
    }

}
