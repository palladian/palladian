package ws.palladian.extraction.keyphrase.temp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.collection.UnorderedPair;

/**
 * <p>
 * A co-occurrence matrix which allows calculating conditional probabilities.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <T> The type of objects this {@link CooccurrenceMatrix} keeps.
 */
public final class CooccurrenceMatrix<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final CountMap<UnorderedPair<T>> pairs;
    private final CountMap<T> items;

    public CooccurrenceMatrix() {
        pairs = CountMap.create();
        items = CountMap.create();
    }

    public void add(T itemA, T itemB) {
        pairs.add(UnorderedPair.of(itemA, itemB));
        items.add(itemA);
        items.add(itemB);
    }

    public void addAll(Collection<T> itemCollection) {
        List<T> itemList = new ArrayList<T>(itemCollection);
        for (int i = 0; i < itemList.size(); i++) {
            for (int j = i + 1; j < itemList.size(); j++) {
                pairs.add(UnorderedPair.of(itemList.get(i), itemList.get(j)));
            }
        }
        items.addAll(itemCollection);
    }

    public void addAll(T... itemArray) {
        addAll(Arrays.asList(itemArray));
    }

    public int getCount(T item) {
        return items.getCount(item);
    }

    public int getCount(T itemA, T itemB) {
        return pairs.getCount(UnorderedPair.of(itemA, itemB));
    }

    public int getNumItems() {
        return items.totalSize();
    }

    public int getNumUniqueItems() {
        return items.uniqueSize();
    }

    public int getNumPairs() {
        return pairs.totalSize();
    }

    public double getProbability(T item) {
        return (double)getCount(item) / getNumItems();
    }

    public double getJointProbability(T itemA, T itemB) {
        return (double)getCount(itemA, itemB) / getNumPairs();
    }

    /**
     * <p>
     * Get the conditional probability <code>P(itemA|itemB)</code>.
     * </p>
     * 
     * @param itemA
     * @param itemB
     * @return
     */
    public double getConditionalProbability(T itemA, T itemB) {
        int pairCount = getCount(itemA, itemB);
        int itemCount = getCount(itemB);
        if (itemCount == 0) {
            return 0; // is this assumption okay?
        }
        return (double)pairCount / itemCount;

    }

    /**
     * <p>
     * Get the conditional probability <code>P(itemA|itemB)</code> with additional La Place (add one) smoothing.
     * </p>
     * 
     * @param itemA
     * @param itemB
     * @return
     */
    public double getConditionalProbabilityLaplace(T itemA, T itemB) {
        int pairCount = getCount(itemA, itemB) + 1;
        int givenCount = getCount(itemB) + getNumUniqueItems();
        return (double)pairCount / givenCount;
    }

    /**
     * <p>
     * Get the highest co-occuring item for the supplied item.
     * </p>
     * 
     * @param item The item for which to retrieve the co-occurring item.
     * @return The item with the highest conditional probability for co-occurring with the supplied item, or
     *         <code>null</code> if no such item exists.
     */
    public Pair<T, Double> getHighest(T item) {
        Pair<T, Double> result = null;
        List<Pair<T, Double>> items = getHighest(item, 1);
        if (items.size() > 0) {
            result = items.get(0);
        }
        return result;
    }

    /**
     * <p>
     * Get the number of highest co-occurring items for the supplied item.
     * </p>
     * 
     * @param item The item for which to retrieve co-occurring items.
     * @param num The number of co-occurring items to retrieve.
     * @return A {@link List} with {@link Pair}s containing of item and conditional probability for co-occurring with
     *         the supplied item, or an empty list if no such items exist.
     */
    public List<Pair<T, Double>> getHighest(T item, int num) {
        List<Pair<T, Double>> result = CollectionHelper.newArrayList();
        CountMap<T> temp = CountMap.create(items);
        for (int i = 0; i < num; i++) {
            T highestItem = null;
            double highestProbability = Integer.MIN_VALUE;
            for (T current : temp.uniqueItems()) {
                if (current.equals(item)) {
                    continue;
                }
                double probability = getConditionalProbability(current, item);
                if (probability > highestProbability) {
                    highestProbability = probability;
                    highestItem = current;
                }
            }
            if (highestItem == null || highestProbability == 0.) {
                break;
            }
            temp.remove(highestItem);
            result.add(ImmutablePair.of(highestItem, highestProbability));
        }
        return result;
    }

    /**
     * <p>
     * Resets the content of this {@link CooccurrenceMatrix}.
     * </p>
     */
    public void reset() {
        pairs.clear();
        items.clear();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CooccurrenceMatrix [numItems=");
        builder.append(getNumItems());
        builder.append(", numUniqueItems=");
        builder.append(getNumUniqueItems());
        builder.append(", numPairs=");
        builder.append(getNumPairs());
        // builder.append(", items=");
        // builder.append(items);
        // builder.append(", pairs=");
        // builder.append(pairs);
        builder.append("]");
        return builder.toString();
    }

}
