package ws.palladian.extraction.sentence;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;

import org.apache.commons.lang.Validate;

import ws.palladian.helper.Cache;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * A sentence detector using an implementation from the <a
 * href="http://incubator.apache.org/opennlp/index.html">OpenNLP</a> framework.
 * </p>
 * 
 * @author Martin Wunderwald
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class OpenNlpSentenceDetector extends AbstractSentenceDetector {

    /** The sentence detector object. */
    private final SentenceDetectorME model;

    /**
     * <p>
     * Creates a new completely initialized sentence detector.
     * </p>
     * 
     * @param modelFilePath A path on the local file system to a model file as expected by an <a
     *            href="http://opennlp.sourceforge.net/models-1.5/">OpenNLP</a> sentence detector.
     */
    public OpenNlpSentenceDetector(File modelFile) {
        super();
        Validate.notNull(modelFile, "The model file must not be null.");
        this.model = loadModel(modelFile);
    }

    private final SentenceDetectorME loadModel(File modelFile) {
        SentenceDetectorME sdetector = (SentenceDetectorME)Cache.getInstance().getDataObject(modelFile.getAbsolutePath());
        if (sdetector == null) {
            InputStream modelIn = null;
            try {
                modelIn = new FileInputStream(modelFile);
                sdetector = new SentenceDetectorME(new SentenceModel(modelIn));
                Cache.getInstance().putDataObject(modelFile.getAbsolutePath(), sdetector);
            } catch (IOException e) {
                throw new IllegalStateException("Error initializing OpenNLP Sentence Detector from \""
                        + modelFile.getAbsolutePath() + "\": " + e.getMessage());
            } finally {
                FileHelper.close(modelIn);
            }
        }
        return sdetector;
    }

    @Override
    public OpenNlpSentenceDetector detect(String text) {
        Span[] sentenceBoundaries = model.sentPosDetect(text);
        PositionAnnotation[] sentenceAnnotations = new PositionAnnotation[sentenceBoundaries.length];
        //TextDocument document = new TextDocument(text);
        for (int i = 0; i < sentenceBoundaries.length; i++) {
            int start = sentenceBoundaries[i].getStart();
            int end = sentenceBoundaries[i].getEnd();
            String value = text.substring(start, end);
            sentenceAnnotations[i] = new PositionAnnotation(providedFeature, start, end, i, value);
        }
        setSentences(sentenceAnnotations);
        return this;
    }
}
