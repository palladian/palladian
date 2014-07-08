package ws.palladian.classification.text;

import java.util.Collection;

import ws.palladian.classification.text.DictionaryModel.TermCategoryEntries;
import ws.palladian.helper.collection.Factory;

/**
 * Builder for a {@link DictionaryModel}.
 * 
 * @author pk
 * 
 */
public interface DictionaryBuilder extends Factory<DictionaryModel> {

    /**
     * A strategy for pruning the dictionary model
     * 
     * @author pk
     */
    interface PruningStrategy {
        /**
         * Decide, whether to remove the given entries.
         * 
         * @param entries The entries.
         * @return <code>true</code> in case the entries should be removed from the model, else <code>false</code>.
         */
        boolean remove(TermCategoryEntries entries);
    }

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
     * Adds a {@link PruningStrategy} to this builder.
     * 
     * @param strategy The pruning strategy, not <code>null</code>.
     * @return The builder instance.
     */
    DictionaryBuilder addPruningStrategy(PruningStrategy strategy);

    /**
     * Adds the content of a given {@link DictionaryModel}.
     * 
     * @param model The dictionary model to add, not <code>null</code>.
     * @return The builder instance.
     */
    DictionaryBuilder addDictionary(DictionaryModel model);

}
