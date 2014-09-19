package ws.palladian.classification.text;

import static ws.palladian.classification.utils.ClassificationUtils.entropy;
import static ws.palladian.helper.math.MathHelper.log2;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.helper.functional.Filter;

/**
 * Different strategies for pruning a {@link DictionaryModel}.
 * 
 * @see DictionaryBuilder#addPruningStrategy(Filter)
 * @author pk
 */
public final class PruningStrategies {

    /**
     * Prune terms, which occur less than the given count.
     * 
     * @author pk
     */
    public static final class TermCountPruningStrategy implements Filter<CategoryEntries> {

        private final int minCount;

        public TermCountPruningStrategy(int minCount) {
            Validate.isTrue(minCount > 0, "minCount must be greater zero");
            this.minCount = minCount;
        }

        @Override
        public boolean accept(CategoryEntries entries) {
            return entries.getTotalCount() >= minCount;
        }

        @Override
        public String toString() {
            return "TermCountPruningStrategy [minCount=" + minCount + "]";
        }

    }

    /**
     * Prunes such terms, where the most likely probability is below a given threshold.
     * 
     * @author pk
     */
    public static final class MinProbabilityPruningStrategy implements Filter<CategoryEntries> {

        private final double minProbability;

        public MinProbabilityPruningStrategy(double minProbability) {
            Validate.isTrue(minProbability > 0, "minProbability must be greater zero");
            this.minProbability = minProbability;
        }

        @Override
        public boolean accept(CategoryEntries item) {
            return item.getMostLikely().getProbability() >= minProbability;
        }

    }

    /**
     * Prunes {@link CategoryEntries} by Information Gain.
     * 
     * @author pk
     */
    public static final class InformationGainPruningStrategy implements Filter<CategoryEntries> {

        private final double threshold;

        private final double categoryEntropy;

        private final CategoryEntries documentCounts;

        private final int numDocuments;

        public InformationGainPruningStrategy(DictionaryModel model, double threshold) {
            Validate.notNull(model, "model must not be null");
            Validate.isTrue(threshold >= 0, "threshold must be greater/equal zero");
            this.threshold = threshold;
            this.categoryEntropy = entropy(model.getDocumentCounts());
            this.documentCounts = model.getDocumentCounts();
            this.numDocuments = model.getNumDocuments();
        }

        public double getInformationGain(CategoryEntries entries) {
            double ig = categoryEntropy;
            double pTerm = (double)entries.getTotalCount() / numDocuments;
            double pNotTerm = 1 - pTerm;
            CategoryEntries temp = new CountingCategoryEntriesBuilder().add(entries).create();
            for (Category documentCategory : documentCounts) {
                int countTerm = temp.getCount(documentCategory.getName());
                int countNotTerm = documentCategory.getCount() - countTerm;
                double pTermCategory = (double)countTerm / numDocuments;
                double pNotTermCategory = (double)countNotTerm / numDocuments;
                ig += countTerm > 0 ? pTermCategory * log2(pTermCategory / pTerm) : 0;
                ig += countNotTerm > 0 ? pNotTermCategory * log2(pNotTermCategory / pNotTerm) : 0;
            }
            return ig;
        }

        @Override
        public boolean accept(CategoryEntries entries) {
            return getInformationGain(entries) >= threshold;
        }

        @Override
        public String toString() {
            return "InformationGainPruningStrategy [threshold=" + threshold + "]";
        }

    }

    private PruningStrategies() {
        // no instances
    }

}
