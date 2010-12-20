package tud.iir.extraction.snippet;

/**
 * The snippet extraction process.
 * 
 * @author David Urbansky
 */
public class SnippetExtractionProcess extends Thread {

    public SnippetExtractionProcess() {
        super();
    }

    @Override
    public void run() {
        SnippetExtractor.getInstance().startExtraction();
    }

    public boolean stopExtraction() {
        return SnippetExtractor.getInstance().stopExtraction(true);
    }

}