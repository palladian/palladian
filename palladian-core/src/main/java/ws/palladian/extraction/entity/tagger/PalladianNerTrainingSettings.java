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

    /**
     * n-gram settings for the entity classifier should be tuned, they do not have a big influence on the size of the
     * model (3-5 to 2-8 => 2MB).
     */
    public static final FeatureSetting ANNOTATION_FEATURE_SETTING = chars(4, 8).characterPadding().create();

    /** be careful with the n-gram sizes, they heavily influence the model size. */
    public static final FeatureSetting CONTEXT_FEATURE_SETTING = chars(5).create();

    public static final int WINDOW_SIZE = 40;

    private final TrainingMode trainingMode;

    private final LanguageMode languageMode;

    private final boolean equalizeTypeCounts;

    private int minDictionaryCount = 0;

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
        this.minDictionaryCount = minDictionaryCount;
    }

}
