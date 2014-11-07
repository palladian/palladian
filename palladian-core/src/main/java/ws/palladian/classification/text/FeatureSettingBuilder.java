package ws.palladian.classification.text;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.FeatureSetting.TextFeatureType;
import ws.palladian.helper.collection.Factory;

/**
 * <p>
 * Convenience builder for creating a {@link FeatureSetting}, which is used for configuring the
 * {@link PalladianTextClassifier}.
 * </p>
 * 
 * @author pk
 */
public final class FeatureSettingBuilder implements Factory<FeatureSetting> {

    final TextFeatureType featureType;
    int maxTerms = FeatureSetting.DEFAULT_MAX_TERMS;
    int minNGramLength = FeatureSetting.DEFAULT_MIN_NGRAM_LENGTH;
    int maxNGramLength = FeatureSetting.DEFAULT_MAX_NGRAM_LENGTH;
    int minTermLength = FeatureSetting.DEFAULT_MIN_TERM_LENGTH;
    int maxTermLength = FeatureSetting.DEFAULT_MAX_TERM_LENGTH;

    /**
     * <p>
     * Create a new {@link FeatureSettingBuilder} for character n-grams.
     * </p>
     * 
     * @return The builder.
     */
    public static FeatureSettingBuilder chars() {
        return new FeatureSettingBuilder(TextFeatureType.CHAR_NGRAMS);
    }

    /**
     * <p>
     * Create a new {@link FeatureSettingBuilder} for character n-grams.
     * </p>
     * 
     * @param min The minimum n-gram length, must be greater zero.
     * @param max The maximum n-gram length, must be greater/equal min.
     * @return The builder.
     */
    public static FeatureSettingBuilder chars(int min, int max) {
        return new FeatureSettingBuilder(TextFeatureType.CHAR_NGRAMS).nGramLength(min, max);
    }

    /**
     * <p>
     * Create a new {@link FeatureSettingBuilder} for character n-grams.
     * </p>
     * 
     * @param length The n-gram length, must be greater zero.
     * @return The builder.
     */
    public static FeatureSettingBuilder chars(int length) {
        return new FeatureSettingBuilder(TextFeatureType.CHAR_NGRAMS).nGramLength(length);
    }

    /**
     * <p>
     * Create a new {@link FeatureSettingBuilder} for word n-grams.
     * </p>
     * 
     * @return The builder.
     */
    public static FeatureSettingBuilder words() {
        return new FeatureSettingBuilder(TextFeatureType.WORD_NGRAMS);
    }

    /**
     * <p>
     * Create a new {@link FeatureSettingBuilder} for word n-grams.
     * </p>
     * 
     * @param min The minimum n-gram length, must be greater zero.
     * @param max The maximum n-gram length, must be greater/equal min.
     * @return The builder.
     */
    public static FeatureSettingBuilder words(int min, int max) {
        return new FeatureSettingBuilder(TextFeatureType.WORD_NGRAMS).nGramLength(min, max);
    }

    /**
     * <p>
     * Create a new {@link FeatureSettingBuilder} for word n-grams.
     * </p>
     * 
     * @param length The n-gram length, must be greater zero.
     * @return The builder.
     */
    public static FeatureSettingBuilder words(int length) {
        return new FeatureSettingBuilder(TextFeatureType.WORD_NGRAMS).nGramLength(length);
    }

    /**
     * <p>
     * Copy all settings of an existing {@link FeatureSetting}, so that it can be altered afterwards.
     * </p>
     * 
     * @param other The setting to copy, not <code>null</code>.
     * @return The builder.
     */
    public static FeatureSettingBuilder copy(FeatureSetting other) {
        Validate.notNull(other, "other must not be null");
        return new FeatureSettingBuilder(other);
    }

    private FeatureSettingBuilder(TextFeatureType featureType) {
        this.featureType = featureType;
    }

    private FeatureSettingBuilder(FeatureSetting other) {
        this.featureType = other.getTextFeatureType();
        this.maxTerms = other.getMaxTerms();
        this.minNGramLength = other.getMinNGramLength();
        this.maxNGramLength = other.getMaxNGramLength();
        this.minTermLength = other.getMinimumTermLength();
        this.maxTermLength = other.getMaximumTermLength();
    }

    /**
     * <p>
     * Set the maximum number of terms to extract per document.
     * </p>
     * 
     * @param maxTerms The maximum number of terms to extract, greater zero.
     * @return The builder, to allow method chaining.
     */
    public FeatureSettingBuilder maxTerms(int maxTerms) {
        Validate.isTrue(maxTerms > 0, "maxTerms must be greater zero");
        this.maxTerms = maxTerms;
        return this;
    }

    /**
     * <p>
     * Set the lengths of n-grams which are extracted (depending on the {@link #setFeatureType(TextFeatureType)}, the
     * length is either in characters or words).
     * </p>
     * 
     * @param min The minimum n-gram length, must be greater zero.
     * @param max The maximum n-gram length, must be greater/equal min.
     * @return The builder, to allow method chaining.
     * @see #nGramLength(int)
     */
    public FeatureSettingBuilder nGramLength(int min, int max) {
        Validate.isTrue(min > 0, "min must be greater zero");
        Validate.isTrue(max >= min, "max must be greater/equal min");
        this.minNGramLength = min;
        this.maxNGramLength = max;
        return this;
    }

    /**
     * <p>
     * Set the length of n-grams which are extracted (depending on the {@link #setFeatureType(TextFeatureType)}, the
     * length is either in characters or words).
     * </p>
     * 
     * @param length The n-gram length, must be greater zero.
     * @return The builder, to allow method chaining.
     * @see #nGramLength(int, int)
     */
    public FeatureSettingBuilder nGramLength(int length) {
        Validate.isTrue(length > 0, "length must be greater zero");
        this.minNGramLength = length;
        this.maxNGramLength = length;
        return this;
    }

    /**
     * <p>
     * Set the minimum and maximum length of terms to extract. This is only effective in case the
     * {@link #setFeatureType(TextFeatureType)} was set to {@link TextFeatureType#WORD_NGRAMS} and
     * {@link #nGramLength(int)} was set to a value of one (in case it is not, it will be set to one automatically, when
     * invoking this method).
     * </p>
     * 
     * @param min The minimum term length, must be greater zero.
     * @param max The maximum term length, must be greater/equal min.
     * @return The builder, to allow method chaining.
     */
    public FeatureSettingBuilder termLength(int min, int max) {
        if (featureType != TextFeatureType.WORD_NGRAMS) {
            throw new UnsupportedOperationException("This is only supported for WORD_NGRAMS mode.");
        }
        Validate.isTrue(min > 0, "min must be greater zero");
        Validate.isTrue(max >= min, "max must be greater/equal min");
        this.minTermLength = min;
        this.maxTermLength = max;
        nGramLength(1);
        return this;
    }

    @Override
    public FeatureSetting create() {
        return new FeatureSetting(this);
    }

}
