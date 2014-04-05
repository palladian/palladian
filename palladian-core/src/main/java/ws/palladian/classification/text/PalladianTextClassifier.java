package ws.palladian.classification.text;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesBuilder;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Learner;
import ws.palladian.classification.text.DictionaryModel.TermCategoryEntries;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Function;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.Trainable;

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
public class PalladianTextClassifier implements Learner<DictionaryModel>, Classifier<DictionaryModel> {

    /**
     * <p>
     * Implementations of this interface allow to influence the scoring during classification. Usually (i.e. in cases
     * where you cannot thoroughly verify, that a different scoring formula works better, using training and test data)
     * you should stick to the provided {@link DefaultScorer}, which has proven to perform well in general
     * classification cases through our extensive experiments.
     * 
     * @author pk
     */
    public static interface Scorer {
        /**
         * Score a term-category-pair in a document which has to be classified.
         * 
         * @param term The term (this value usually has no influence on the scoring, but is provided for debugging
         *            purposes).
         * @param category The category (for debugging purposes, see above).
         * @param termCategoryCount The absolute count of the term in the current category, as extracted from the
         *            dictionary model.
         * @param dictCount The absolute count of documents in the dictionary which contain the term.
         * @param docCount The absolute count of the term in the current document.
         * @param categorySum The absolute count sum of all terms in the current category.
         * @param numTerms The total number of unique terms in the dictionary model.
         * @return A score for the term-category pair, greater/equal zero.
         */
        double score(String term, String category, int termCategoryCount, int dictCount, int docCount, int categorySum,
                int numTerms);

        /**
         * (Re)score a category, after all term-category-pairs have been scored.
         * 
         * @param category The category.
         * @param termScore The determined term score (see {@link #score(String, String, double, int, int)}).
         * @param categoryProbability The probability in the dictionary for the current category.
         * @param matched Whether any terms matched during term-category-scoring (in case this is <code>false</code>,
         *            all term scores are zero).
         * @return A score for the category, greater/equal zero.
         */
        double scoreCategory(String category, double termScore, double categoryProbability, boolean matched);
    }

    /**
     * Default scorer implementation which scores a term-category-pair using the squared term-category probability.
     * 
     * @author pk
     */
    public static class DefaultScorer implements Scorer {
        @Override
        public double score(String term, String category, int termCategoryCount, int dictCount, int docCount,
                int categoryCount, int numTerms) {
            if (dictCount == 0) {
                return 0;
            }
            double termCategoryProbability = (double)termCategoryCount / dictCount;
            return termCategoryProbability * termCategoryProbability;
        }

        @Override
        public double scoreCategory(String category, double categoryScore, double categoryProbability, boolean matched) {
            // If we have a category weight by matching terms from the document, use them to create the probability
            // distribution. Else wise return the prior probability distribution of the categories.
            return matched ? categoryScore : categoryProbability;
        }
    }

    /**
     * Scorer, which normalizes the result scores by the prior category probability. This may improve classification
     * results for data with skewed class counts.
     * 
     * @author pk
     */
    public static class CategoryEqualizationScorer extends DefaultScorer {
        @Override
        public double scoreCategory(String category, double categoryScore, double categoryProbability, boolean matched) {
            double score = super.scoreCategory(category, categoryScore, categoryProbability, matched);
            return matched ? score / categoryProbability : score;
        }
    }
    
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
     * @param featureSetting The configuration for feature extraction, not <code>null</code>.
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
     * @param scorer The scorer to use, not <code>null</code>.
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
    public DictionaryModel train(Iterable<? extends Trainable> trainables) {
        for (Trainable trainable : trainables) {
            String targetClass = trainable.getTargetClass();
            String content = ((TextDocument)trainable).getContent();
            Iterator<String> iterator = preprocessor.compute(content);
            Set<String> terms = CollectionHelper.newHashSet();
            while (iterator.hasNext() && terms.size() < featureSetting.getMaxTerms()) {
                terms.add(iterator.next());
            }
            dictionaryBuilder.addDocument(terms, targetClass);
        }
        return dictionaryBuilder.create();
    }

    @Override
    public CategoryEntries classify(Classifiable classifiable, DictionaryModel model) {
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder();
        String content = ((TextDocument)classifiable).getContent();
        Iterator<String> iterator = preprocessor.compute(content);
        Bag<String> termCounts = Bag.create();
        while (iterator.hasNext() && termCounts.uniqueItems().size() < featureSetting.getMaxTerms()) {
            termCounts.add(iterator.next());
        }
        
        CategoryEntries termPriors = model.getTermPriors();
        Set<String> categories = model.getCategories();
        int numTerms = model.getNumTerms();

        for (Entry<String, Integer> termCount : termCounts.unique()) {
            String term = termCount.getKey();
            TermCategoryEntries categoryEntries = model.getCategoryEntries(term);
            int docCount = termCount.getValue();
            int dictCount = categoryEntries.getTotalCount();
            Set<String> zeroCountCategories = CollectionHelper.newHashSet(categories);
            for (Category category : categoryEntries) {
                String categoryName = category.getName();
                int categorySum = termPriors.getCount(category.getName());
                double score = scorer.score(term, categoryName, category.getCount(), dictCount, docCount, categorySum,
                        numTerms);
                assert !Double.isInfinite(score) && !Double.isNaN(score) : "score was NaN/infinity";
                builder.add(categoryName, score);
                zeroCountCategories.remove(categoryName);
            }
            // iterate non-matched
            for (String categoryName : zeroCountCategories) {
                int categoryCount = termPriors.getCount(categoryName);
                double score = scorer.score(term, categoryName, 0, dictCount, docCount, categoryCount, numTerms);
                builder.add(categoryName, score);
            }
        }
        boolean matched = builder.getTotalScore() != 0;
        for (Category category : model.getPriors()) {
            String categoryName = category.getName();
            double termScore = builder.getScore(categoryName);
            double categoryProbability = category.getProbability();
            double newScore = scorer.scoreCategory(categoryName, termScore, categoryProbability, matched);
            assert !Double.isInfinite(newScore) && !Double.isNaN(newScore) : "newScore was NaN/infinity";
            builder.set(categoryName, newScore);
        }
        return builder.create();
    }

    public CategoryEntries classify(String text, DictionaryModel model) {
        return classify(new TextDocument(text), model);
    }

}
