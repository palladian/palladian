package tud.iir.extraction.entity;

import java.io.Serializable;
import java.util.Comparator;

import tud.iir.knowledge.Entity;

/**
 * Sort entities by the date they were last searched.
 * 
 * @author David Urbansky
 */
public class EntityDateComparator implements Comparator<Entity>, Serializable {

    /**
     * <p>
     * 
     * </p>
     */
    private static final long serialVersionUID = 8424522489594400812L;

    /**
     * Sort that the oldest entity appears first.
     * 
     * @param e1 Entity1
     * @param e2 Entity2
     */
    public int compare(Entity e1, Entity e2) {
        if (e1.getLastSearched() == null || e2.getLastSearched() == null)
            return 1;
        return (int) (e1.getLastSearched().getTime() - e2.getLastSearched().getTime());
    }
}