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

    /* (non-Javadoc)
 * @see java.lang.Thread#run()
 */
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

    /**
     * Checks if is benchmark.
     *
     * @return true, if is benchmark
     */
    public boolean isBenchmark() {
        return benchmark;
    }

    /**
     * Sets the benchmark.
     *
     * @param benchmark the new benchmark
     */
    public void setBenchmark(final boolean benchmark) {
        this.benchmark = benchmark;
    }

}
