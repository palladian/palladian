package ws.palladian.classification.text;

import java.io.PrintStream;
import java.util.Collection;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Model;

/**
 * A term-category dictionary used for classification with the text classifier.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public interface DictionaryModel extends Model, Iterable<DictionaryModel.TermCategoryEntries> {

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

        /**
         * Decide, whether t oremove the given category.
         * 
         * @param category The category.
         * @return <code>true</code> in case the category should be removed from the model, else <code>false</code>.
         */
        boolean remove(Category category);

    }

    /**
     * Category entries associated with a specific term.
     * 
     * @author pk
     */
    interface TermCategoryEntries extends CategoryEntries {

        /**
         * @return The term.
         */
        String getTerm();

    }

    /** Default, when no name is assigned. */
    final String NO_NAME = "NONAME";

    /**
     * @return The name of this model, or {@value #NO_NAME} in case no name was specified.
     */
    String getName();

    /**
     * Set a name for this model.
     * 
     * @param name The name, not <code>null</code>.
     */
    void setName(String name);

    /**
     * @return The feature setting which was used for extracting the features in this model, or <code>null</code> in
     *         case not specified.
     */
    FeatureSetting getFeatureSetting();

    /**
     * <p>
     * Add a document (represented by a {@link Collection} of terms) to this model.
     * </p>
     * 
     * @param terms The terms from the document, not <code>null</code>.
     * @param category The category of the document, not <code>null</code>.
     */
    void addDocument(Collection<String> terms, String category);

    /**
     * <p>
     * Get the probabilities for the given term in different categories.
     * </p>
     * 
     * @param term The term, not <code>null</code>.
     * @return The category probabilities for the specified term, or an empty {@link TermCategoryEntries} instance, in
     *         case the term is not present in this model. Never <code>null</code>.
     */
    TermCategoryEntries getCategoryEntries(String term);

    /**
     * @return The number of distinct terms in this model.
     */
    int getNumTerms();

    /**
     * @return The number of distinct categories in this model.
     */
    int getNumCategories();

    /**
     * @return The prior probabilities for the trained categories. (e.g. category "A" appeared 10 times, category "B"
     *         appeared 15 times during training would make a prior 10/25=0.4 for category "A").
     */
    CategoryEntries getPriors();

    /**
     * <p>
     * Dump the {@link DictionaryModel} as CSV format to a {@link PrintStream}. This is more memory efficient than
     * invoking {@link #toString()} as the dictionary can be written directly to a file or console.
     * </p>
     * 
     * @param printStream The print stream to which to write the model, not <code>null</code>.
     */
    void toCsv(PrintStream printStream);

    /**
     * <p>
     * Apply a pruning to this dictionary model. To save memory, the pruning is carried out directly in place. If
     * applied correctly, pruning allows to compact a model significantly (thus saving memory and time during
     * classification), while not degrading classification accuracy.
     * 
     * @param strategy The pruning strategy to use.
     * @return The number of removed entries.
     */
    int prune(PruningStrategy strategy);

}
