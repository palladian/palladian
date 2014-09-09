package ws.palladian.extraction.entity.tagger;

import static ws.palladian.extraction.entity.tagger.PalladianNerSettings.LanguageMode.English;
import static ws.palladian.extraction.entity.tagger.PalladianNerSettings.TrainingMode.Complete;
import static ws.palladian.extraction.entity.tagger.PalladianNerSettings.TrainingMode.Sparse;

import java.io.Serializable;

import org.apache.commons.lang3.Validate;

public class PalladianNerSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The language mode, language independent uses more generic regexp to detect entities, while there are more
     * specific ones for English texts.
     * 
     * @author David Urbansky
     * 
     */
    public enum LanguageMode {
        LanguageIndependent, English
    }

    /**
     * The two possible learning modes. Complete requires fully tagged data, sparse needs only some entities tagged in
     * the training file.
     * 
     * @author David Urbansky
     * 
     */
    public enum TrainingMode {
        Complete, Sparse
    }

    // private static final LanguageMode DEFAULT_LANGUAGE_MODE = English;

    // private static final TrainingMode DEFAULT_TRAINING_MODE = Complete;

    /** Whether the tagger should tag URLs. */
    boolean tagUrls = true;

    /** Whether the tagger should tag dates. */
    boolean tagDates = true;

    /** The language mode. */
    final LanguageMode languageMode;

    /** The training mode. */
    private final TrainingMode trainingMode;

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

    // learning features

    boolean removeDates() {
        return languageMode == English;
    }

    boolean removeDateFragments() {
        return languageMode == English;
    }

    boolean removeIncorrectlyTaggedInTraining() {
        return languageMode == English && trainingMode == Complete;
    }

    boolean removeSentenceStartErrorsCaseDictionary() {
        return trainingMode == Sparse;
    }

    boolean switchTagAnnotationsUsingPatterns() {
        return languageMode == English;
    }

    boolean switchTagAnnotationsUsingDictionary() {
        return true;
    }

    boolean unwrapEntities() {
        return languageMode == English;
    }

    boolean unwrapEntitiesWithContext() {
        return languageMode == English;
    }

    boolean retraining() { // XXX isn't this the same property as removeIncorrectlyTaggedInTraining?
        return trainingMode == Complete;
    }

}
