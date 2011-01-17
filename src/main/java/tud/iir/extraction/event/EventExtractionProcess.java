package tud.iir.extraction.event;

/**
 * Instantiates a new event extraction process.
 *
 * @author David Urbansky
 */

public class EventExtractionProcess extends Thread {

    @Override
    public void run() {
        EventExtractor.getInstance().startExtraction();
    }

    /**
     * Stop extraction process.
     *
     * @return true, if successful.
     */
    public boolean stopExtraction() {
        return EventExtractor.getInstance().stopExtraction(true);
    }

}
