package ws.palladian.classification.text;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper.Order;

public enum TermSelector {
    /** Get the terms from the beginning of the text. */
    FIRST {
        @Override
        Set<Entry<String, Integer>> getTerms(Iterator<String> termIterator, int limit) {
            Bag<String> termCounts = new Bag<>();
            while (termIterator.hasNext() && termCounts.uniqueItems().size() < limit) {
                termCounts.add(termIterator.next());
            }
            return termCounts.unique();
        }
    },

    /** Get the most frequent terms. */
    FREQUENCY {
        @Override
        Set<Entry<String, Integer>> getTerms(Iterator<String> termIterator, int limit) {
            Bag<String> termCounts = new Bag<>();
            while (termIterator.hasNext()) {
                termCounts.add(termIterator.next());
            }
            return termCounts.createSorted(Order.DESCENDING).unique().stream().limit(limit).collect(Collectors.toSet());
        }
    };

    abstract Set<Entry<String, Integer>> getTerms(Iterator<String> termIterator, int limit);
}
