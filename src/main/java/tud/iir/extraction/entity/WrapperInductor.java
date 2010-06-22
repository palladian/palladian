package tud.iir.extraction.entity;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import tud.iir.knowledge.Entity;

/**
 * The abstract WrapperInductor class.
 * 
 * @author David Urbansky
 */
public abstract class WrapperInductor implements WrapperInductorInterface {

    protected static final Logger logger = Logger.getLogger(WrapperInductor.class);

    protected EntityExtractor ee = null;

    // protected LinkedHashMap<String, Integer> extractions = null;

    public WrapperInductor() {
        // extractions = new LinkedHashMap<String, Integer>();
    }

    public ArrayList<Entity> getExtractions() {
        return ee.getExtractions();
    }
    /*
     * public void setExtractions(LinkedHashMap<String, Integer> extractions) { this.extractions = extractions; } protected void addExtraction(String
     * extraction) { if (extractions.containsKey(extraction)) { int count = extractions.get(extraction); extractions.put(extraction, count + 1); } else {
     * extractions.put(extraction, 1); } }
     */
}