package tud.iir.extraction.event;

public class TagAnnotation {

    /** The start index of the annotation in the annotated text. */
    private int offset = -1;

    /** The length of the annotation. */
    private int length = -1;

    /** The tag of the annotation. */
    private String tag = "";

    /** The token of the annotation. */
    private String chunk = "";

    public TagAnnotation(TagAnnotation tagAnnotation) {
        offset = tagAnnotation.getOffset();
        length = tagAnnotation.getLength();
        tag = tagAnnotation.getTag();
        chunk = tagAnnotation.getChunk();
    }

    public TagAnnotation(int offset, String tag, String chunk) {
        this.offset = offset;
        length = chunk.length();
        this.chunk = chunk;
        this.tag = tag;
    }

    /**
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * @param offset
     *            the offset to set
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * @return the length
     */
    public int getLength() {
        return length;
    }

    /**
     * @param length
     *            the length to set
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * @return the tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * @param tag
     *            the tag to set
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * @return the chunk
     */
    public String getChunk() {
        return chunk;
    }

    /**
     * @param chunk
     *            the chunk to set
     */
    public void setChunk(String chunk) {
        this.chunk = chunk;
    }

    @Override
    public String toString() {
        return chunk + "/" + tag;
    }

}
