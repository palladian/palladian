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
import opennlp.tools.util.Span;

import org.apache.log4j.Logger;

import ws.palladian.helper.Cache;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.featureextraction.Token;

/**
 * @author Martin Wunderwald
 * @author Klemens Muthmann
 */
public class OpenNLPSentenceDetector extends AbstractSentenceDetector {

    /**
     * 
     */
    private static final long serialVersionUID = -673731236797308512L;
    /**
     * Logger for this class.
     */
    private static final Logger LOGGER = Logger.getLogger(OpenNLPSentenceDetector.class);

    /**
     * constructor for this class.
     */
    public OpenNLPSentenceDetector(String modelFilePath) {
        super();
        setName("OpenNLP Sentence Detector");

        SentenceModel sentenceModel = null;
        InputStream modelIn = null;
        SentenceDetectorME sdetector = null;

        if (Cache.getInstance().containsDataObject(modelFilePath)) {

            sdetector = (SentenceDetectorME) Cache.getInstance().getDataObject(modelFilePath);

        } else {

            final StopWatch stopWatch = new StopWatch();

            try {

                modelIn = new FileInputStream(modelFilePath);

                sentenceModel = new SentenceModel(modelIn);

                sdetector = new SentenceDetectorME(sentenceModel);
                Cache.getInstance().putDataObject(modelFilePath, sdetector);
                LOGGER.info("Reading " + getName() + " from file " + modelFilePath + " in "
                        + stopWatch.getElapsedTimeString());

            } catch (final InvalidFormatException e) {
                LOGGER.error(e);
            } catch (final FileNotFoundException e) {
                LOGGER.error(e);
            } catch (final IOException e) {
                LOGGER.error(e);
            } finally {
                FileHelper.close(modelIn);
            }
        }

        setModel(sdetector);
    }

    @Override
    public OpenNLPSentenceDetector detect(String text) {
        Span[] sentenceBoundaries = ((SentenceDetectorME) getModel()).sentPosDetect(text);
        Token[] sentenceAnnotations = new Token[sentenceBoundaries.length];
        PipelineDocument document = new PipelineDocument(text);
        for (int i = 0; i < sentenceBoundaries.length; i++) {
            int start = sentenceBoundaries[i].getStart();
            int end = sentenceBoundaries[i].getEnd();
            sentenceAnnotations[i] = new Token(document, start, end);
        }
        setSentences(sentenceAnnotations);
        return this;
    }
}
