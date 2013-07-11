package ws.palladian.preprocessing.nlp;

import java.util.List;

import ws.palladian.extraction.feature.TextDocumentPipelineProcessor;
import ws.palladian.extraction.sentence.AbstractSentenceDetector;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.PositionAnnotationFactory;

/**
 * <p>
 * A {@link PipelineProcessor} detecting 1H5W questions and questions ending on "?" from natural language texts. This is
 * the most simple way to find questions but usually sufficient. The 1H5W words are:
 * <ul>
 * <li>How</li>
 * <li>Who</li>
 * <li>What</li>
 * <li>Where</li>
 * <li>When</li>
 * <li>Why</li>
 * </ul>
 * </p>
 * <p>
 * As input the question annotator requires annotated sentences as produced by any {@link AbstractSentenceDetector}
 * processor. So if you need to detect questions from a text create a new {@link ProcessingPipeline} with an
 * {@code AbstractSentenceDetector} followed by a {@code QuestionAnnotator}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @version 2.0
 * @since 0.1.7
 */
public final class QuestionAnnotator extends TextDocumentPipelineProcessor {
    
    /**
     * The world wide unique identifier of the {@link Feature}s created by this annotator.
     */
    public final static String FEATURE_IDENTIFIER = "ws.palladian.features.question";

    @Override
    public void processDocument(TextDocument document) {
        List<PositionAnnotation> sentences = document.get(ListFeature.class,
                AbstractSentenceDetector.PROVIDED_FEATURE);
        ListFeature<PositionAnnotation> questions = new ListFeature<PositionAnnotation>(FEATURE_IDENTIFIER);
        PositionAnnotationFactory annotationFactory = new PositionAnnotationFactory(document);
        for (PositionAnnotation sentence : sentences) {
            String coveredText = sentence.getValue();
            if (coveredText.endsWith("?") || coveredText.toLowerCase().startsWith("what")
                    || coveredText.toLowerCase().startsWith("who") || coveredText.toLowerCase().startsWith("where")
                    || coveredText.toLowerCase().startsWith("how ") || coveredText.toLowerCase().startsWith("why")) {
                questions.add(annotationFactory.create(sentence.getStartPosition(), sentence.getEndPosition()));
            }
        }
        document.getFeatureVector().addAll(questions);
    }


}
