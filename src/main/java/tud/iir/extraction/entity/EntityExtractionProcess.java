package tud.iir.extraction.entity;

/**
 * The EntityExtractionProcess is a thread that runs the entity extraction.
 * 
 * @author David Urbansky
 */
public class EntityExtractionProcess extends Thread {

    public EntityExtractionProcess() {
        super();
    }

    @Override
    public void run() {
        // start entity extraction
        // EntityExtractor.getInstance().setBenchmark(true); // TODO put that somewhere else
        EntityExtractor.getInstance().startExtraction(true, true, true);
    }

    public boolean stopExtraction() {
        // stop entity extraction
        return EntityExtractor.getInstance().stopExtraction(true);
    }
}