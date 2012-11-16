/**
 *
 */
package ws.palladian.extraction.sentence;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.feature.StringDocumentPipelineProcessor;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * Abstract base class for all sentence detectors. Subclasses of this class provide an implementation to split texts
 * into sentences.
 * </p>
 * <p>
 * A call to a sentence detector might look like:
 * 
 * <pre>
 * {@code
 * AbstractSentenceDetector sentenceDetector = new ...();
 *    sentenceDetector.detect("This is my sentence. This is another!");
 *    PositionAnnotation[] sentences = sentenceDetector.getSentences();
 *    String firstSentence = sentences[0].getValue();
 * }
 * </pre>
 * 
 * It will return an array containing annotations for the two sentences: "This is my sentence." and "This is another!".
 * Annotations are pointers into a {@link PipelineDocument} created from the input String, marking the start index and
 * end index of the extracted sentence. To access the value just call {@link PositionAnnotation#getValue()}.
 * </p>
 * <p>
 * You can reuse an instance of this class if you want to. Simply call {@link #detect(String)} or
 * {@link #detect(String, String)} on a new {@code String}, consisting out of multiple sentences.
 * </p>
 * 
 * @author Martin Wunderwald
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public abstract class AbstractSentenceDetector extends StringDocumentPipelineProcessor/*  implements
        FeatureProvider<TextAnnotationFeature> */ {

    /**
     * <p>
     * The world wide unique identifier of the {@link Feature}s created by this annotator.
     * </p>
     */
    public static final String PROVIDED_FEATURE = "ws.palladian.features.sentence";
    
    protected final String providedFeature;

//    /**
//     * <p>
//     * The world wide unique feature descriptor of the {@link Feature} created by this annotator.
//     * </p>
//     */
//    public static final FeatureDescriptor<TextAnnotationFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder
//            .build(PROVIDED_FEATURE, TextAnnotationFeature.class);

    /** holds the sentences. **/
    private PositionAnnotation[] sentences;

//    /**
//     * <p>
//     * The {@link FeatureDescriptor} used to identify the provided {@code Feature}.
//     * </p>
//     */
//    private final FeatureDescriptor<TextAnnotationFeature> featureDescriptor;

    /**
     * <p>
     * Creates anew completely initialized sentence detector working on the "originalContent" view if used as
     * {@code PipelineProcessor}.
     * </p>
     */
    public AbstractSentenceDetector() {
        this.providedFeature = PROVIDED_FEATURE;
    }

    public AbstractSentenceDetector(String featureIdentifier) {
        Validate.notNull(featureIdentifier, "featureIdentifier must not be null");
        this.providedFeature = featureIdentifier;
    }

    /**
     * <p>
     * Chunks a text into sentences. Method returns <code>this</code> instance of AbstractSentenceDetector, to allow
     * convenient concatenations of method invocations, like:
     * {@code new OpenNLPSentenceDetector().detect(...).getSentences();}
     * </p>
     * 
     * @param text
     *            The text to detect sentences on.
     * @return {@code this} object for convenient chaining of method calls.
     */
    public abstract AbstractSentenceDetector detect(String text);

    /**
     * <p>
     * Provides the sentences extracted by the previous call to {@link #detect(String)} or
     * {@link #detect(String, String)}.
     * </p>
     * 
     * @return the extracted sentences.
     */
    public final PositionAnnotation[] getSentences() {
        return Arrays.copyOf(sentences, sentences.length);
    }

    /**
     * <p>
     * Resets ond overwrites the last extraction result with the current one in the form of sentece {@code Annotation}s.
     * </p>
     * 
     * @param sentences
     *            Extracted sentence {@code Annotation}
     */
    protected final void setSentences(PositionAnnotation[] sentences) {
        this.sentences = Arrays.copyOf(sentences, sentences.length);
    }

    @Override
    public final void processDocument(PipelineDocument<String> document) {
        Validate.notNull(document, "document must not be null");

        detect(document.getContent());
        PositionAnnotation[] sentences = getSentences();
        List<PositionAnnotation> sentencesList = Arrays.asList(sentences);
        document.getFeatureVector().addAll(sentencesList);
    }

//    @Override
//    public FeatureDescriptor<TextAnnotationFeature> getDescriptor() {
//        return featureDescriptor;
//    }
}
