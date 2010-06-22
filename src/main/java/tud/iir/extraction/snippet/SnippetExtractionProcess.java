package tud.iir.extraction.snippet;

/**
 * The snippet extraction process.
 * 
 * @author Christopher Friedrich
 */
public class SnippetExtractionProcess extends Thread {

    private boolean benchmark = false;

    public SnippetExtractionProcess() {
        super();
    }

    public SnippetExtractionProcess(boolean benchmark) {
        super();
        this.setBenchmark(benchmark);
    }

    @Override
    public void run() {
        // start snippet extraction
        // SnippetExtractor.getInstance().setBenchmark(this.isBenchmark());
        SnippetExtractor.getInstance().startExtraction();
    }

    public boolean stopExtraction() {
        // stop snippet extraction
        return SnippetExtractor.getInstance().stopExtraction(true);
    }

    public boolean isBenchmark() {
        return benchmark;
    }

    public void setBenchmark(boolean benchmark) {
        this.benchmark = benchmark;
    }
}