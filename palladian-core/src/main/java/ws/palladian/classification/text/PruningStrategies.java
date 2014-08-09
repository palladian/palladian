package ws.palladian.classification.text;

import static ws.palladian.classification.utils.ClassificationUtils.entropy;
import static ws.palladian.helper.math.MathHelper.log2;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.DictionaryModel.TermCategoryEntries;
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
    public static final class TermCountPruningStrategy implements Filter<TermCategoryEntries> {

        private final int minCount;

        public TermCountPruningStrategy(int minCount) {
            Validate.isTrue(minCount > 0, "minCount must be greater zero");
            this.minCount = minCount;
        }

        @Override
        public boolean accept(TermCategoryEntries entries) {
            return entries.getTotalCount() >= minCount;
        }

        @Override
        public String toString() {
            return "TermCountPruningStrategy [minCount=" + minCount + "]";
        }

    }

    /**
     * Prunes {@link CategoryEntries} by Information Gain.
     * 
     * @author pk
     */
    public static final class InformationGainPruningStrategy implements Filter<TermCategoryEntries> {

        /** The logger for this class. */
        private static final Logger LOGGER = LoggerFactory.getLogger(InformationGainPruningStrategy.class);

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

        public double getInformationGain(TermCategoryEntries entries) {
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
        public boolean accept(TermCategoryEntries entries) {
            double informationGain = getInformationGain(entries);
            LOGGER.trace("IG({})={}", entries.getTerm(), informationGain);
            return informationGain >= threshold;
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
