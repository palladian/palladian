/**
 * Instantiates a new MIOExtraction-process.
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

public class MIOExtractionProcess extends Thread {

	@Override
	public void run() {
		// start MIOExtraction
		MIOExtractor.getInstance().startExtraction();
	}

	/**
	 * Stop extraction.
	 * 
	 * @return true, if successful
	 */
	public boolean stopExtraction() {
		// stop MIOExtraction
		return MIOExtractor.getInstance().stopExtraction(true);
	}

}
