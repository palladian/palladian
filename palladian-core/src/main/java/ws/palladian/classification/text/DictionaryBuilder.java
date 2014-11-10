package ws.palladian.classification.text;

import java.util.Collection;

import ws.palladian.core.CategoryEntries;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Filter;

/**
 * Builder for a {@link DictionaryModel}.
 * 
 * @author pk
 * 
 */
public interface DictionaryBuilder extends Factory<DictionaryModel> {

    /**
     * Set the name of the dictionary.
     * 
     * @param name The name of the dictionary.
     * @return The builder instance.
     */
    DictionaryBuilder setName(String name);

    /**
     * Set the feature extraction setting used for creating this dictionary.
     * 
     * @param featureSetting The feature setting.
     * @return The builder instance.
     */
    DictionaryBuilder setFeatureSetting(FeatureSetting featureSetting);

    /**
     * Add a document to the dictionary.
     * 
     * @param terms The terms extracted from the document, not <code>null</code>.
     * @param category The category of the document, not <code>null</code>.
     * @return The builder instance.
     */
    DictionaryBuilder addDocument(Collection<String> terms, String category);

    /**
     * Add a document to the dictionary.
     * 
     * @param terms The terms extracted from the document, not <code>null</code>.
     * @param category The category of the document, not <code>null</code>.
     * @param weight A weight for the document (values greater one act like adding the document n times).
     * @return The builder instance.
     */
    DictionaryBuilder addDocument(Collection<String> terms, String category, int weight);

    /**
     * Set a {@link Filter} for pruning to this builder. The filter should be applied before invoking the
     * {@link #create()} method.
     * 
     * @param strategy The pruning strategy, not <code>null</code>.
     * @return The builder instance.
     */
    DictionaryBuilder setPruningStrategy(Filter<? super CategoryEntries> strategy);

    /**
     * Adds the content of a given {@link DictionaryModel}.
     * 
     * @param model The dictionary model to add, not <code>null</code>.
     * @return The builder instance.
     */
    DictionaryBuilder addDictionary(DictionaryModel model);

}
