package tud.iir.extraction.fact;

/**
 * The fact extraction process.
 * 
 * @author David Urbansky
 */
public class FactExtractionProcess extends Thread {

    private boolean benchmark = false;

    public FactExtractionProcess() {
        super();
    }

    public FactExtractionProcess(boolean benchmark) {
        super();
        this.setBenchmark(benchmark);
    }

    @Override
    public void run() {
        // start fact extraction
        // FactExtractor.getInstance().setBenchmark(this.isBenchmark());
        FactExtractor.getInstance().startExtraction();
    }

    public boolean stopExtraction() {
        // stop entity extraction
        return FactExtractor.getInstance().stopExtraction(true);
    }

    public boolean isBenchmark() {
        return benchmark;
    }

    public void setBenchmark(boolean benchmark) {
        this.benchmark = benchmark;
    }
}