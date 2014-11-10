package ws.palladian.classification.discretization;

import java.util.Set;

import ws.palladian.classification.text.CountingCategoryEntriesBuilder;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Instance;
import ws.palladian.helper.collection.CollectionHelper;

public class DatasetStatistics {

    private CategoryEntries categoryPriors;
    
    private Set<String> featureNames;

    public DatasetStatistics(Iterable<? extends Instance> instances) {
        CountingCategoryEntriesBuilder categoryPriorsBuilder = new CountingCategoryEntriesBuilder();
        this.featureNames = CollectionHelper.newHashSet();
        for (Instance instance : instances) {
            categoryPriorsBuilder.add(instance.getCategory(), 1);
            featureNames.addAll(instance.getVector().keys());
        }
        categoryPriors = categoryPriorsBuilder.create();
    }

    public CategoryEntries getCategoryPriors() {
        return categoryPriors;
    }
    
    public Set<String> getFeatureNames() {
        return featureNames;
    }

}
