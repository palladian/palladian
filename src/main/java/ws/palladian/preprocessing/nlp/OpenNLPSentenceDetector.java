/**
 *
 */
package ws.palladian.preprocessing.nlp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.InvalidFormatException;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.DataHolder;
import ws.palladian.helper.StopWatch;

/**
 * @author Martin Wunderwald
 */
public class OpenNLPSentenceDetector extends AbstractSentenceDetector {

    /**
     * Logger for this class.
     */
    protected static final Logger LOGGER = Logger.getLogger(OpenNLPSentenceDetector.class);

    /** model path for opennlp sentence detection. */
    private final transient String MODEL;

    /**
     * constructor for this class.
     */
    public OpenNLPSentenceDetector() {
        super();
        setName("OpenNLP Sentence Detector");

        PropertiesConfiguration config = null;

        config = ConfigHolder.getInstance().getConfig();

        if (config == null) {
            MODEL = "";
        } else {
            MODEL = config.getString("models.root") + config.getString("models.opennlp.en.sentdetect");
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * tud.iir.extraction.event.AbstractSentenceDetector#detect(java.lang.String
     * )
     */
    @Override
    public OpenNLPSentenceDetector detect(String text) {
        setSentences(((SentenceDetectorME) getModel()).sentDetect(text));
        return this;
    }

    /*
     * (non-Javadoc)
     * @see
     * tud.iir.extraction.event.AbstractSentenceDetector#detect(java.lang.String
     * , java.lang.String)
     */
    @Override
    public OpenNLPSentenceDetector detect(String text, String modelFilePath) {
        loadModel(modelFilePath);
        return detect(text);
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.event.AbstractSentenceDetector#loadModel()
     */
    @Override
    public OpenNLPSentenceDetector loadDefaultModel() {
        return loadModel(MODEL);
    }

    /*
     * (non-Javadoc)
     * @see
     * tud.iir.extraction.event.AbstractSentenceDetector#loadModel(java.lang
     * .String)
     */
    @Override
    public OpenNLPSentenceDetector loadModel(String modelFilePath) {

        SentenceModel sentenceModel = null;
        InputStream modelIn = null;
        SentenceDetectorME sdetector = null;

        if (DataHolder.getInstance().containsDataObject(modelFilePath)) {

            sdetector = (SentenceDetectorME) DataHolder.getInstance().getDataObject(modelFilePath);

        } else {

            final StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            try {

                modelIn = new FileInputStream(modelFilePath);

                sentenceModel = new SentenceModel(modelIn);

                sdetector = new SentenceDetectorME(sentenceModel);
                DataHolder.getInstance().putDataObject(modelFilePath, sdetector);
                LOGGER.info("Reading " + getName() + " from file " + modelFilePath + " in "
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
                        LOGGER.error(e);
                    }
                }
            }
        }

        setModel(sdetector);

        return this;
    }

}
