package ws.palladian.extraction.entity.tagger;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.DictionaryModel.DictionaryEntry;
import ws.palladian.extraction.entity.tagger.PalladianNerTrainingSettings.LanguageMode;
import ws.palladian.extraction.entity.tagger.PalladianNerTrainingSettings.TrainingMode;
import ws.palladian.helper.collection.CollectionHelper;

public final class PalladianNerModel implements Serializable {

    /** The serial version id. */
    private static final long serialVersionUID = 2L;

    /** This dictionary contains the entity terms as they are. */
    DictionaryModel entityDictionary;

    /** A list containing the order of likelihood of the concepts. */
    List<String> conceptLikelihoodOrder;

    /** This dictionary contains the n-grams of the entity terms, create by the text classifier. */
    DictionaryModel annotationDictionary;

    /** Context classifier for the left and right context around the annotations. */
    DictionaryModel contextDictionary;

    /** A dictionary of tokens which frequently occur in lower case within the training data. */
    Set<String> lowerCaseDictionary;

    Set<String> leftContexts;

    Set<String> removeAnnotations;

    LanguageMode languageMode;

    TrainingMode trainingMode;

    /** Cache for the case insensitive entity dictionary. */
    transient Set<String> entityValuesCaseInsensitive = null;

    /**
     * @return The tags which are supported by this model.
     */
    public Set<String> getTags() {
        return entityDictionary.getCategories();
    }

    public PalladianNerTaggingSettings getTaggingSettings() {
        return new PalladianNerTaggingSettings(languageMode, trainingMode);
    }

    /**
     * Check (case insensitively), if the given value is contained within the entity dictionary.
     * 
     * @param value
     * @return
     */
    public boolean entityDictionaryContains(String value) {
        if (entityValuesCaseInsensitive == null) {
            Set<String> values = CollectionHelper.newHashSet();
            for (DictionaryEntry entry : entityDictionary) {
                values.add(entry.getTerm().toLowerCase());
            }
            entityValuesCaseInsensitive = values;
        }
        return entityValuesCaseInsensitive.contains(value.toLowerCase());
    }

    @Override
    public String toString() {
        StringBuilder summary = new StringBuilder();
        summary.append("PalladianNerModel [");
        summary.append("Dictionary sizes: ");
        summary.append("annotation:").append(annotationDictionary.getNumUniqTerms()).append(',');
        summary.append("entity:").append(entityDictionary.getNumUniqTerms()).append(',');
        summary.append("context:").append(contextDictionary.getNumUniqTerms()).append(',');
        if (lowerCaseDictionary != null) {
            summary.append("case:").append(lowerCaseDictionary.size()).append(',');
        }
        if (removeAnnotations != null) {
            summary.append("remove:").append(removeAnnotations.size()).append(',');
        }
        summary.append("leftContexts:").append(leftContexts.size()).append(", ");
        summary.append("Tags: ").append(StringUtils.join(getTags(), ','));
        summary.append(']');
        return summary.toString();
    }

}
