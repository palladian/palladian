package ws.palladian.extraction.sentence;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;

import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;

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
        Validate.notNull(modelFile, "The model file must not be null.");
        this.model = loadModel(modelFile);
    }

    private static final SentenceDetectorME loadModel(File modelFile) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(modelFile);
            return new SentenceDetectorME(new SentenceModel(inputStream));
        } catch (IOException e) {
            throw new IllegalStateException("Error initializing OpenNLP Sentence Detector from \""
                    + modelFile.getAbsolutePath() + "\": " + e.getMessage());
        } finally {
            FileHelper.close(inputStream);
        }
    }

    @Override
    public List<Annotation> getAnnotations(String text) {
        List<Annotation> sentences = CollectionHelper.newArrayList();
        Span[] sentenceBoundaries = model.sentPosDetect(text);
        for (int i = 0; i < sentenceBoundaries.length; i++) {
            int start = sentenceBoundaries[i].getStart();
            int end = sentenceBoundaries[i].getEnd();
            String value = text.substring(start, end);
            sentences.add(new ImmutableAnnotation(start, value, StringUtils.EMPTY));
        }
        return sentences;
    }
}
