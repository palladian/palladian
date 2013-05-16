package ws.palladian.extraction.feature;

import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * A {@link PipelineProcessor} for removing tokens based on the length from a pre-tokenized text. This means, the
 * documents to be processed by this class must be processed by a Tokenizer in advance, supplying
 * Tokenizer.PROVIDED_FEATURE annotations.
 * </p>
 * 
 * @author Philipp Katz
 * 
 */
public final class LengthTokenRemover extends AbstractTokenRemover {

    private final int minLength;
    private final int maxLength;

    /**
     * <p>
     * Creates a new {@link LengthTokenRemover} with the specified minimum and maximum lengths.
     * </p>
     * 
     * @param minLength Minimum length for a token to be accepted.
     * @param maxLength Maximum length for a token to be accepted.
     */
    public LengthTokenRemover(int minLength, int maxLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    /**
     * <p>
     * Creates a new {@link LengthTokenRemover} with the specified minimum length and no limitation for the maximum
     * length.
     * </p>
     * 
     * @param minLength Minimum length for a token to be accepted.
     */
    public LengthTokenRemover(int minLength) {
        this(minLength, Integer.MAX_VALUE);
    }

    @Override
    protected boolean remove(PositionAnnotation annotation) {
        int length = annotation.getValue().length();
        return length < minLength || length > maxLength;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LengthTokenRemover [minLength=");
        builder.append(minLength);
        builder.append(", maxLength=");
        builder.append(maxLength);
        builder.append("]");
        return builder.toString();
    }

}
