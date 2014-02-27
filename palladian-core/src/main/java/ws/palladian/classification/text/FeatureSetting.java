package ws.palladian.classification.text;

import java.io.Serializable;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Save the settings which text features should be used for a classifier. Use the {@link FeatureSettingBuilder} to
 * instantiate.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class FeatureSetting implements Serializable {

    private static final long serialVersionUID = 8129286644101075891L;

    /** The default maximum term length. */
    static final int DEFAULT_MIN_TERM_LENGTH = 3;

    /** The default maximum term length. */
    static final int DEFAULT_MAX_TERM_LENGTH = 20;

    /** The default maximum terms to extract. */
    static final int DEFAULT_MAX_TERMS = 800;

    /** The default minimum n-gram length. */
    static final int DEFAULT_MIN_NGRAM_LENGTH = 4;

    /** The default maximum n-gram length. */
    static final int DEFAULT_MAX_NGRAM_LENGTH = 7;

    public static enum TextFeatureType {
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
    private int minimumTermLength = DEFAULT_MIN_TERM_LENGTH;

    /**
     * The maximum length of a single term, this only applies if {@link textFeatureType} is set to
     * {@link TextFeatureType#WORD_NGRAMS} and {@link maxNGramLength} is 1, that is, only unigrams will be used.
     */
    private int maximumTermLength = DEFAULT_MAX_TERM_LENGTH;

    /**
     * @deprecated Consider using the {@link FeatureSettingBuilder} for better readability.
     */
    @Deprecated
    public FeatureSetting() {
        this(TextFeatureType.CHAR_NGRAMS, DEFAULT_MIN_NGRAM_LENGTH, DEFAULT_MAX_NGRAM_LENGTH, DEFAULT_MAX_TERMS);
    }

    /**
     * @deprecated Consider using the {@link FeatureSettingBuilder} for better readability.
     */
    @Deprecated
    public FeatureSetting(TextFeatureType textFeatureType, int minNGramLength, int maxNGramLength) {
        this(textFeatureType, minNGramLength, maxNGramLength, DEFAULT_MAX_TERMS);
    }

    /**
     * @deprecated Consider using the {@link FeatureSettingBuilder} for better readability.
     */
    @Deprecated
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

    FeatureSetting(FeatureSettingBuilder builder) {
        this.textFeatureType = builder.featureType;
        this.maxTerms = builder.maxTerms;
        this.minNGramLength = builder.minNGramLength;
        this.maxNGramLength = builder.maxNGramLength;
        this.minimumTermLength = builder.minTermLength;
        this.maximumTermLength = builder.maxTermLength;
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

    /**
     * @return <code>true</code> in case word unigrams are extracted, <code>false</code> otherwise.
     */
    public boolean isWordUnigrams() {
        return textFeatureType == TextFeatureType.WORD_NGRAMS && minNGramLength == 1 & maxNGramLength == 1;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FeatureSetting [");
        builder.append(textFeatureType).append(", ");
        builder.append("nGramLength=").append(minNGramLength);
        if (maxNGramLength > minNGramLength) {
            builder.append("...").append(maxNGramLength);
        }
        builder.append(", ");
        if (isWordUnigrams()) {
            builder.append("termLength=").append(minimumTermLength);
            builder.append("...").append(maximumTermLength).append(", ");
        }
        builder.append("maxTerms=").append(maxTerms);
        builder.append("]");
        return builder.toString();
    }

}