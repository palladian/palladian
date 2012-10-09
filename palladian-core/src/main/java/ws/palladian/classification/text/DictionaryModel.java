package ws.palladian.classification.text;

import ws.palladian.classification.Categories;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Model;
import ws.palladian.classification.text.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.text.evaluation.FeatureSetting;

/**
 * <p>
 * The model implementation for the {@link PalladianTextClassifier}.
 * </p>
 * 
 * @author David Urbansky
 */
public final class DictionaryModel implements Model {

    private static final long serialVersionUID = 1L;

    private Dictionary dictionary;

    /** A classifier classifies to certain categories. */
//    private Categories categories;

    /**
     * Configurations for the classification type ({@link ClassificationTypeSetting.SINGLE},
     * {@link ClassificationTypeSetting.HIERARCHICAL}, or {@link ClassificationTypeSetting.TAG}).
     */
    private ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();

    /** The feature settings which should be used by the text classifier. */
    private FeatureSetting featureSetting = new FeatureSetting();

    public DictionaryModel() {
        dictionary = new Dictionary();
    }

    public void updateWord(String key, String name, Double value) {
        dictionary.updateWord(key, name, value);
    }

    public CategoryEntries get(String key) {
        return dictionary.get(key);
    }

    public Categories getCategories() {
        return dictionary.getCategories();
    }

    public ClassificationTypeSetting getClassificationTypeSetting() {
        return classificationTypeSetting;
    }

    public void setClassificationTypeSetting(ClassificationTypeSetting classificationTypeSetting) {
        this.classificationTypeSetting = classificationTypeSetting;
    }

    public FeatureSetting getFeatureSetting() {
        return featureSetting;
    }

    public void setFeatureSetting(FeatureSetting featureSetting) {
        this.featureSetting = featureSetting;
    }

    public int size() {
        return dictionary.size();
    }

    @Override
    public String toString() {
        return "DictionaryModel [dictionarySize=" + dictionary.size() + ", categories=" + dictionary.getCategories() + "]";
    }

    public String toDictionaryCsv() {
        return dictionary.toCsv();
    }

}