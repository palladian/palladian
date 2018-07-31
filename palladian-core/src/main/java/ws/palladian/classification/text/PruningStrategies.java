package ws.palladian.classification.text;

import static ws.palladian.helper.math.MathHelper.log2;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import java.util.function.Predicate;

/**
 * Different strategies for pruning a {@link DictionaryModel}.
 * 
 * @see DictionaryBuilder#addPruningStrategy(Predicate)
 * @author Philipp Katz
 */
public final class PruningStrategies {
    
    // XXX make static methods
    
    public static Predicate<CategoryEntries> none() {
        return new TermCountPruningStrategy(0);
    }
    
    public static Predicate<CategoryEntries> termCount(int minCount) {
        return new TermCountPruningStrategy(minCount);
    }

    /**
     * Prune terms, which occur less than the given count.
     * 
     * @author Philipp Katz
     * @deprecated Use {@link PruningStrategies#termCount(int)} instead.
     */
    @Deprecated
    public static final class TermCountPruningStrategy implements Predicate<CategoryEntries> {

        private final int minCount;

        public TermCountPruningStrategy(int minCount) {
            Validate.isTrue(minCount > 0, "minCount must be greater zero");
            this.minCount = minCount;
        }

        @Override
        public boolean test(CategoryEntries entries) {
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
     * @author Philipp Katz
     */
    public static final class MinProbabilityPruningStrategy implements Predicate<CategoryEntries> {

        private final double minProbability;

        public MinProbabilityPruningStrategy(double minProbability) {
            Validate.isTrue(minProbability > 0, "minProbability must be greater zero");
            this.minProbability = minProbability;
        }

        @Override
        public boolean test(CategoryEntries item) {
            return item.getMostLikely().getProbability() >= minProbability;
        }

    }

    /**
     * Prunes {@link CategoryEntries} by Information Gain.
     * 
     * @author Philipp Katz
     */
    public static final class InformationGainPruningStrategy implements Predicate<CategoryEntries> {

        private final double threshold;

        private final double categoryEntropy;

        private final CategoryEntries documentCounts;

        private final int numDocuments;

        public InformationGainPruningStrategy(DictionaryModel model, double threshold) {
            Validate.notNull(model, "model must not be null");
            Validate.isTrue(threshold >= 0, "threshold must be greater/equal zero");
            this.threshold = threshold;
            this.categoryEntropy = model.getDocumentCounts().getEntropy();
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
        public boolean test(CategoryEntries entries) {
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
