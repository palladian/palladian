package ws.palladian.preprocessing.nlp.pos;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NominalFeature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;
import ws.palladian.preprocessing.featureextraction.Annotation;
import ws.palladian.preprocessing.featureextraction.AnnotationFeature;
import ws.palladian.preprocessing.nlp.TagAnnotation;
import ws.palladian.preprocessing.nlp.TagAnnotations;
import ws.palladian.preprocessing.nlp.tokenization.RegExTokenizer;
import ws.palladian.preprocessing.nlp.tokenization.Tokenizer;

/**
 * <p>
 * This is the abstract base class for all Part of Speech taggers offered by Palladian. It implements two interfaces:
 * <ol>
 * <li>PosTagger, which is the "traditional" API used in Palladian. It allows POS tagging for text supplied as String.
 * In this case, the text is tokenized using a default {@link Tokenizer} implementation specific for the respective POS
 * tagger.
 * </ol>
 * </p>
 * 
 * @author Martin Wunderwald
 * @author David Urbansky
 * @author Philipp Katz
 */
public abstract class BasePosTagger implements PosTagger, PipelineProcessor {

    private static final long serialVersionUID = 1L;

    /**
     * <p>
     * The identifier of the feature provided by this {@link PipelineProcessor}.
     * </p>
     */
    public static final String PROVIDED_FEATURE = "ws.palladian.features.pos";

    /**
     * <p>
     * The descriptor of the feature provided by this {@link PipelineProcessor}.
     * </p>
     */
    public static final FeatureDescriptor<NominalFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder.build(
            PROVIDED_FEATURE, NominalFeature.class);

    private final Tokenizer tokenizer;

    public BasePosTagger() {
        this.tokenizer = new RegExTokenizer();
        // this.tokenizer = new LingPipeTokenizer();
    }

    @Override
    public TagAnnotations tag(String text) {
        PipelineDocument document = new PipelineDocument(text);
        tokenizer.process(document);
        process(document);
        AnnotationFeature annotationFeature = document.getFeatureVector().get(Tokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        TagAnnotations ret = new TagAnnotations();
        int offset = 0;
        for (Annotation annotation : annotationFeature.getValue()) {
            String tag = annotation.getFeatureVector().get(PROVIDED_FEATURE_DESCRIPTOR).getValue();
            TagAnnotation tagAnnotation = new TagAnnotation(offset++, tag, annotation.getValue());
            ret.add(tagAnnotation);
        }
        return ret;
    }

    public abstract void tag(List<Annotation> annotations);

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(Tokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new IllegalStateException(
                    "Document content is not tokenized. Please use a tokenizer before using a POS tagger.");
        }
        tag(annotationFeature.getValue());
    }

    protected static List<String> getTokenList(List<Annotation> annotations) {
        List<String> tokenList = new ArrayList<String>(annotations.size());
        for (Annotation annotation : annotations) {
            tokenList.add(annotation.getValue());
        }
        return tokenList;
    }

    protected static String normalizeTag(String tag) {
        // return tag.replaceAll("(-|\\+).*", "");
        return tag.replaceAll("-.*", "");
    }

    /**
     * @param annotation
     * @param tag
     */
    protected static void assignTag(Annotation annotation, String tag) {
        annotation.getFeatureVector().add(new NominalFeature(PROVIDED_FEATURE_DESCRIPTOR, tag.toUpperCase()));
    }

}
