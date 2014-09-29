package ws.palladian.extraction.entity.tagger;

import static ws.palladian.classification.text.FeatureSettingBuilder.chars;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.FeatureSetting;

/**
 * @author David Urbansky
 * @author Philipp Katz
 */
public class PalladianNerTrainingSettings {

    /**
     * <p>
     * The language mode, language independent uses more generic regular expressions to detect entities, while there are
     * more specific ones for English texts.
     */
    public static enum LanguageMode {
        LanguageIndependent, English
    }

    /**
     * <p>
     * The two possible learning modes. Complete requires fully tagged data, sparse needs only some entities tagged in
     * the training file.
     */
    public static enum TrainingMode {
        Complete, Sparse
    }

    /** Original was 4-8 grams, 5 grams basically gives same result, but the model is significantly smaller. */
    public static final FeatureSetting ANNOTATION_FEATURE_SETTING = chars(5).characterPadding().create();

    /** be careful with the n-gram sizes, they heavily influence the model size. */
    public static final FeatureSetting CONTEXT_FEATURE_SETTING = chars(5).create();

    public static final int WINDOW_SIZE = 40;

    private final TrainingMode trainingMode;

    private final LanguageMode languageMode;

    private final boolean equalizeTypeCounts;

    private int minDictionaryCount = 1;

    public PalladianNerTrainingSettings(LanguageMode languageMode, TrainingMode trainingMode) {
        this(languageMode, trainingMode, false);
    }

    public PalladianNerTrainingSettings(LanguageMode languageMode, TrainingMode trainingMode, boolean equalizeTypeCounts) {
        Validate.notNull(languageMode, "languageMode must not be null");
        Validate.notNull(trainingMode, "trainingMode must not be null");
        this.languageMode = languageMode;
        this.trainingMode = trainingMode;
        this.equalizeTypeCounts = equalizeTypeCounts;
    }

    public TrainingMode getTrainingMode() {
        return trainingMode;
    }

    public LanguageMode getLanguageMode() {
        return languageMode;
    }

    public boolean isEqualizeTypeCounts() {
        return equalizeTypeCounts;
    }

    public int getMinDictionaryCount() {
        return minDictionaryCount;
    }

    public void setMinDictionaryCount(int minDictionaryCount) {
        Validate.isTrue(minDictionaryCount > 0, "minDictionaryCount must be greater zero");
        this.minDictionaryCount = minDictionaryCount;
    }

}
