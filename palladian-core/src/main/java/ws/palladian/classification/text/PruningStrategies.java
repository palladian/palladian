package ws.palladian.classification.text;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.DictionaryBuilder.PruningStrategy;
import ws.palladian.classification.text.DictionaryModel.TermCategoryEntries;
import ws.palladian.core.Category;

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
    public static final class TermFrequencyPruningStrategy implements PruningStrategy {

        private final int minCount;

        public TermFrequencyPruningStrategy(int minCount) {
            Validate.isTrue(minCount > 0, "minCount must be greater zero");
            this.minCount = minCount;
        }

        @Override
        public boolean remove(TermCategoryEntries entries) {
            return entries.getTotalCount() < minCount;
        }

    }

    /**
     * Prune terms, where entropy for the category probabilities is above a given threshold. This means effectively,
     * that terms where category probabilities are distributed equally can be pruned.
     * 
     * @author pk
     */
    public static final class EntropyPruningStrategy implements PruningStrategy {

        private final double maxEntropy;

        public EntropyPruningStrategy(int numCategories, double maxEntropyRatio) {
            Validate.isTrue(numCategories > 1, "numCategories must be greater one");
            Validate.isTrue(0 < maxEntropyRatio && maxEntropyRatio <= 1, "maxEntropyRatio must be in range ]0,1]");
            this.maxEntropy = maxEntropyRatio * Math.log(numCategories);
        }

        public EntropyPruningStrategy(DictionaryModel model, double maxEntropyRatio) {
            this(model.getNumCategories(), maxEntropyRatio);
        }

        @Override
        public boolean remove(TermCategoryEntries entries) {
            return entropy(entries) >= maxEntropy * 0.9999999; // rounding issue
        }

        private static double entropy(TermCategoryEntries entries) {
            double entropy = 0;
            for (Category category : entries) {
                entropy += category.getProbability() * Math.log(category.getProbability());
            }
            return -entropy;
        }

    }

    private PruningStrategies() {
        // no instances
    }

}
