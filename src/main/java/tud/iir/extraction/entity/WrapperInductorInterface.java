package tud.iir.extraction.entity;

import tud.iir.knowledge.Concept;

/**
 * The WrapperInductorInterface class.
 * 
 * @author David Urbansky
 */
public interface WrapperInductorInterface {
    public void extract(String url, EntityQuery eq, Concept currentConcept);
}
