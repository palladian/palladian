package tud.iir.extraction;

import java.io.Serializable;
import java.util.Comparator;

import tud.iir.knowledge.Concept;

/**
 * Sort concepts by the date they were last searched.
 * 
 * @author David Urbansky
 */
public class ConceptDateComparator implements Comparator<Concept>, Serializable {

    /**
     * <p>
     * 
     * </p>
     */
    private static final long serialVersionUID = 559690360305044017L;

    /**
     * Oldest concept first (null before that as null means "never searched so far").
     * 
     * @param c1 Concept1
     * @param c2 Concept2
     */
    public int compare(Concept c1, Concept c2) {
        if (c1.getLastSearched() == null)
            return -1;
        if (c2.getLastSearched() == null)
            return 1;
        // System.out.println((int)(c1.getLastSearched().getTime() - c2.getLastSearched().getTime()));
        return (int) (c1.getLastSearched().getTime() - c2.getLastSearched().getTime());
    }
}
