/**
 * Instantiates a new MIOExtraction-process.
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

public class MIOExtractionProcess extends Thread {

	@Override
	public void run() {
		MIOExtractor.getInstance().startExtraction();
	}

    /**
     * Stop extraction.
     * 
     * @return True, if successful.
     */
	public boolean stopExtraction() {
		return MIOExtractor.getInstance().stopExtraction(true);
	}

}
