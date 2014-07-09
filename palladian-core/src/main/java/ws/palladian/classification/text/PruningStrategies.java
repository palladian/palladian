package ws.palladian.classification.text;

import static ws.palladian.classification.utils.ClassificationUtils.entropy;
import static ws.palladian.helper.math.MathHelper.log2;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.DictionaryBuilder.PruningStrategy;
import ws.palladian.classification.text.DictionaryModel.TermCategoryEntries;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;

/**
 * Different strategies for pruning a {@link DictionaryModel}.
 * 
 * @see DictionaryModel#prune(PruningStrategy)
 * @author pk
 */
public final class PruningStrategies {

    /**
     * Prune terms, which occur less than the given count.
     * 
     * @author pk
     */
    public static final class TermCountPruningStrategy implements PruningStrategy {

        private final int minCount;

        public TermCountPruningStrategy(int minCount) {
            Validate.isTrue(minCount > 0, "minCount must be greater zero");
            this.minCount = minCount;
        }

        @Override
        public boolean remove(TermCategoryEntries entries) {
            return entries.getTotalCount() < minCount;
        }

    }

//    /**
//     * Prune terms, where entropy for the category probabilities is above a given threshold. This means effectively,
//     * that terms where category probabilities are distributed equally can be pruned.
//     * 
//     * @author pk
//     */
//    public static final class EntropyPruningStrategy implements PruningStrategy {
//
//        private final double maxEntropy;
//
//        public EntropyPruningStrategy(int numCategories, double maxEntropyRatio) {
//            Validate.isTrue(numCategories > 1, "numCategories must be greater one");
//            Validate.isTrue(0 < maxEntropyRatio && maxEntropyRatio <= 1, "maxEntropyRatio must be in range ]0,1]");
//            this.maxEntropy = maxEntropyRatio * Math.log(numCategories);
//        }
//
//        public EntropyPruningStrategy(DictionaryModel model, double maxEntropyRatio) {
//            this(model.getNumCategories(), maxEntropyRatio);
//        }
//
//        @Override
//        public boolean remove(TermCategoryEntries entries) {
//            return entropy(entries) >= maxEntropy * 0.9999999; // rounding issue
//        }
//
//        private static double entropy(TermCategoryEntries entries) {
//            double entropy = 0;
//            for (Category category : entries) {
//                entropy += category.getProbability() * Math.log(category.getProbability());
//            }
//            return -entropy;
//        }
//
//    }
    
    /**
     * Prunes {@link CategoryEntries} by Information Gain.
     * 
     * @author pk
     */
    public static final class InformationGainPruningStrategy implements PruningStrategy {

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
        public boolean remove(TermCategoryEntries entries) {
            double informationGain = getInformationGain(entries);
            LOGGER.trace("IG({})={}", entries.getTerm(), informationGain);
            return informationGain < threshold;
        }

    }


    private PruningStrategies() {
        // no instances
    }

}
