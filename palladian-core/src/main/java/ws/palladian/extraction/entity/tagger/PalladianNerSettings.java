package ws.palladian.extraction.entity.tagger;

import static ws.palladian.extraction.entity.tagger.PalladianNerSettings.LanguageMode.English;
import static ws.palladian.extraction.entity.tagger.PalladianNerSettings.TrainingMode.Complete;
import static ws.palladian.extraction.entity.tagger.PalladianNerSettings.TrainingMode.Sparse;

import java.io.Serializable;

import org.apache.commons.lang3.Validate;

/**
 * @author David Urbansky
 * @author Philipp Katz
 */
public class PalladianNerSettings implements Serializable {

    private static final long serialVersionUID = 1L;

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

    /** The training mode. */
    private final TrainingMode trainingMode;

    /** The language mode. */
    private final LanguageMode languageMode;

    /** Whether the tagger should tag URLs. */
    private boolean tagUrls = true;

    /** Whether the tagger should tag dates. */
    private boolean tagDates = true;

    public PalladianNerSettings(LanguageMode languageMode, TrainingMode trainingMode) {
        Validate.notNull(languageMode, "languageMode must not be null");
        Validate.notNull(trainingMode, "trainingMode must not be null");
        this.languageMode = languageMode;
        this.trainingMode = trainingMode;
    }

    public void setTagUrls(boolean tagUrls) {
        this.tagUrls = tagUrls;
    }

    public void setTagDates(boolean tagDates) {
        this.tagDates = tagDates;
    }

    public LanguageMode getLanguageMode() {
        return languageMode;
    }

    // learning features

    boolean isRemoveDates() {
        return languageMode == English;
    }

    boolean isRemoveDateFragments() {
        return languageMode == English;
    }

    boolean isRemoveIncorrectlyTaggedInTraining() {
        return languageMode == English && trainingMode == Complete;
    }

    boolean isRemoveSentenceStartErrorsCaseDictionary() {
        return trainingMode == Sparse;
    }

    boolean isSwitchTagAnnotationsUsingPatterns() {
        return languageMode == English;
    }

    boolean isSwitchTagAnnotationsUsingDictionary() {
        return true;
    }

    boolean isUnwrapEntities() {
        return languageMode == English;
    }

    boolean isUnwrapEntitiesWithContext() {
        return languageMode == English;
    }

    boolean isRetraining() { // XXX isn't this the same property as removeIncorrectlyTaggedInTraining?
        return trainingMode == Complete;
    }

    public boolean isTagUrls() {
        return tagUrls;
    }

    public boolean isTagDates() {
        return tagDates;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PalladianNerSettings [trainingMode=");
        builder.append(trainingMode);
        builder.append(", languageMode=");
        builder.append(languageMode);
        builder.append(", tagUrls=");
        builder.append(tagUrls);
        builder.append(", tagDates=");
        builder.append(tagDates);
        builder.append("]");
        return builder.toString();
    }

}
