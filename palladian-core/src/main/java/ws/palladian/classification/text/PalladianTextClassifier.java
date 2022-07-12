package ws.palladian.classification.text;

import org.apache.commons.lang3.Validate;
import ws.palladian.core.*;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.value.TextValue;
import ws.palladian.helper.ProgressMonitor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * This text classifier builds a dictionary from a pre-categorized list of text documents which can then be used to
 * categorize new, uncategorized text documents. During learning, a weighted term look up table is created, to learn how
 * probable each n-gram is for a given category. This look up table is used during classification.
 *
 * <p>
 * This classifier won the first Research Garden competition where the goal was to classify product descriptions into
 * eight different categories. See <a href=
 * "https://web.archive.org/web/20120122045250/http://www.research-garden.de/c/document_library/get_file?uuid=e60fa8da-4f76-4e64-a692-f74d5ffcf475&amp;groupId=10137"
 * >press release</a> (via archive.org).
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public class PalladianTextClassifier extends AbstractLearner<DictionaryModel> implements Classifier<DictionaryModel> {

    /**
     * <p>
     * Implementations of this interface allow to influence the scoring during classification. Usually (i.e. in cases
     * where you cannot thoroughly verify, that a different scoring formula works better, using training and test data)
     * you should stick to the provided {@link DefaultScorer}, which has proven to perform well in general
     * classification cases through our extensive experiments. The available attributes for scoring are depicted for a
     * sample dictionary below:
     * <p>
     * <img src="doc-files/DictionaryExample.png" />
     *
     * @author Philipp Katz
     */
    public interface Scorer {
        /**
         * Score a term-category-pair in a document which has to be classified.
         *
         * @param term              The term (this value usually has no influence on the scoring, but is provided for debugging
         *                          purposes).
         * @param category          The category (for debugging purposes, see above).
         * @param termCategoryCount The absolute count of the term in the current category, as extracted from the
         *                          dictionary model.
         * @param dictCount         The absolute count of documents in the dictionary which contain the term.
         * @param docCount          The absolute count of the term in the current document.
         * @param categorySum       The absolute count sum of all terms in the current category.
         * @param numUniqTerms      The total number of unique terms in the dictionary model.
         * @param numDocs           The total number of documents in the dictionary model.
         * @param numTerms          The total number of terms in the dictionary model.
         * @return A score for the term-category pair, greater/equal zero.
         */
        double score(String term, String category, int termCategoryCount, int dictCount, int docCount, int categorySum, int numUniqTerms, int numDocs, int numTerms);

        /**
         * (Re)score a category, after all term-category-pairs have been scored.
         *
         * @param category            The category.
         * @param summedTermScore     The determined term score (see
         *                            {@link #score(String, String, int, int, int, int, int, int)} ).
         * @param categoryProbability The probability in the dictionary for the current category.
         * @param matched             Whether any terms matched during term-category-scoring (in case this is <code>false</code>,
         *                            all term scores are zero).
         * @return A score for the category, greater/equal zero.
         */
        double scoreCategory(String category, double summedTermScore, double categoryProbability, boolean matched);

        /**
         * Indicate, whether to call {@link #score(String, String, int, int, int, int, int, int, int)} in case,
         * termCategoryCount is <code>0</code>. For the default scorer this is not necessary. Bayes scorer on the other
         * requires this, in case e.g. smoothing is activated.
         *
         * @return <code>true</code> to call the score method for zero termCategoryCounts, <code>false</code> otherwise.
         */
        boolean scoreNonMatches();
    }

    /**
     * Default scorer implementation which scores a term-category-pair using the squared term-category probability.
     *
     * @author Philipp Katz
     */
    public static class DefaultScorer implements Scorer {
        /** @deprecated Use the {@link PalladianTextClassifier#DEFAULT_SCORER} instead. */
        public DefaultScorer() {
            // no op.
        }

        @Override
        public double score(String term, String category, int termCategoryCount, int dictCount, int docCount, int categorySum, int numUniqTerms, int numDocs, int numTerms) {
            if (dictCount == 0) { // prevent zero division
                return 0;
            }
            double termCategoryProbability = (double) termCategoryCount / dictCount;
            return termCategoryProbability * termCategoryProbability;
        }

        @Override
        public double scoreCategory(String category, double categoryScore, double categoryProbability, boolean matched) {
            // If we have a category weight by matching terms from the document, use them to create the probability
            // distribution. Else wise return the prior probability distribution of the categories.
            return matched ? categoryScore : categoryProbability;
        }

        @Override
        public boolean scoreNonMatches() {
            return false;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    public static final String VECTOR_TEXT_IDENTIFIER = "text";

    public static final Scorer DEFAULT_SCORER = new DefaultScorer();

    private final DictionaryBuilder dictionaryBuilder;

    private final FeatureSetting featureSetting;

    private final Scorer scorer;

    private final Function<String, Iterator<String>> preprocessor;

    /**
     * <p>
     * Creates a new {@link PalladianTextClassifier} using the given configuration for feature extraction.
     *
     * @param featureSetting The configuration for feature extraction, not <code>null</code>.
     */
    public PalladianTextClassifier(FeatureSetting featureSetting) {
        this(featureSetting, new DefaultScorer());
    }

    /**
     * <p>
     * Creates a new {@link PalladianTextClassifier} using the specified builder for creating the dictionary and the
     * given feature setting.
     *
     * @param featureSetting    The configuration for feature extraction, not <code>null</code>.
     * @param dictionaryBuilder The builder for creating the model, not <code>null</code>.
     */
    public PalladianTextClassifier(FeatureSetting featureSetting, DictionaryBuilder dictionaryBuilder) {
        Validate.notNull(dictionaryBuilder, "dictionaryBuilder must not be null");
        Validate.notNull(featureSetting, "featureSetting must not be null");
        this.dictionaryBuilder = dictionaryBuilder;
        this.dictionaryBuilder.setFeatureSetting(featureSetting);
        this.featureSetting = featureSetting;
        this.scorer = new DefaultScorer();
        this.preprocessor = new Preprocessor(featureSetting);
    }

    /**
     * <p>
     * Creates a new {@link PalladianTextClassifier} using the specified builder for creating the dictionary and the
     * given scorer.
     *
     * @param featureSetting The configuration for feature extraction, not <code>null</code>.
     * @param scorer         The scorer to use, not <code>null</code>.
     */
    public PalladianTextClassifier(FeatureSetting featureSetting, Scorer scorer) {
        Validate.notNull(featureSetting, "featureSetting must not be null");
        Validate.notNull(scorer, "scorer must not be null");
        Validate.notNull(scorer, "scorer must not be null");
        this.dictionaryBuilder = new DictionaryTrieModel.Builder();
        this.dictionaryBuilder.setFeatureSetting(featureSetting);
        this.featureSetting = featureSetting;
        this.scorer = scorer;
        this.preprocessor = new Preprocessor(featureSetting);
    }

    @Override
    public DictionaryModel train(Dataset dataset) {
        Validate.notNull(dataset, "dataset must not be null");
//        long size = dataset.size();
//        ProgressMonitor progressMonitor = new ProgressMonitor(size, 0.1, "Training text classifier");
        for (Instance instance : dataset) {
            String targetClass = instance.getCategory();
            TextValue textValue = (TextValue) instance.getVector().get(VECTOR_TEXT_IDENTIFIER);
            String text = textValue.getText();

            Iterator<String> iterator = preprocessor.apply(text);
            Set<Entry<String, Integer>> terms = featureSetting.getTermSelector().getTerms(iterator, featureSetting.getMaxTerms());
            dictionaryBuilder.addDocument(terms.stream().map(Entry::getKey).collect(Collectors.toSet()), targetClass, instance.getWeight());
//            progressMonitor.incrementAndPrintProgress();
        }
        return dictionaryBuilder.create();
    }

    @Override
    public CategoryEntries classify(FeatureVector featureVector, DictionaryModel model) {
        Validate.notNull(featureVector, "featureVector must not be null");
        Validate.notNull(model, "model must not be null");
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
        TextValue textValue = (TextValue) featureVector.get(VECTOR_TEXT_IDENTIFIER);
        Iterator<String> iterator = preprocessor.apply(textValue.getText());
        Set<Entry<String, Integer>> termCounts = featureSetting.getTermSelector().getTerms(iterator, featureSetting.getMaxTerms());
        final CategoryEntries termSums = model.getTermCounts();
        final int numUniqueTerms = model.getNumUniqTerms();
        final int numDocs = model.getNumDocuments();
        final int numTerms = model.getNumTerms();
        final boolean scoreNonMatches = scorer.scoreNonMatches();
        final Set<String> matchedCategories = new HashSet<>();

        for (Entry<String, Integer> termCount : termCounts) {
            String term = termCount.getKey();
            CategoryEntries categoryEntries = model.getCategoryEntries(term);
            int docCount = termCount.getValue();
            int dictCount = categoryEntries.getTotalCount();
            for (Category category : categoryEntries) {
                String categoryName = category.getName();
                int categorySum = termSums.getCount(categoryName);
                int count = category.getCount();
                double score = scorer.score(term, categoryName, count, dictCount, docCount, categorySum, numUniqueTerms, numDocs, numTerms);
                builder.add(categoryName, score);
                if (scoreNonMatches) {
                    matchedCategories.add(categoryName);
                }
            }
            // do the scoring for the non-matches; i.e. term-category combinations with count zero;
            // this is necessary e.g. for smoothing during the Bayes scoring. It's only done in case it is explicitly
            // requested by Scorer#scoreNonMatches, because it takes time (especially with lots of categories).
            if (scoreNonMatches) {
                for (Category category : termSums) {
                    String categoryName = category.getName();
                    if (!matchedCategories.contains(categoryName)) {
                        int categorySum = category.getCount();
                        double score = scorer.score(term, categoryName, 0, dictCount, docCount, categorySum, numUniqueTerms, numDocs, numTerms);
                        builder.add(categoryName, score);
                    }
                }
                matchedCategories.clear();
            }
        }
        boolean matched = builder.getTotalScore() != 0;
        for (Category category : model.getDocumentCounts()) {
            String categoryName = category.getName();
            double termScore = builder.getScore(categoryName);
            double categoryProbability = category.getProbability();
            double newScore = scorer.scoreCategory(categoryName, termScore, categoryProbability, matched);
            builder.set(categoryName, newScore);
        }
        return builder.create();
    }

    public CategoryEntries classify(String text, DictionaryModel model) {
        Validate.notNull(text, "text must not be null");
        Validate.notNull(model, "model must not be null");
        FeatureVector featureVector = new InstanceBuilder().setText(text).create();
        return classify(featureVector, model);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[scorer=" + scorer + ", featureSetting=" + featureSetting + "]";
    }

}
