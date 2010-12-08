/**
 *
 */
package tud.iir.extraction.event;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.InvalidFormatException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import tud.iir.helper.CollectionHelper;
import tud.iir.helper.DataHolder;
import tud.iir.helper.StopWatch;

/**
 * @author Martin Wunderwald
 */
public class OpenNLPSentenceDetector extends AbstractSentenceDetector {

    /**
     * Logger for this class.
     */
    protected static final Logger LOGGER = Logger
            .getLogger(OpenNLPSentenceDetector.class);

    /** model for opennlp sentence detection */
    private final String MODEL;

    /**
     * constructor for this class.
     */
    public OpenNLPSentenceDetector() {
        setName("OpenNLP Sentence Detector");

        PropertiesConfiguration config = null;

        try {
            config = new PropertiesConfiguration("config/models.conf");
        } catch (final ConfigurationException e) {
            LOGGER.error("could not get modepath from config/models.conf, "
                    + e.getMessage());
        }

        if (config != null) {
            MODEL = config.getString("models.opennlp.en.sentdetect");
        } else {
            MODEL = "";
        }
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

        SentenceModel sentenceModel = null;
        InputStream modelIn = null;
        SentenceDetectorME sdetector = null;

        if (DataHolder.getInstance().containsDataObject(configModelFilePath)) {

            sdetector = (SentenceDetectorME) DataHolder.getInstance()
                    .getDataObject(configModelFilePath);

        } else {

            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            try {

                modelIn = new FileInputStream(configModelFilePath);

                sentenceModel = new SentenceModel(modelIn);

                sdetector = new SentenceDetectorME(sentenceModel);
                DataHolder.getInstance().putDataObject(configModelFilePath,
                        sdetector);
                LOGGER.info("Reading " + this.getName() + " from file "
                        + configModelFilePath + " in "
                        + stopWatch.getElapsedTimeString());

            } catch (final InvalidFormatException e) {
                LOGGER.error(e);
            } catch (final FileNotFoundException e) {
                LOGGER.error(e);
            } catch (final IOException e) {
                LOGGER.error(e);
            } finally {
                if (modelIn != null) {
                    try {
                        modelIn.close();
                    } catch (final IOException e) {
                    }
                }
            }
        }

        setModel(sdetector);

        return false;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.AbstractSentenceDetector#loadModel()
     */
    @Override
    public boolean loadModel() {
        return this.loadModel(MODEL);
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
