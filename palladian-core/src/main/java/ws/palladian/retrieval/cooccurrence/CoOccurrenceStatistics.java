package ws.palladian.retrieval.cooccurrence;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.helper.collection.CountMap;

/**
 * <p>
 * This class holds the co-occurrence statistics for two terms. *
 * </p>
 * 
 * @author David Urbansky
 */
public class CoOccurrenceStatistics {

    /** The first term. */
    private String term1;

    /** The second term. */
    private String term2;
    
    /**
     * The number of co-occurrences for each searcher used. The key is the name of the searcher.
     */
    private CountMap<String> coOccurrences;
    
    /** The actual sources of the co-occurrence. */
    private Map<String, Collection<String>> coOccurrenceSources;

    public CoOccurrenceStatistics(String term1, String term2) {
        this.term1 = term1;
        this.term2 = term2;
        coOccurrences = CountMap.create();

        coOccurrenceSources = new HashMap<String, Collection<String>>();
    }

    public Map<String, Collection<String>> getCoOccurrenceSources() {
        return coOccurrenceSources;
    }

    public void setCoOccurrenceSources(Map<String, Collection<String>> coOccurrenceSources) {
        this.coOccurrenceSources = coOccurrenceSources;
    }

    public String getTerm1() {
        return term1;
    }

    public void setTerm1(String term1) {
        this.term1 = term1;
    }

    public String getTerm2() {
        return term2;
    }

    public void setTerm2(String term2) {
        this.term2 = term2;
    }

    public CountMap<String> getCoOccurrences() {
        return coOccurrences;
    }

    public void setCoOccurrences(CountMap<String> coOccurrences) {
        this.coOccurrences = coOccurrences;
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
        
        return coOccurrences.totalSize();

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
