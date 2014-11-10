package ws.palladian.retrieval.cooccurrence;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * This class holds the co-occurrence statistics for two terms. *
 * </p>
 * 
 * @author David Urbansky
 */
public class CoOccurrenceStatistics {

    /** The first term. */
    private final String term1;

    /** The second term. */
    private final String term2;
    
    /**
     * The number of co-occurrences for each searcher used. The key is the name of the searcher.
     */
    private final Bag<String> coOccurrences;
    
    /** The actual sources of the co-occurrence. */
    private Map<String, Collection<String>> coOccurrenceSources;

    public CoOccurrenceStatistics(String term1, String term2) {
        this.term1 = term1;
        this.term2 = term2;
        coOccurrences = Bag.create();
        coOccurrenceSources = CollectionHelper.newHashMap();
    }

    public Map<String, Collection<String>> getCoOccurrenceSources() {
        return coOccurrenceSources;
    }

    public String getTerm1() {
        return term1;
    }

    public String getTerm2() {
        return term2;
    }

    public Bag<String> getCoOccurrences() {
        return coOccurrences;
    }

    public void addCoOccurrence(String searcherName, String source) {
        coOccurrences.add(searcherName);

        Collection<String> collection = coOccurrenceSources.get(searcherName);
        if (collection == null) {
            collection = new HashSet<String>();
            coOccurrenceSources.put(searcherName, collection);
        }

        collection.add(source);
    }

    public int getTotalCoOccurrenceCount() {
        return coOccurrences.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Co-Occurrence between \"" + getTerm1() + "\" and \"" + getTerm2() + "\"")
                .append(" (total: " + getTotalCoOccurrenceCount() + ")").append("\n");
        for (Entry<String, Collection<String>> element : coOccurrenceSources.entrySet()) {
            sb.append("\t").append(element.getKey()).append("\n");
            for (String source : element.getValue()) {
                sb.append("\t\t").append(source).append("\n");
            }
        }

        return sb.toString();
    }

}
