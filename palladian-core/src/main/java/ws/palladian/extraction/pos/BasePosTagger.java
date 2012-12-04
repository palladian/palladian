package ws.palladian.extraction.pos;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.extraction.TagAnnotation;
import ws.palladian.extraction.TagAnnotations;
import ws.palladian.extraction.feature.TextDocumentPipelineProcessor;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * This is the abstract base class for all Part of Speech taggers offered by Palladian. It implements two interfaces:
 * 
 * <ol>
 * <li>{@link PosTagger}, which is the "traditional" API used in Palladian. It allows POS tagging for text supplied as
 * String. In this case, the text is tokenized using a default {@link BaseTokenizer} implementation specific for the
 * respective POS tagger. Subclasses may override {@link #getTokenizer()} if they require a specific tokenizer.</li>
 * 
 * <li>{@link PipelineProcessor}, which works based on token annotations provided by an {@link AnnotationFeature}. This
 * means, that the input document must be tokenized in advance, using one of the available {@link BaseTokenizer}
 * implementations. In this mode, the POS tags are appended to the token's {@link FeatureVector}s and can be retrieved
 * later using the {@link #PROVIDED_FEATURE_DESCRIPTOR}.</li>
 * </ol>
 * </p>
 * 
 * @author Martin Wunderwald
 * @author David Urbansky
 * @author Philipp Katz
 */
public abstract class BasePosTagger extends TextDocumentPipelineProcessor implements PosTagger {

    /**
     * <p>
     * The identifier of the feature provided by this {@link PipelineProcessor}.
     * </p>
     */
    public static final String PROVIDED_FEATURE = "ws.palladian.features.pos";

//    /**
//     * <p>
//     * The descriptor of the feature provided by this {@link PipelineProcessor}.
//     * </p>
//     */
//    public static final FeatureDescriptor<NominalFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder.build(
//            PROVIDED_FEATURE, NominalFeature.class);

    /**
     * <p>
     * The default {@link BaseTokenizer} used if not overridden.
     * </p>
     */
    private static final BaseTokenizer DEFAULT_TOKENIZER = new RegExTokenizer();

    // ////////////////////////////////////////////
    // PosTagger API
    // ////////////////////////////////////////////

    @Override
    public TagAnnotations tag(String text) {
        TextDocument document = new TextDocument(text);
        try {
            BaseTokenizer tokenizer = getTokenizer();
            tokenizer.processDocument(document);
            this.processDocument(document);
        } catch (DocumentUnprocessableException e) {
            throw new IllegalArgumentException(e);
        }
        List<PositionAnnotation> annotationFeatureList = document.getFeatureVector().getAll(PositionAnnotation.class,
                BaseTokenizer.PROVIDED_FEATURE);
        TagAnnotations ret = new TagAnnotations();
        int offset = 0;
        for (PositionAnnotation annotation : annotationFeatureList) {
            NominalFeature tagFeature = annotation.getFeatureVector().getFeature(NominalFeature.class, PROVIDED_FEATURE);
            String tag = tagFeature.getValue();
            TagAnnotation tagAnnotation = new TagAnnotation(offset++, tag, annotation.getValue());
            ret.add(tagAnnotation);
        }
        return ret;
    }

    /**
     * <p>
     * Return the {@link BaseTokenizer} which this {@link PosTagger} uses when tagging String using {@link #tag(String)}
     * . Per default, a {@link RegExTokenizer} is returned, subclasses may override this method, if a specific
     * {@link BaseTokenizer} is required.
     * </p>
     * 
     * @return The {@link BaseTokenizer} to use.
     */
    protected BaseTokenizer getTokenizer() {
        return DEFAULT_TOKENIZER;
    }

    // ////////////////////////////////////////////
    // PipelineProcessor API
    // ////////////////////////////////////////////

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        FeatureVector featureVector = document.getFeatureVector();
        List<PositionAnnotation> annotationFeature = featureVector.getAll(PositionAnnotation.class, BaseTokenizer.PROVIDED_FEATURE);
//        if (annotationFeature == null) {
//            throw new DocumentUnprocessableException(
//                    "Document content is not tokenized. Please use a tokenizer before using a POS tagger.");
//        }
        tag(annotationFeature);
    }

    // ////////////////////////////////////////////
    // internal/subclass methods
    // ////////////////////////////////////////////

    /**
     * <p>
     * Subclasses implement this method to perform the POS tagging. The POS tags can be assigned to each annotation
     * using the provided convenience method {@link #assignTag(PositionAnnotation, String)}.
     * </p>
     * 
     * @param annotations
     *            The list of annotations to process, this is the tokenized
     *            text.
     */
    protected abstract void tag(List<PositionAnnotation> annotations);

    /**
     * <p>
     * Helper method to convert a {@link List} of {@link PositionAnnotation}s to a {@link List} with their String values.
     * </p>
     * 
     * @param annotations
     * @return
     */
    protected static List<String> getTokenList(List<PositionAnnotation> annotations) {
        List<String> tokenList = new ArrayList<String>(annotations.size());
        for (PositionAnnotation annotation : annotations) {
            tokenList.add(annotation.getValue());
        }
        return tokenList;
    }

    protected static String normalizeTag(String tag) {
        // return tag.replaceAll("(-|\\+).*", "");
        return tag.replaceAll("-.*", "");
    }

    /**
     * <p>
     * Helper method to assign a POS tag to a {@link PositionAnnotation}.
     * </p>
     * 
     * @param annotation
     * @param tag
     */
    protected static void assignTag(PositionAnnotation annotation, String tag) {
        annotation.getFeatureVector().add(new NominalFeature(PROVIDED_FEATURE, tag.toUpperCase()));
    }

}
