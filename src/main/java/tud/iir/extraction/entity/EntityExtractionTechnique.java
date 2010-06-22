package tud.iir.extraction.entity;

import java.util.HashSet;

import tud.iir.knowledge.Concept;

/**
 * The abstract class for each entity extraction technique.
 * 
 * @author David Urbansky
 */
abstract class EntityExtractionTechnique {
    protected HashSet<String> urlProcessed = new HashSet<String>();

    public int extractionTechnique = -1;

    public abstract Integer[] getPatterns();

    public abstract EntityQuery getEntityQuery(Concept concept, int entityQueryType);

    public abstract void extract(String url, EntityQuery eq, Concept concept);

    public int getExtractionTechnique() {
        return extractionTechnique;
    }

    public void setExtractionTechnique(int extractionTechnique) {
        this.extractionTechnique = extractionTechnique;
    }
}