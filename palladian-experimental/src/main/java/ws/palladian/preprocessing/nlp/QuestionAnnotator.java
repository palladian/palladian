package ws.palladian.preprocessing.nlp;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.extraction.feature.StringDocumentPipelineProcessor;
import ws.palladian.extraction.sentence.AbstractSentenceDetector;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.TextAnnotationFeature;

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
public final class QuestionAnnotator extends StringDocumentPipelineProcessor {

    /**
     * Unique identifier to serialize and deserialize objects of this type to and from a file.
     */
    private static final long serialVersionUID = -2998306515098026978L;
    /**
     * The world wide unique identifier of the {@link Feature}s created by this annotator.
     */
    public final static String FEATURE_IDENTIFIER = "ws.palladian.features.question";

    @Override
    public void processDocument(PipelineDocument<String> document) {
        TextAnnotationFeature sentences = document.getFeatureVector().get(
                AbstractSentenceDetector.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation<String>> questions = new ArrayList<Annotation<String>>();
        for (Annotation<String> sentence : sentences.getValue()) {
            String coveredText = sentence.getValue();
            if (coveredText.endsWith("?") || coveredText.toLowerCase().startsWith("what")
                    || coveredText.toLowerCase().startsWith("who") || coveredText.toLowerCase().startsWith("where")
                    || coveredText.toLowerCase().startsWith("how ") || coveredText.toLowerCase().startsWith("why")) {

                questions.add(createQuestion(sentence));
            }
        }
        Feature<List<Annotation<String>>> questionsFeature = new Feature<List<Annotation<String>>>(FEATURE_IDENTIFIER,
                questions);
        document.getFeatureVector().add(questionsFeature);
    }

    /**
     * <p>
     * Creates a new question {@code Annotation} based on an existing sentence {@code Annotation}.
     * </p>
     * 
     * @param sentence The sentence {@code Annotation} representing the new question.
     * @return A new annotation of the question type spanning the same area as the provided sentence.
     */
    private Annotation<String> createQuestion(Annotation<String> sentence) {
        return new PositionAnnotation(sentence.getDocument(), sentence.getStartPosition(), sentence.getEndPosition(),
                -1, sentence.getValue());
    }

}
