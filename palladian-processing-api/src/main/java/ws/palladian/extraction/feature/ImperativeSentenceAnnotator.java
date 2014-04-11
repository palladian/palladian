/**
 * Created on: 14.06.2012 20:43:38
 */
package ws.palladian.extraction.feature;

import java.util.Arrays;
import java.util.Iterator;

import ws.palladian.extraction.sentence.AbstractSentenceDetector;
import ws.palladian.extraction.token.AbstractTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.PositionAnnotationFactory;

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
        ListFeature<PositionAnnotation> tokenAnnotations = document.get(ListFeature.class, AbstractTokenizer.PROVIDED_FEATURE);
        ListFeature<PositionAnnotation> sentences = document.get(ListFeature.class, AbstractSentenceDetector.PROVIDED_FEATURE);
        ListFeature<PositionAnnotation> ret = new ListFeature<PositionAnnotation>(PROVIDED_FEATURE);

        Iterator<PositionAnnotation> posTagsIterator = tokenAnnotations.iterator();
        PositionAnnotationFactory annotationFactory = new PositionAnnotationFactory(document);
        for (Annotation sentence : sentences) {
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
                ret.add(annotationFactory.create(sentence.getStartPosition(), sentence.getEndPosition()));
            }
        }
        document.add(ret);
    }
}
