package ws.palladian.classification.discretization;

import ws.palladian.classification.text.CountingCategoryEntriesBuilder;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Instance;

public class DatasetStatistics {

    private CategoryEntries categoryPriors;

    public DatasetStatistics(Iterable<? extends Instance> instances) {
        CountingCategoryEntriesBuilder categoryPriorsBuilder = new CountingCategoryEntriesBuilder();
        for (Instance instance : instances) {
            categoryPriorsBuilder.add(instance.getCategory(), 1);
        }
        categoryPriors = categoryPriorsBuilder.create();
    }

    public CategoryEntries getCategoryPriors() {
        return categoryPriors;
    }

}
