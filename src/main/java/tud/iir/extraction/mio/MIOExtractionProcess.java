package tud.iir.extraction.mio;

public class MIOExtractionProcess extends Thread {

    /** The benchmark. */
    private boolean benchmark = false;

 
//    /**
//     * Instantiates a new MIOExtraction-process.
//     * 
//     */
//    public MIOExtractionProcess() {
//        super();
//        //this.setBenchmark(benchmark);
//    }

    @Override
    public void run() {
        // start MIOExtraction
        MIOExtractor.getInstance().startExtraction();
    }

    public boolean stopExtraction() {
        // stop MIOExtraction
        return MIOExtractor.getInstance().stopExtraction(true);
    }

    public boolean isBenchmark() {
        return benchmark;
    }

    public void setBenchmark(final boolean benchmark) {
        this.benchmark = benchmark;
    }

}
