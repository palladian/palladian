package ws.palladian.extraction.entity.tagger;

import static ws.palladian.extraction.entity.tagger.PalladianNerTrainingSettings.LanguageMode.English;
import static ws.palladian.extraction.entity.tagger.PalladianNerTrainingSettings.TrainingMode.Complete;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.entity.tagger.PalladianNerTrainingSettings.LanguageMode;
import ws.palladian.extraction.entity.tagger.PalladianNerTrainingSettings.TrainingMode;

public final class PalladianNerTaggingSettings {

    private boolean removeDates;
    private boolean removeDateFragments;
    private boolean removeIncorrectlyTaggedInTraining;
    private boolean removeSentenceStartErrorsCaseDictionary;
    private boolean switchTagAnnotationsUsingContext;
    private boolean switchTagAnnotationsUsingDictionary;
    private boolean fixStartErrorsCaseDictionary;
    private boolean unwrapEntities;
    private boolean unwrapEntitiesWithContext;
    private boolean tagUrls;
    private boolean tagDates;

    public PalladianNerTaggingSettings(LanguageMode languageMode, TrainingMode trainingMode) {
        Validate.notNull(languageMode, "languageMode must not be null");
        Validate.notNull(trainingMode, "trainingMode must not be null");
        removeDates = languageMode == English;
        removeDateFragments = languageMode == English;
        removeIncorrectlyTaggedInTraining = languageMode == English && trainingMode == Complete;
        removeSentenceStartErrorsCaseDictionary = true;
        switchTagAnnotationsUsingContext = languageMode == English;
        switchTagAnnotationsUsingDictionary = true;
        fixStartErrorsCaseDictionary = true;
        unwrapEntities = languageMode == English;
        unwrapEntitiesWithContext = languageMode == English;
        tagUrls = false;
        tagDates = false;
    }

    public boolean isRemoveDates() {
        return removeDates;
    }

    public void setRemoveDates(boolean removeDates) {
        this.removeDates = removeDates;
    }

    public boolean isRemoveDateFragments() {
        return removeDateFragments;
    }

    public void setRemoveDateFragments(boolean removeDateFragments) {
        this.removeDateFragments = removeDateFragments;
    }

    public boolean isRemoveIncorrectlyTaggedInTraining() {
        return removeIncorrectlyTaggedInTraining;
    }

    public void setRemoveIncorrectlyTaggedInTraining(boolean removeIncorrectlyTaggedInTraining) {
        this.removeIncorrectlyTaggedInTraining = removeIncorrectlyTaggedInTraining;
    }

    public boolean isRemoveSentenceStartErrorsCaseDictionary() {
        return removeSentenceStartErrorsCaseDictionary;
    }

    public void setRemoveSentenceStartErrorsCaseDictionary(boolean removeSentenceStartErrorsCaseDictionary) {
        this.removeSentenceStartErrorsCaseDictionary = removeSentenceStartErrorsCaseDictionary;
    }

    public boolean isSwitchTagAnnotationsUsingContext() {
        return switchTagAnnotationsUsingContext;
    }

    public void setSwitchTagAnnotationsUsingContext(boolean switchTagAnnotationsUsingContext) {
        this.switchTagAnnotationsUsingContext = switchTagAnnotationsUsingContext;
    }

    public boolean isSwitchTagAnnotationsUsingDictionary() {
        return switchTagAnnotationsUsingDictionary;
    }

    public void setSwitchTagAnnotationsUsingDictionary(boolean switchTagAnnotationsUsingDictionary) {
        this.switchTagAnnotationsUsingDictionary = switchTagAnnotationsUsingDictionary;
    }

    public boolean isFixStartErrorsCaseDictionary() {
        return fixStartErrorsCaseDictionary;
    }

    public void setFixStartErrorsCaseDictionary(boolean fixStartErrorsCaseDictionary) {
        this.fixStartErrorsCaseDictionary = fixStartErrorsCaseDictionary;
    }

    public boolean isUnwrapEntities() {
        return unwrapEntities;
    }

    public void setUnwrapEntities(boolean unwrapEntities) {
        this.unwrapEntities = unwrapEntities;
    }

    public boolean isUnwrapEntitiesWithContext() {
        return unwrapEntitiesWithContext;
    }

    public void setUnwrapEntitiesWithContext(boolean unwrapEntitiesWithContext) {
        this.unwrapEntitiesWithContext = unwrapEntitiesWithContext;
    }

    public boolean isTagUrls() {
        return tagUrls;
    }

    public void setTagUrls(boolean tagUrls) {
        this.tagUrls = tagUrls;
    }

    public boolean isTagDates() {
        return tagDates;
    }

    public void setTagDates(boolean tagDates) {
        this.tagDates = tagDates;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PalladianNerTaggingSettings [removeDates=");
        builder.append(removeDates);
        builder.append(", removeDateFragments=");
        builder.append(removeDateFragments);
        builder.append(", removeIncorrectlyTaggedInTraining=");
        builder.append(removeIncorrectlyTaggedInTraining);
        builder.append(", removeSentenceStartErrorsCaseDictionary=");
        builder.append(removeSentenceStartErrorsCaseDictionary);
        builder.append(", switchTagAnnotationsUsingContext=");
        builder.append(switchTagAnnotationsUsingContext);
        builder.append(", switchTagAnnotationsUsingDictionary=");
        builder.append(switchTagAnnotationsUsingDictionary);
        builder.append(", fixStartErrorsCaseDictionary=");
        builder.append(fixStartErrorsCaseDictionary);
        builder.append(", unwrapEntities=");
        builder.append(unwrapEntities);
        builder.append(", unwrapEntitiesWithContext=");
        builder.append(unwrapEntitiesWithContext);
        builder.append(", tagUrls=");
        builder.append(tagUrls);
        builder.append(", tagDates=");
        builder.append(tagDates);
        builder.append("]");
        return builder.toString();
    }

}
