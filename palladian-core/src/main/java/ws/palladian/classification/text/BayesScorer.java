package ws.palladian.classification.text;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.PalladianTextClassifier.Scorer;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * Naive Bayes scorer. For more information, see e.g.
 * "<a href="http://nlp.stanford.edu/IR-book/">An Introduction to Information Retrieval</a>"; Christopher D. Manning;
 * Prabhakar Raghavan; Hinrich Schütze; 2009, chapter 13 (pp. 253).
 * 
 * @author pk
 */
public final class BayesScorer implements Scorer {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BayesScorer.class);

    /** BayesScorer without Laplace smoothing. */
    public static final BayesScorer NO_SMOOTHING = new BayesScorer(false, true, false);

    /** BayesScorer with Laplace smoothing. */
    public static final BayesScorer LAPLACE_SMOOTHING = new BayesScorer(true, true, false);

    /** BayesScorer with Laplace smoothing, ignoring prior probabilities. */
    public static final BayesScorer LAPLACE_SMOOTHING_NO_PRIOR = new BayesScorer(true, false, false);

    /** BayesScorer with Laplace smoothing, ignoring prior probabilities, using tf-idf frequencies. */
    public static final BayesScorer LAPLACE_SMOOTHING_NO_PRIOR_FREQ = new BayesScorer(true, false, true);

    /** BayesScorer with Laplace smoothing, using tf-idf frequencies. */
    public static final BayesScorer LAPLACE_SMOOTHING_FREQ = new BayesScorer(true, true, true);

    private final boolean laplace;

    private final boolean prior;

    private final boolean frequencies;

    /** Use the predefined singleton constants. */
    private BayesScorer(boolean laplace, boolean prior, boolean frequencies) {
        this.laplace = laplace;
        this.prior = prior;
        this.frequencies = frequencies;
    }

    @Override
    public double score(String term, String category, int termCategoryCount, int dictCount, int docCount,
            int categorySum, int numTerms, int numDocs) {
        int numerator = termCategoryCount + (laplace ? 1 : 0);
        int denominator = categorySum + (laplace ? numTerms : 0);
        if (numerator == 0 || denominator == 0) {
            return 0;
        }
        double weight = docCount;
        if (frequencies) { // gives minimal improvement
            double idf = Math.log((numDocs + 1) / (dictCount + 1));
            weight = Math.log(docCount + 1) * idf;
        }
        double score = weight * Math.log((double)numerator / denominator);
        LOGGER.trace("({},{}) ({}/{})^{} = {}", term, category, numerator, denominator, docCount, score);
        return score;
    }

    @Override
    public double scoreCategory(String category, double summedTermScore, double categoryProbability, boolean matched) {
        double score = summedTermScore + (prior ? Math.log(categoryProbability) : 0);
        LOGGER.trace("{}: {}·{}={}", category, categoryProbability, summedTermScore, score);
        return score;
    }

    @Override
    public String toString() {
        List<String> options = CollectionHelper.newArrayList();
        if (laplace) {
            options.add("laplace");
        }
        if (prior) {
            options.add("prior");
        }
        if (frequencies) {
            options.add("frequencies");
        }
        return "BayesScorer [" + StringUtils.join(options, ',') + "]";
    }

}
