package ws.palladian.classification.text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.PalladianTextClassifier.DefaultScorer;

public final class BayesScorer extends DefaultScorer {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BayesScorer.class);

    /** BayesScorer without Laplace smoothing. */
    public static final BayesScorer NO_SMOOTHING = new BayesScorer(false);

    /** BayesScorer with Laplace smoothing. */
    public static final BayesScorer LAPLACE_SMOOTHING = new BayesScorer(true);

    private final boolean laplace;

    private BayesScorer(boolean laplace) {
        this.laplace = laplace;
    }

    @Override
    public double score(String term, String category, int termCategoryCount, int dictCount, int docCount,
            int categoryCount, int numTerms) {
        int numerator = termCategoryCount + (laplace ? 1 : 0);
        int denominator = categoryCount + (laplace ? numTerms : 0);
        double score = docCount * Math.log((double)numerator) / (denominator);
        LOGGER.info("({},{}) {}/{}", term, category, numerator, denominator);
        return score;
    }

    @Override
    public double scoreCategory(String category, double categoryScore, double categoryProbability, boolean matched) {
        double score = Math.pow(Math.E, categoryScore + Math.log(categoryProbability));
        LOGGER.info("{} {}·{}={}", category, categoryProbability, categoryScore, score);
        return score;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BayesScorer [laplace=");
        builder.append(laplace);
        builder.append("]");
        return builder.toString();
    }

}
