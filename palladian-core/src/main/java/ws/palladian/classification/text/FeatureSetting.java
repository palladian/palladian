package ws.palladian.classification.text;

import java.io.Serializable;

/**
 * <p>Save the settings which text features should be used for a classifier.</p>
 * 
 * @author David Urbansky
 * 
 */
public class FeatureSetting implements Serializable {

    private static final long serialVersionUID = 8129286644101075891L;

    /** Use n-Grams on a character level. */
    public static final int CHAR_NGRAMS = 1;

    /** Use n-Grams on a word level. */
    public static final int WORD_NGRAMS = 2;

    /** Set which n-Gram type should be used. */
    private int textFeatureType = CHAR_NGRAMS;

    /** The maximum number of terms that should be used per document. */
    private int maxTerms = 800;

    /** Minimum n-gram length. */
    private int minNGramLength = 4;

    /** Maximum n-gram length. */
    private int maxNGramLength = 7;

    /**
     * The minimum length of a single term, this only applies if {@link textFeatureType} is set to {@link WORD_NGRAMS}
     * and {@link maxNGramLength} is 1, that is, only unigrams will be used.
     */
    private int minimumTermLength = 3;

    /**
     * The maximum length of a single term, this only applies if {@link textFeatureType} is set to {@link WORD_NGRAMS}
     * and {@link maxNGramLength} is 1, that is, only unigrams will be used.
     */
    private int maximumTermLength = 20;
    
    public int getTextFeatureType() {
        return textFeatureType;
    }

    public void setTextFeatureType(int textFeatureType) {
        this.textFeatureType = textFeatureType;
    }

    public void setMaxTerms(int maxTerms) {
        this.maxTerms = maxTerms;
    }

    public int getMaxTerms() {
        return maxTerms;
    }

    public int getMinNGramLength() {
        return minNGramLength;
    }

    public void setMinNGramLength(int minNGramLength) {
        this.minNGramLength = minNGramLength;
    }

    public int getMaxNGramLength() {
        return maxNGramLength;
    }

    public void setMaxNGramLength(int maxNGramLength) {
        this.maxNGramLength = maxNGramLength;
    }

    /**
     * Set the maximum length of a single term, this only applies if {@link textFeatureType} is set to
     * {@link WORD_NGRAMS} and {@link maxNGramLength} is 1, that is, only unigrams will be used.
     */
    public void setMaximumTermLength(int maximumTermLength) {
        this.maximumTermLength = maximumTermLength;
    }

    public int getMaximumTermLength() {
        return maximumTermLength;
    }

    /**
     * Set the minimum length of a single term, this only applies if {@link textFeatureType} is set to
     * {@link WORD_NGRAMS} and {@link maxNGramLength} is 1, that is, only unigrams will be used.
     */
    public void setMinimumTermLength(int minimumTermLength) {
        this.minimumTermLength = minimumTermLength;
    }

    public int getMinimumTermLength() {
        return minimumTermLength;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FeatureSetting [maxNGramLength=");
        builder.append(maxNGramLength);
        builder.append(", maxTerms=");
        builder.append(maxTerms);
        builder.append(", maximumTermLength=");
        builder.append(maximumTermLength);
        builder.append(", minNGramLength=");
        builder.append(minNGramLength);
        builder.append(", minimumTermLength=");
        builder.append(minimumTermLength);
        builder.append(", textFeatureType=");
        builder.append(textFeatureType);
        builder.append("]");
        return builder.toString();
    }

}