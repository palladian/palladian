package ws.palladian.classification.text;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.classification.AbstractCategoryEntries;
import ws.palladian.classification.Category;
import ws.palladian.classification.ImmutableCategory;
import ws.palladian.classification.text.DictionaryModel.TermCategoryEntries;
import ws.palladian.helper.collection.AbstractIterator;

public final class MapTermCategoryEntries extends AbstractCategoryEntries implements TermCategoryEntries {

    private final String term;
    private final int countSum;
    private final Map<String, Integer> categoryCounts;

    /**
     * Create an empty {@link MapTermCategoryEntries}.
     * 
     * @param term The term.
     */
    public MapTermCategoryEntries(String term) {
        this(term, Collections.<String, Integer> emptyMap(), 0);
    }

    public MapTermCategoryEntries(String term, Map<String, Integer> categoryCounts) {
        this(term, categoryCounts, sum(categoryCounts.values()));
    }

    private static final int sum(Collection<Integer> collection) {
        int sum = 0;
        for (Integer count : collection) {
            sum += count;
        }
        return sum;
    }

    public MapTermCategoryEntries(String term, Map<String, Integer> categoryCounts, int countSum) {
        this.term = term;
        this.categoryCounts = categoryCounts;
        this.countSum = countSum;
    }

    @Override
    public Iterator<Category> iterator() {
        final Iterator<Entry<String, Integer>> entryIterator = categoryCounts.entrySet().iterator();
        return new AbstractIterator<Category>() {
            @Override
            protected Category getNext() throws Finished {
                if (entryIterator.hasNext()) {
                    Entry<String, Integer> entry = entryIterator.next();
                    String name = entry.getKey();
                    int count = entry.getValue();
                    double probability = countSum > 0 ? (double)count / countSum : 0;
                    return new ImmutableCategory(name, probability, count);
                }
                throw FINISHED;
            }
        };
    }

    @Override
    public int size() {
        return categoryCounts.size();
    }

    @Override
    public String getTerm() {
        return term;
    }

    @Override
    public int getTotalCount() {
        return countSum;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + categoryCounts.hashCode();
        result = prime * result + term.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MapTermCategoryEntries other = (MapTermCategoryEntries)obj;
        if (countSum != other.countSum) {
            return false;
        }
        if (!term.equals(other.term)) {
            return false;
        }
        return categoryCounts.equals(other.categoryCounts);
    }

    @Override
    public String toString() {
        return term + ":" + categoryCounts;
    }

}
