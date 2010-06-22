package tud.iir.extraction.fact;

import java.io.Serializable;
import java.util.Comparator;

import tud.iir.knowledge.FactValue;

/**
 * Sort facts by their trust.
 * 
 * @author David
 * 
 */
public class FactValueComparator implements Comparator<FactValue>, Serializable {

    /**
     * <p>
     * 
     * </p>
     */
    private static final long serialVersionUID = -2936921538610063960L;

    /**
     * Highest trust first.
     * 
     * @param fv1 FactValue1
     * @param fv2 FactValue2
     * @return 0 or 1 depending on the trust.
     */
    public int compare(FactValue fv1, FactValue fv2) {
        return (int) (1000 * fv2.getCorroboration() - 1000 * fv1.getCorroboration());
    }
}