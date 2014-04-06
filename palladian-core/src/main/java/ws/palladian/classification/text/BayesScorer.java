package ws.palladian.classification.text;

import static java.lang.Math.log;

import java.util.Arrays;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.PalladianTextClassifier.Scorer;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * Naive Bayes scorer. For more general information about Naive Bayes for text classification, see e.g.
 * "<a href="http://nlp.stanford.edu/IR-book/">An Introduction to Information Retrieval</a>"; Christopher D. Manning;
 * Prabhakar Raghavan; Hinrich Schütze; 2009, chapter 13 (pp. 253).
 * 
 * @author pk
 */
public final class BayesScorer implements Scorer {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BayesScorer.class);

    public static enum Options {
        /** Enable Laplace smoothing. */
        LAPLACE,
        /** Use prior class probability. */
        PRIORS,
        /** Use tf-idf frequencies; transform each term's count to a tf-idf value. */
        FREQUENCIES,
        /**
         * Use complement classes for prediction (see "<a href="http://people.csail.mit.edu/jrennie/papers/icml03-nb.pdf
         * ">Tackling the Poor Assumptions of Naive Bayes Text Classifiers</a>";
         * Jason D. M. Rennie; Lawrence Shih; Jaime Teevan; David R. Karger; 2003). This way, not the term counts in the
         * regarded class, but the counts from all other classes are regarded for each class prediction. This leads to
         * better classification accuracy.
         */
        COMPLEMENT
    }

    private final boolean laplace;

    private final boolean prior;

    private final boolean frequencies;

    private final boolean complement;

    private final Options[] options;

    public BayesScorer(Options... options) {
        Validate.notNull(options, "options must not be null");
        this.options = options;
        Set<Options> temp = CollectionHelper.newHashSet(options);
        this.laplace = temp.contains(Options.LAPLACE);
        this.prior = temp.contains(Options.PRIORS);
        this.frequencies = temp.contains(Options.FREQUENCIES);
        this.complement = temp.contains(Options.COMPLEMENT);
    }

    @Override
    public double score(String term, String category, int termCategoryCount, int dictCount, int docCount, int categorySum,
            int numUniqTerms, int numDocs, int numTerms) {
        int numerator = (complement ? dictCount - termCategoryCount : termCategoryCount) + (laplace ? 1 : 0);
        int denominator = (complement ? numTerms - categorySum : categorySum) + (laplace ? numTerms : 0);
        if (numerator == 0 || denominator == 0) {
            return 0;
        }
        double weight;
        if (frequencies) { // gives minimal improvement
            double idf = log((numDocs + 1) / (dictCount + 1));
            weight = log(docCount + 1) * idf;
        } else {
            weight = docCount;
        }
        double score = weight * log((double)numerator / denominator);
        LOGGER.trace("({},{}) ({}/{})^{} = {}", term, category, numerator, denominator, docCount, score);
        return score;
    }

    @Override
    public double scoreCategory(String category, double summedTermScore, double categoryProbability, boolean matched) {
        double score = (complement ? -1 : 1) * summedTermScore + (prior ? log(categoryProbability) : 0);
        LOGGER.trace("{}: {}·{}={}", category, categoryProbability, summedTermScore, score);
        return score;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append(getClass().getSimpleName());
        if (options.length > 0) {
            builder.append(" ").append(Arrays.toString(options));
        }
        return builder.toString();
    }

}
