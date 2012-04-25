package ws.palladian.extraction.keyphrase.temp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

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
    
    private final Bag<UnorderedPair<T>> pairs;
    private final Bag<T> items;

    public CooccurrenceMatrix() {
        pairs = new HashBag<UnorderedPair<T>>();
        items = new HashBag<T>();
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
        return items.size();
    }

    public int getNumUniqueItems() {
        return items.uniqueSet().size();
    }

    public int getNumPairs() {
        return pairs.size();
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

    public T getHighest(T item) {
        T highestItem = null;
        double highestProbability = Integer.MIN_VALUE;
        for (T current : items.uniqueSet()) {
            if (current.equals(item)) {
                continue;
            }
            double probability = getConditionalProbability(current, item);
            if (probability > highestProbability) {
                highestProbability = probability;
                highestItem = current;
            }
        }
        return highestItem;
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
        builder.append(", items=");
        builder.append(items);
        builder.append(", pairs=");
        builder.append(pairs);
        builder.append("]");
        return builder.toString();
    }

}
