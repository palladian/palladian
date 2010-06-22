package tud.iir.extraction.entity;

import java.io.Serializable;
import java.util.Comparator;

import tud.iir.knowledge.Entity;

/**
 * Sort entities by trust.
 * 
 * @author David Urbansky
 */
public class EntityTrustComparator implements Comparator<Entity>, Serializable {

    /**
     * <p>
     * 
     * </p>
     */
    private static final long serialVersionUID = 538330317493559905L;

    /**
     * Highest trust first.
     * 
     * @param e1 Entity1
     * @param e2 Entity2
     */
    public int compare(Entity e1, Entity e2) {
        return (1000 * e2.getExtractionCount() - 1000 * e1.getExtractionCount());
    }
}