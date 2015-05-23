package ws.palladian.extraction.entity.tagger;

import static ws.palladian.classification.text.FeatureSettingBuilder.chars;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.helper.functional.Factory;

/**
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class PalladianNerTrainingSettings {

    public static final class Builder implements Factory<PalladianNerTrainingSettings> {

        private final LanguageMode languageMode;

        private TrainingMode trainingMode = TrainingMode.Complete;

        private boolean equalizeTypeCounts = false;

        private int minDictionaryCount = 1;

        public static Builder english() {
            return new Builder(LanguageMode.English);
        }

        public static Builder languageIndependent() {
            return new Builder(LanguageMode.LanguageIndependent);
        }

        private Builder(LanguageMode languageMode) {
            this.languageMode = languageMode;
        }

        public Builder sparse() {
            this.trainingMode = TrainingMode.Sparse;
            return this;
        }

        public Builder equalizeTypeCounts() {
            if (languageMode != LanguageMode.English) {
                throw new UnsupportedOperationException("Sparse training is only supported for English language mode.");
            }
            this.equalizeTypeCounts = true;
            return this;
        }

        public Builder minDictCount(int minDictionaryCount) {
            Validate.isTrue(minDictionaryCount > 0, "minDictionaryCount must be greater zero");
            this.minDictionaryCount = minDictionaryCount;
            return this;
        }

        @Override
        public PalladianNerTrainingSettings create() {
            return new PalladianNerTrainingSettings(this);
        }

    }

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

    private final int minDictionaryCount;

    private PalladianNerTrainingSettings(Builder builder) {
        this.languageMode = builder.languageMode;
        this.trainingMode = builder.trainingMode;
        this.equalizeTypeCounts = builder.equalizeTypeCounts;
        this.minDictionaryCount = builder.minDictionaryCount;
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

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append("PalladianNerTrainingSettings [");
        toStringBuilder.append(trainingMode);
        toStringBuilder.append(", ");
        toStringBuilder.append(languageMode);
        if (equalizeTypeCounts) {
            toStringBuilder.append(", equalizeTypeCounts");
        }
        if (minDictionaryCount > 1) {
            toStringBuilder.append(", minDictCount=");
            toStringBuilder.append(minDictionaryCount);
        }
        toStringBuilder.append("]");
        return toStringBuilder.toString();
    }

}
