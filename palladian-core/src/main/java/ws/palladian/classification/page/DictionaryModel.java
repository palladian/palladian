package ws.palladian.classification.page;

import ws.palladian.classification.Categories;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Dictionary;
import ws.palladian.classification.Model;

/**
 * <p>
 * The model implementation for the {@link DictionaryClassifier}.
 * </p>
 * 
 * @author David Urbansky
 */
public final class DictionaryModel implements Model {

    private static final long serialVersionUID = 1L;

    private Dictionary dictionary;

    /** A classifier classifies to certain categories. */
    private Categories categories;

    public DictionaryModel() {
        dictionary = new Dictionary("");
    }

    public void updateWord(String key, String name, Double value) {
        dictionary.updateWord(key, name, value);
        categories = dictionary.getCategories();
    }

    public CategoryEntries get(String key) {
        return dictionary.get(key);
    }

    public Categories getCategories() {
        return categories;
    }

}
