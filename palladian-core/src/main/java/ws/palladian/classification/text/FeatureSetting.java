package ws.palladian.classification.text;

import java.io.Serializable;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Save the settings which text features should be used for a classifier.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class FeatureSetting implements Serializable {

    private static final long serialVersionUID = 8129286644101075891L;

    public enum TextFeatureType {
        /** Use n-Grams on a character level. */
        CHAR_NGRAMS,
        /** Use n-Grams on a word level. */
        WORD_NGRAMS;
    }

    /** Set which n-Gram type should be used. */
    private final TextFeatureType textFeatureType;

    /** The maximum number of terms that should be used per document. */
    private final int maxTerms;

    /** Minimum n-gram length. */
    private final int minNGramLength;

    /** Maximum n-gram length. */
    private final int maxNGramLength;

    /**
     * The minimum length of a single term, this only applies if {@link textFeatureType} is set to
     * {@link TextFeatureType#WORD_NGRAMS} and {@link maxNGramLength} is 1, that is, only unigrams will be used.
     */
    private final int minimumTermLength = 3;

    /**
     * The maximum length of a single term, this only applies if {@link textFeatureType} is set to
     * {@link TextFeatureType#WORD_NGRAMS} and {@link maxNGramLength} is 1, that is, only unigrams will be used.
     */
    private final int maximumTermLength = 20;

    public FeatureSetting() {
        this(TextFeatureType.CHAR_NGRAMS, 4, 7, 800);
    }

    public FeatureSetting(TextFeatureType textFeatureType, int minNGramLength, int maxNGramLength) {
        this(textFeatureType, minNGramLength, maxNGramLength, 800);
    }

    public FeatureSetting(TextFeatureType textFeatureType, int minNGramLength, int maxNGramLength, int maxTerms) {
        Validate.notNull(textFeatureType, "textFeatureType must not be null");
        Validate.isTrue(minNGramLength > 0);
        Validate.isTrue(maxNGramLength >= minNGramLength);
        Validate.isTrue(maxTerms > 0);

        this.textFeatureType = textFeatureType;
        this.minNGramLength = minNGramLength;
        this.maxNGramLength = maxNGramLength;
        this.maxTerms = maxTerms;
    }

    public TextFeatureType getTextFeatureType() {
        return textFeatureType;
    }

    public int getMaxTerms() {
        return maxTerms;
    }

    public int getMinNGramLength() {
        return minNGramLength;
    }

    public int getMaxNGramLength() {
        return maxNGramLength;
    }

    public int getMaximumTermLength() {
        return maximumTermLength;
    }

    public int getMinimumTermLength() {
        return minimumTermLength;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FeatureSetting [");
        builder.append("featureType=").append(textFeatureType).append(", ");
        builder.append("nGramLength=").append(minNGramLength);
        if (maxNGramLength > minNGramLength) {
            builder.append("...").append(maxNGramLength);
        }
        builder.append(", ");
        builder.append("termLength=").append(minimumTermLength);
        builder.append("...").append(maximumTermLength).append(", ");
        builder.append("maxTerms=").append(maxTerms);
        builder.append("]");
        return builder.toString();
    }

}