package ws.palladian.preprocessing.nlp;

import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.feature.TextDocumentPipelineProcessor;
import ws.palladian.extraction.sentence.AbstractSentenceDetector;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureProvider;
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
 * <p>
 * Questions are extracted as a {@link TextAnnotationFeature}. You may retrieve your extracted questions as a
 * {@code List} of {@link PositionAnnotation}s using the {@link Feature#getValue()} method.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @version 2.0
 * @since 0.1.7
 */
public final class QuestionAnnotator extends TextDocumentPipelineProcessor implements FeatureProvider {

    /**
     * The world wide unique identifier of the {@link Feature}s created by this annotator.
     */
    @Deprecated
    public final static String FEATURE_IDENTIFIER = "ws.palladian.features.question";

    /**
     * <p>
     * The name used to identify the provided {@code Feature}.
     * </p>
     */
    private final String featureName;

    /**
     * <p>
     * Creates a new {@code PalladianQuestionAnnotator} annotating questions in a document and saving those annotations
     * under the {@code Feature} described by {@code FeatureDescriptor}.
     * </p>
     * 
     * @param featureName The name used to identify the provided {@code Feature}.
     */
    public QuestionAnnotator(final String featureName) {
        Validate.notNull(featureName, "featureDescriptor must not be null");

        this.featureName = featureName;
    }

    /**
     * <p>
     * The no argument constructor using a default {@code FeatureDescriptor} for the annotated questions.
     * </p>
     * 
     */
    @Deprecated
    public QuestionAnnotator() {
        this.featureName = FEATURE_IDENTIFIER;
    }

    @Override
    public void processDocument(TextDocument document) {
        List<PositionAnnotation> sentences = document.get(ListFeature.class, AbstractSentenceDetector.PROVIDED_FEATURE);
        ListFeature<PositionAnnotation> questions = new ListFeature<PositionAnnotation>(getCreatedFeatureName());
        PositionAnnotationFactory annotationFactory = new PositionAnnotationFactory(document);
        for (PositionAnnotation sentence : sentences) {
            String coveredText = sentence.getValue();
            if (coveredText.endsWith("?") || coveredText.toLowerCase().startsWith("what")
                    || coveredText.toLowerCase().startsWith("who") || coveredText.toLowerCase().startsWith("where")
                    || coveredText.toLowerCase().startsWith("how ") || coveredText.toLowerCase().startsWith("why")) {
                questions.add(annotationFactory.create(sentence.getStartPosition(), sentence.getEndPosition()));
            }
        }
        document.add(questions);
    }

    @Override
    public String getCreatedFeatureName() {
        return featureName;
    }
}
