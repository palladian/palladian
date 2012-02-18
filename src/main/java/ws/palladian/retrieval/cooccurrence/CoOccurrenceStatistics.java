package ws.palladian.retrieval.cooccurrence;

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
    private CountMap coOccurrences;
    
    public CoOccurrenceStatistics(String term1, String term2) {
        this.term1 = term1;
        this.term2 = term2;
        coOccurrences = new CountMap();
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

    public CountMap getCoOccurrences() {
        return coOccurrences;
    }

    public void setCoOccurrences(CountMap coOccurrences) {
        this.coOccurrences = coOccurrences;
    }

    public void addCoOccurrence(String searcherName) {
        coOccurrences.increment(searcherName);
    }

    @Override
    public String toString() {
        return "CoOccurrenceStatistics [term1=" + term1 + ", term2=" + term2 + ", coOccurrences=" + coOccurrences + "]";
    }

}
