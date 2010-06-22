package tud.iir.extraction.entity;

import java.io.Serializable;
import java.util.HashSet;

import tud.iir.extraction.ExtractionType;

/**
 * Save the status of the last entity extraction process in order to be able to continue later on. Save the technique used, the current concept and the stack of
 * URLs that need to be processed.
 * 
 * @author David Urbansky
 */
final class EntityExtractionStatus implements Serializable {

    private static final long serialVersionUID = 7512162142957348590L;

    // if true, the status has been loaded and thus should be used for the
    // extraction process, false otherwise
    private boolean loaded = false;

    // if true, the loaded values have been used in the extraction process that
    // loaded the status, that is, the status can be updated and overwritten
    // again
    private boolean initialized = false;

    // the extraction technique to continue with
    private int extractionType = -1;

    // some techniques work with different patterns, therefore we need to
    // remember which pattern we used before serializing the state
    private int patternNumber = 0;

    // the current concepts for the current urlStack
    private String currentConcept = null;

    // the current stack of URLs
    private HashSet<String> urlStack = new HashSet<String>();

    public EntityExtractionStatus() {
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public int getExtractionType() {
        return extractionType;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public void setExtractionType(int extractionType) {
        this.extractionType = extractionType;
    }

    public void nextExtractionType() {
        switch (getExtractionType()) {
            case ExtractionType.ENTITY_PHRASE:
                setExtractionType(ExtractionType.ENTITY_FOCUSED_CRAWL);
                break;
            case ExtractionType.ENTITY_FOCUSED_CRAWL:
                setExtractionType(ExtractionType.ENTITY_SEED);
                break;
            case ExtractionType.ENTITY_SEED:
                setExtractionType(ExtractionType.ENTITY_PHRASE);
                break;
        }
    }

    public String getCurrentConcept() {
        return currentConcept;
    }

    public void setCurrentConcept(String currentConcept) {
        this.currentConcept = currentConcept;
    }

    public HashSet<String> getUrlStack() {
        return urlStack;
    }

    public void setUrlStack(HashSet<String> urlStack) {
        this.urlStack = urlStack;
    }

    public void setPatternNumber(int patternNumber) {
        this.patternNumber = patternNumber;
    }

    public int getPatternNumber() {
        return patternNumber;
    }

    @Override
    public String toString() {
        return "current concept: " + getCurrentConcept() + ", et: " + getExtractionType() + ", pattern: " + getPatternNumber() + ", URL stack size: "
                + getUrlStack().size() + ", loaded: " + isLoaded() + ", initialized: " + isInitialized();
    }
}