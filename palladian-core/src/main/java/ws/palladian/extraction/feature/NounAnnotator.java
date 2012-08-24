/**
 * Created on: 16.06.2012 09:24:59
 */
package ws.palladian.extraction.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.Validate;

import ws.palladian.extraction.pos.BasePosTagger;
import ws.palladian.extraction.sentence.AbstractSentenceDetector;
import ws.palladian.extraction.sentence.LingPipeSentenceDetector;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.TextAnnotationFeature;

/**
 * <p>
 * Annotates all nouns in a text. The text must have been processed by an {@link AbstractSentenceDetector} and a
 * {@link BaseTokenizer}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class NounAnnotator extends StringDocumentPipelineProcessor {

    /**
     * 
     */
    private static final long serialVersionUID = -9032354669004873512L;

    private final static String[] NOUN_TAGS = new String[] {"NN", "NN$", "NNS", "NNS$", "NP", "NP$", "NPS", "NPS$"};

    private final FeatureDescriptor<TextAnnotationFeature> featureDescriptor;

    public NounAnnotator(FeatureDescriptor<TextAnnotationFeature> featureDescriptor) {
        super();

        Validate.notNull(featureDescriptor, "questionNounFeatureDescriptor must not be null");

        this.featureDescriptor = featureDescriptor;
    }

    @Override
    public void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException {
        TextAnnotationFeature sentencesFeature = document.getFeature(LingPipeSentenceDetector.PROVIDED_FEATURE_DESCRIPTOR);
        Validate.notNull(
                sentencesFeature,
                "Nount annotator can only work if the text was processed by an AbstractSentenceDetector. Please add one to your pipeline.");
        TextAnnotationFeature tokenFeature = document.getFeature(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        Validate.notNull(tokenFeature,
                "Nount annotator can only work if the text was processed by a BaseTokenizer. Please add one to your pipeline.");

        List<Annotation<String>> ret = new ArrayList<Annotation<String>>();
        List<String> nounTagList = Arrays.asList(NOUN_TAGS);
        for (Annotation<String> sentence : sentencesFeature.getValue()) {
            List<Annotation<String>> tokens = tokenFeature.getAnnotations(sentence.getStartPosition(),
                    sentence.getEndPosition());
            for (Annotation<String> token : tokens) {
            	NominalFeature posTag = token.getFeature(BasePosTagger.PROVIDED_FEATURE_DESCRIPTOR);
                if (nounTagList.contains(posTag.getValue())) {
                    ret.add(new PositionAnnotation(document, token.getStartPosition(), token.getEndPosition()));
                }
            }
        }

        document.addFeature(new TextAnnotationFeature(featureDescriptor, ret));
    }

}
