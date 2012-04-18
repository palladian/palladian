/**
 *
 */
package ws.palladian.extraction.sentence;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

import org.apache.log4j.Logger;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.helper.Cache;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.PositionAnnotation;

/**
 * <p>
 * A sentence detector using an implementation from the <a
 * href="http://incubator.apache.org/opennlp/index.html">OpenNLP</a> framework.
 * </p>
 * 
 * @author Martin Wunderwald
 * @author Klemens Muthmann
 */
public final class OpenNlpSentenceDetector extends AbstractSentenceDetector {

    /**
     * <p>
     * Unique identifier to serialize and deserialize objects of this type to and from a file.
     * </p>
     */
    private static final long serialVersionUID = -673731236797308512L;
    /**
     * Logger for this class.
     */
    private static final Logger LOGGER = Logger.getLogger(OpenNlpSentenceDetector.class);

    /**
     * <p>
     * Creates a new completely initialized sentence detector.
     * </p>
     * 
     * @param modelFilePath A path on the local file system to a model file as expected by an <a
     *            href="http://opennlp.sourceforge.net/models-1.5/">OpenNLP</a> sentence detector.
     */
    public OpenNlpSentenceDetector(String modelFilePath) {
        super();

        SentenceModel sentenceModel = null;
        InputStream modelIn = null;
        SentenceDetectorME sdetector = null;

        if (Cache.getInstance().containsDataObject(modelFilePath)) {

            sdetector = (SentenceDetectorME)Cache.getInstance().getDataObject(modelFilePath);

        } else {

            final StopWatch stopWatch = new StopWatch();

            try {

                modelIn = new FileInputStream(modelFilePath);

                sentenceModel = new SentenceModel(modelIn);

                sdetector = new SentenceDetectorME(sentenceModel);
                Cache.getInstance().putDataObject(modelFilePath, sdetector);
                LOGGER.info("Reading OpenNLP Sentence Detector from file " + modelFilePath + " in "
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
    public OpenNlpSentenceDetector detect(String text) {
        Span[] sentenceBoundaries = ((SentenceDetectorME)getModel()).sentPosDetect(text);
        Annotation[] sentenceAnnotations = new Annotation[sentenceBoundaries.length];
        PipelineDocument document = new PipelineDocument(text);
        for (int i = 0; i < sentenceBoundaries.length; i++) {
            int start = sentenceBoundaries[i].getStart();
            int end = sentenceBoundaries[i].getEnd();
            sentenceAnnotations[i] = new PositionAnnotation(document, start, end);
        }
        setSentences(sentenceAnnotations);
        return this;
    }
}
