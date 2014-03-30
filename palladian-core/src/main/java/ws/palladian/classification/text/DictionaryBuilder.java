package ws.palladian.classification.text;

import java.util.Collection;

import ws.palladian.helper.collection.Factory;

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

}
