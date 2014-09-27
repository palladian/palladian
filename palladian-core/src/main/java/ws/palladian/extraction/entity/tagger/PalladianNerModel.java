package ws.palladian.extraction.entity.tagger;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import ws.palladian.classification.text.DictionaryModel;

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
    /** keep the case dictionary from the training data */
    DictionaryModel caseDictionary;

    Set<String> leftContexts;

    Set<String> removeAnnotations;

    PalladianNerSettings settings;

    /**
     * @return The tags which are supported by this model.
     */
    public Set<String> getTags() {
        return entityDictionary.getCategories();
    }

}