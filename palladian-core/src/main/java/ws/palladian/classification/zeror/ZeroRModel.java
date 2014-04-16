package ws.palladian.classification.zeror;

import java.util.Map;
import java.util.Set;

import ws.palladian.core.Model;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CollectionHelper.Order;

/**
 * <p>
 * Model for ZeroR classification. Just keeps class counts from training.
 * </p>
 * 
 * @author pk
 */
public final class ZeroRModel implements Model {

    private static final long serialVersionUID = 1L;

    private final Map<String, Double> categoryProbabilities;

    ZeroRModel(Bag<String> categoryCounts) {
        Map<String, Double> map = CollectionHelper.newHashMap();
        for (String categoryName : categoryCounts.uniqueItems()) {
            map.put(categoryName, (double)categoryCounts.count(categoryName) / categoryCounts.size());
        }
        categoryProbabilities = CollectionHelper.sortByValue(map, Order.DESCENDING);
    }

    @Override
    public Set<String> getCategories() {
        return categoryProbabilities.keySet();
    }

    public Map<String, Double> getCategoryProbabilities() {
        return categoryProbabilities;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ZeroRModel [categoryProbabilities=");
        builder.append(categoryProbabilities);
        builder.append("]");
        return builder.toString();
    }

}
