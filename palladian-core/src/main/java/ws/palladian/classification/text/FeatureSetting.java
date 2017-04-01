package ws.palladian.classification.text;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.constants.Language;

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

    private static final long serialVersionUID = 8747272894244244525L;

    /** Name of the key for maxTermLength when creating a map. */
    public static final String PROPERTY_MAX_TERM_LENGTH = "maxTermLength";

    /** Name of the key for minTermLength when creating a map. */
    public static final String PROPERTY_MIN_TERM_LENGTH = "minTermLength";

    /** Name of the key for maxNGramLength when creating a map. */
    public static final String PROPERTY_MAX_N_GRAM_LENGTH = "maxNGramLength";

    /** Name of the key for minNGramLength when creating a map. */
    public static final String PROPERTY_MIN_N_GRAM_LENGTH = "minNGramLength";

    /** Name of the key for maxTerms when creating a map. */
    public static final String PROPERTY_MAX_TERMS = "maxTerms";

    /** Name of the key for textFeatureType when creating a map. */
    public static final String PROPERTY_TEXT_FEATURE_TYPE = "textFeatureType";

    /** Name of the key for caseSensitive switch when creating a map. */
    public static final String PROPERTY_CASE_SENSITIVE = "caseSensitive";

    /** Name of the key for characterPadding switch when creating a map. */
    public static final String PROPERTY_CHARACTER_PADDING = "characterPadding";

    /** Name of the key for stemming setting. */
    public static final String PROPERTY_STEM = "stem";

    /** Name of the key for stopword removal setting. */
    public static final String PROPERTY_REMOVE_STOPWORDS = "removeStopwords";

    /** Name of the key for langauge setting. */
    public static final String PROPERTY_LANGUAGE = "language";
    
    public static final String PROPERTY_CREATE_SKIP_GRAMS = "createSkipGrams";

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

    /** The default value for case sensitive switch. */
    static final boolean DEFAULT_CASE_SENSITIVE = false;

    /** The default value for character padding. */
    static final boolean DEFAULT_CHARACTER_PADDING = false;

    static final boolean DEFAULT_STEM = false;

    static final boolean DEFAULT_REMOVE_STOPWORDS = false;

    public static final Language DEFAULT_LANGUAGE = Language.ENGLISH;
    
    static final boolean DEFAULT_CREATE_SKIP_GRAMS = false;

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
    private int minimumTermLength = DEFAULT_MIN_TERM_LENGTH;

    /**
     * The maximum length of a single term, this only applies if {@link textFeatureType} is set to
     * {@link TextFeatureType#WORD_NGRAMS} and {@link maxNGramLength} is 1, that is, only unigrams will be used.
     */
    private int maximumTermLength = DEFAULT_MAX_TERM_LENGTH;

    /** Indicate, whether the text should be treated case insensitively or not. */
    private boolean caseSensitive = DEFAULT_CASE_SENSITIVE;

    /** Indicate, whether to add character padding when using character n-gram features. */
    private boolean characterPadding = DEFAULT_CHARACTER_PADDING;

    /** Indicate, whether to stem the tokens (only in word n-gram mode). */
    private boolean stem = DEFAULT_STEM;

    /** Indicate, whether to remove stop words (only in word n-gram mode). */
    private boolean removeStopwords = DEFAULT_REMOVE_STOPWORDS;

    /** The language used for stemming and stop word removal. */
    private Language language = DEFAULT_LANGUAGE;
    
    /** Whether to create skip grams, e.g. for "the quick brown", a skip gram would be "the brown". */
    private boolean createSkipGrams = false;

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
        this.caseSensitive = builder.caseSensitive;
        this.characterPadding = builder.characterPadding;
        this.stem = builder.stem;
        this.removeStopwords = builder.removeStopwords;
        this.language = builder.language;
        this.createSkipGrams = builder.createSkipGrams;
    }

    /**
     * <p>
     * Create a feature setting from a properties map.
     * 
     * @param properties The properties, not <code>null</code>.
     */
    public FeatureSetting(Map<String, String> properties) {
        Validate.notNull(properties, "properties must not be null");
        this.textFeatureType = TextFeatureType.valueOf(properties.get(PROPERTY_TEXT_FEATURE_TYPE));
        this.maxTerms = Integer.parseInt(properties.get(PROPERTY_MAX_TERMS));
        this.minNGramLength = Integer.parseInt(properties.get(PROPERTY_MIN_N_GRAM_LENGTH));
        this.maxNGramLength = Integer.parseInt(properties.get(PROPERTY_MAX_N_GRAM_LENGTH));
        this.minimumTermLength = Integer.parseInt(properties.get(PROPERTY_MIN_TERM_LENGTH));
        this.maximumTermLength = Integer.parseInt(properties.get(PROPERTY_MAX_TERM_LENGTH));
        String csValue = properties.get(PROPERTY_CASE_SENSITIVE);
        this.caseSensitive = csValue != null ? Boolean.parseBoolean(csValue) : DEFAULT_CASE_SENSITIVE;
        String cpValue = properties.get(PROPERTY_CHARACTER_PADDING);
        this.characterPadding = cpValue != null ? Boolean.parseBoolean(cpValue) : DEFAULT_CHARACTER_PADDING;
        String stValue = properties.get(PROPERTY_STEM);
        this.stem = stValue != null ? Boolean.parseBoolean(stValue) : DEFAULT_STEM;
        String swValue = properties.get(PROPERTY_REMOVE_STOPWORDS);
        this.removeStopwords = swValue != null ? Boolean.parseBoolean(swValue) : DEFAULT_REMOVE_STOPWORDS;
        String langValue = properties.get(PROPERTY_LANGUAGE);
        this.language = langValue != null ? Language.valueOf(langValue) : DEFAULT_LANGUAGE;
        String skipGramsValue = properties.get(PROPERTY_CREATE_SKIP_GRAMS);
        this.createSkipGrams = skipGramsValue != null ? Boolean.parseBoolean(skipGramsValue) : DEFAULT_CREATE_SKIP_GRAMS;
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

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public boolean isCharacterPadding() {
        return characterPadding;
    }

    public boolean isStem() {
        return stem;
    }

    public boolean isRemoveStopwords() {
        return removeStopwords;
    }

    public Language getLanguage() {
        return language;
    }
    
    public boolean isCreateSkipGrams() {
		return createSkipGrams;
	}

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FeatureSetting [");
        builder.append(minNGramLength);
        if (maxNGramLength > minNGramLength) {
            builder.append('-').append(maxNGramLength);
        }
        builder.append('-').append(textFeatureType);
        if (isWordUnigrams()) {
            if (minimumTermLength != DEFAULT_MIN_TERM_LENGTH || maximumTermLength != DEFAULT_MAX_TERM_LENGTH) {
                builder.append(", termLength=").append(minimumTermLength);
                builder.append('-').append(maximumTermLength);
            }
        }
        if (DEFAULT_MAX_TERMS != maxTerms) {
            builder.append(", maxTerms=").append(maxTerms);
        }
        if (isCaseSensitive()) {
            builder.append(", caseSensitive");
        }
        if (isCharacterPadding()) {
            builder.append(", characterPadding");
        }
        if (isStem()) {
            builder.append(", stem");
        }
        if (isRemoveStopwords()) {
            builder.append(", removeStopwords");
        }
        if (getLanguage() != null && textFeatureType == TextFeatureType.WORD_NGRAMS) {
            builder.append(", language=").append(language);
        }
        if (isCreateSkipGrams()) {
        	builder.append(", createSkipGrams");
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * @return The settings as key-value properties (useful e.g. for persistence).
     */
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put(PROPERTY_TEXT_FEATURE_TYPE, textFeatureType.name());
        map.put(PROPERTY_MAX_TERMS, String.valueOf(maxTerms));
        map.put(PROPERTY_MIN_N_GRAM_LENGTH, String.valueOf(minNGramLength));
        map.put(PROPERTY_MAX_N_GRAM_LENGTH, String.valueOf(maxNGramLength));
        map.put(PROPERTY_MIN_TERM_LENGTH, String.valueOf(minimumTermLength));
        map.put(PROPERTY_MAX_TERM_LENGTH, String.valueOf(maximumTermLength));
        map.put(PROPERTY_CASE_SENSITIVE, String.valueOf(caseSensitive));
        map.put(PROPERTY_CHARACTER_PADDING, String.valueOf(characterPadding));
        map.put(PROPERTY_STEM, String.valueOf(stem));
        map.put(PROPERTY_REMOVE_STOPWORDS, String.valueOf(removeStopwords));
        map.put(PROPERTY_LANGUAGE, String.valueOf(language));
        map.put(PROPERTY_CREATE_SKIP_GRAMS, String.valueOf(createSkipGrams));
        return map;
    }
    
    // hashCode + equals

    @Override
    public int hashCode() {
    	return toMap().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FeatureSetting other = (FeatureSetting)obj;
        return toMap().equals(other.toMap());
    }

}
