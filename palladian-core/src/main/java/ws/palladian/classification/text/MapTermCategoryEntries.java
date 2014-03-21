package ws.palladian.classification.text;

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

    public MapTermCategoryEntries(String term, Map<String, Integer> categoryCounts) {
        this.term = term;
        this.categoryCounts = categoryCounts;
        int sum = 0;
        for (Integer count : categoryCounts.values()) {
            sum += count;
        }
        this.countSum = sum;
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
                    double probability = (double)count / countSum;
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

}
