/**
 * Created on: 14.06.2012 20:43:38
 */
package ws.palladian.extraction.feature;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import ws.palladian.extraction.sentence.AbstractSentenceDetector;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * Marks imperative sentences inside the processed text.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class ImperativeSentenceAnnotator extends TextDocumentPipelineProcessor {

    private static final String[] IMPERATIVE_TAGS = new String[] {"VB", "DO"};

    public static final String PROVIDED_FEATURE= "ws.palladian.imperative";

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        List<PositionAnnotation> tokenAnnotations = document.getFeatureVector().getAll(PositionAnnotation.class, BaseTokenizer.PROVIDED_FEATURE);
        List<PositionAnnotation> sentences = document.getFeatureVector().getAll(PositionAnnotation.class, AbstractSentenceDetector.PROVIDED_FEATURE);
        List<PositionAnnotation> ret = new LinkedList<PositionAnnotation>();

        Iterator<PositionAnnotation> posTagsIterator = tokenAnnotations.iterator();
        for (PositionAnnotation sentence : sentences) {
            String firstTagInSentence = null;
            while (posTagsIterator.hasNext()) {
                PositionAnnotation currentPosTag = posTagsIterator.next();
                if (currentPosTag.getStartPosition() >= sentence.getStartPosition()) {
                    firstTagInSentence = currentPosTag.getValue();
                    break;
                }
            }

            if (firstTagInSentence == null) {
                break;
            }
            if (Arrays.asList(IMPERATIVE_TAGS).contains(firstTagInSentence)) {
                ret.add(new PositionAnnotation(PROVIDED_FEATURE, sentence.getStartPosition(), sentence.getEndPosition(),
                        0, sentence.getValue()));
            }
        }
        document.getFeatureVector().addAll(ret);
    }
}
