package ws.palladian.extraction.sentence;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

import org.apache.commons.lang.Validate;

import ws.palladian.core.ImmutableToken;
import ws.palladian.core.Token;
import ws.palladian.helper.collection.AbstractIterator;
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
public final class OpenNlpSentenceDetector implements SentenceDetector {

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
    public Iterator<Token> iterateTokens(final String text) {
        final opennlp.tools.util.Span[] spans = model.sentPosDetect(text);
        return new AbstractIterator<Token>() {
            int idx = 0;

            @Override
            protected Token getNext() throws Finished {
                if (idx >= spans.length) {
                    throw FINISHED;
                }
                opennlp.tools.util.Span span = spans[idx++];
                String value = text.substring(span.getStart(), span.getEnd());
                return new ImmutableToken(span.getStart(), value);
            }
        };
    }

}
