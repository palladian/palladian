package tud.iir.extraction.qa;

/**
 * The QA extraction process.
 * 
 * @author David Urbansky
 */
public class QAExtractionProcess extends Thread {

    public QAExtractionProcess() {
        super();
    }

    @Override
    public void run() {
        // start qa extraction
        QAExtractor.getInstance().startExtraction();
    }

    public boolean stopExtraction() {
        // stop qa extraction
        return QAExtractor.getInstance().stopExtraction(true);
    }
}