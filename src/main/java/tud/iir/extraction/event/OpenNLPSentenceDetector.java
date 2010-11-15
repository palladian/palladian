/**
 *
 */
package tud.iir.extraction.event;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.StopWatch;

/**
 * @author Martin Wunderwald
 */
public class OpenNLPSentenceDetector extends AbstractSentenceDetector {

    /**
     * 
     */
    public OpenNLPSentenceDetector() {
        setName("OpenNLP Sentence Detector");
    }

    /*
     * (non-Javadoc)
     * @see
     * tud.iir.extraction.event.AbstractSentenceDetector#detect(java.lang.String
     * )
     */
    @Override
    public void detect(String text) {

        setSentences(((SentenceDetectorME) getModel()).sentDetect(text));

    }

    /*
     * (non-Javadoc)
     * @see
     * tud.iir.extraction.event.AbstractSentenceDetector#detect(java.lang.String
     * , java.lang.String)
     */
    @Override
    public void detect(String text, String configModelFilePath) {
        loadModel(configModelFilePath);
        detect(text);
    }

    /*
     * (non-Javadoc)
     * @see
     * tud.iir.extraction.event.AbstractSentenceDetector#loadModel(java.lang
     * .String)
     */
    @Override
    public boolean loadModel(String configModelFilePath) {
        try {

            final SentenceDetectorME sdetector = new SentenceDetectorME(new SentenceModel(new FileInputStream(new File(
                    configModelFilePath))));

            setModel(sdetector);

        } catch (final IOException e) {
            LOGGER.error(e);
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.AbstractSentenceDetector#loadModel()
     */
    @Override
    public boolean loadModel() {
        return this.loadModel(MD_SBD_ONLP);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final OpenNLPSentenceDetector onlpstd = new OpenNLPSentenceDetector();
        onlpstd.loadModel();

        onlpstd
                .detect("This are two example Sentences. Oh, we now in the second sentence already.");
        CollectionHelper.print(onlpstd.getSentences());

        stopWatch.stop();
        LOGGER.info("time elapsed: " + stopWatch.getElapsedTimeString());

    }

}
