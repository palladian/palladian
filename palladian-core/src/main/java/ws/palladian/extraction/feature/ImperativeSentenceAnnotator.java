/**
 * Created on: 14.06.2012 20:43:38
 */
package ws.palladian.extraction.feature;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.extraction.sentence.AbstractSentenceDetector;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.FeatureDescriptorBuilder;
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
public final class ImperativeSentenceAnnotator extends StringDocumentPipelineProcessor {

    private static final String[] IMPERATIVE_TAGS = new String[] {"VB", "DO"};
    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(ImperativeSentenceAnnotator.class);

    public static final FeatureDescriptor<AnnotationFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder
            .build("ws.palladian.imperative", AnnotationFeature.class);

    @Override
    public void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException {
        AnnotationFeature feature = document.getFeatureVector().get(
                AbstractSentenceDetector.PROVIDED_FEATURE_DESCRIPTOR);
        AnnotationFeature tokenAnnotations = document.getFeatureVector().get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> sentences = feature.getValue();
        List<Annotation> posTags = tokenAnnotations.getValue();
        List<Annotation> ret = new LinkedList<Annotation>();

        Iterator<Annotation> posTagsIterator = posTags.iterator();
        for (Annotation sentence : sentences) {
            String firstTagInSentence = null;
            while (posTagsIterator.hasNext()) {
                Annotation currentPosTag = posTagsIterator.next();
                if (currentPosTag.getStartPosition() >= sentence.getStartPosition()) {
                    firstTagInSentence = currentPosTag.getValue();
                    break;
                }
            }

            if (firstTagInSentence == null) {
                break;
            }
            if (Arrays.asList(IMPERATIVE_TAGS).contains(firstTagInSentence)) {
                ret.add(new PositionAnnotation(document, sentence.getStartPosition(), sentence.getEndPosition(),
                        sentence.getValue()));
            }
        }
        document.addFeature(new AnnotationFeature(PROVIDED_FEATURE_DESCRIPTOR, ret));
    }
}
