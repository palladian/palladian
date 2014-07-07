package ws.palladian.classification.text;

import java.io.PrintStream;

import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Model;

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
     * @return The feature setting which was used for extracting the features in this model, or <code>null</code> in
     *         case not specified.
     */
    FeatureSetting getFeatureSetting();

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
    int getNumUniqTerms();

    /**
     * @return The number of terms in this model.
     */
    int getNumTerms();

    /**
     * @return The number of distinct categories in this model.
     */
    int getNumCategories();

    /**
     * @return The number of (non-zero) term-category entries in this model.
     */
    int getNumEntries();

    /**
     * @return The number of documents in this model.
     */
    int getNumDocuments();

    /**
     * <p>
     * Get the counts/probabilities for the individual categories based on the documents in the training set. (e.g.
     * there were 10 category "A" documents, 15 category "B" documents during training, this would make a prior
     * probability 10/25=0.4 for category "A").
     * 
     * @return The counts for the trained categories based on the documents.
     */
    CategoryEntries getDocumentCounts();

    /**
     * <p>
     * Get the counts/probabilities for the individual categories based on the terms in the training set, in other
     * words, this represents the count of all terms within the individual categories.
     * 
     * @return The counts for the trained categories measured trough the terms.
     */
    CategoryEntries getTermCounts();

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
    // FIXME this contradicts the idea of the dictionary builder
    int prune(PruningStrategy strategy);

}
