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
        QAExtractor.getInstance().startExtraction();
    }

    public boolean stopExtraction() {
        return QAExtractor.getInstance().stopExtraction(true);
    }
}