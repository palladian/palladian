package ws.palladian.classification.text;

import ws.palladian.classification.text.PalladianTextClassifier.DefaultScorer;
import ws.palladian.classification.text.PalladianTextClassifier.Scorer;

public final class ExperimentalScorers {

    private ExperimentalScorers() {
        // no instances
    }

    /**
     * <p>
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

    /**
     * <p>
     * Scorer for the text classifier which weights the squared term-category probabilities additionally with the TF-IDF
     * score determined from the model.
     * 
     * @author pk
     * @deprecated Best results were achieved using the {@link BayesScorer}.
     */
    @Deprecated
    public static final class FrequencyScorer extends PalladianTextClassifier.DefaultScorer {
        @Override
        public double score(String term, String category, int termCategoryCount, int dictCount, int docCount,
                int categorySum, int numUniqTerms, int numDocs, int numTerms) {
            if (dictCount == 0) { // prevent zero division
                return 0;
            }
            double termCategoryProbability = (double)termCategoryCount / dictCount;
            double squaredTermCategoryProb = termCategoryProbability * termCategoryProbability;
            double idf = Math.log((numDocs + 1) / (dictCount + 1));
            double weight = Math.log(docCount + 1) * idf;
            return weight * squaredTermCategoryProb;
        }
    }

    /**
     * <p>
     * Scorer for the text classifier which weights the squared term-category probabilities additionally with the number
     * of documents in the dictionary, which contain the term. This way, we weight terms with more observations in the
     * training data higher than those with only few observations.
     * 
     * @author pk
     * @deprecated Best results were achieved using the {@link BayesScorer}.
     */
    @Deprecated
    public static final class LogCountScorer extends PalladianTextClassifier.DefaultScorer {
        @Override
        public double score(String term, String category, int termCategoryCount, int dictCount, int docCount,
                int categorySum, int numUniqTerms, int numDocs, int numTerms) {
            if (dictCount == 0) { // prevent zero division
                return 0;
            }
            double termCategoryProbability = (double)termCategoryCount / dictCount;
            double squaredTermCategoryProb = termCategoryProbability * termCategoryProbability;
            // return squaredTermCategoryProb * Math.log(1 + dictCount);
            return squaredTermCategoryProb * (1 + Math.log(dictCount));
        }
    }

    /**
     * <p>
     * Scorer using the <a href="http://en.wikipedia.org/wiki/Kullback–Leibler_divergence">Kullback-Leibler
     * divergence</a>. Basically, it measures the similarity between the probability distributions of the document to
     * score and each category in the model based on the term frequencies (including some backoff).
     * 
     * @author pk
     */
    public static final class KLScorer implements Scorer {

        /** The singleton instance of {@link KLScorer}. */
        public static final KLScorer INSTANCE = new KLScorer();

        private KLScorer() {
            // singleton
        }

        @Override
        public double score(String term, String category, int termCategoryCount, int dictCount, int docCount,
                int categorySum, int numUniqTerms, int numDocs, int numTerms) {
            // backoff, similar to a Naive Bayes; pretend, we have seen each term once more than actually
            double p = (double)(docCount + 1) / numUniqTerms;
            double q = (double)(termCategoryCount + 1) / (categorySum + numUniqTerms);
            return p * Math.log(p / q);
        }

        @Override
        public double scoreCategory(String category, double summedTermScore, double categoryProbability, boolean matched) {
            return -summedTermScore;
        }

        @Override
        public boolean scoreNonMatches() {
            return true;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }

    }

}
