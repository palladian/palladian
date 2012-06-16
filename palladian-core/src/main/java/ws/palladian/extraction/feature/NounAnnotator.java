/**
 * Created on: 16.06.2012 09:24:59
 */
package ws.palladian.extraction.feature;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;

import scala.actors.threadpool.Arrays;
import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.pos.BasePosTagger;
import ws.palladian.extraction.sentence.LingPipeSentenceDetector;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.NominalFeature;
import ws.palladian.model.features.PositionAnnotation;

/**
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

    private final FeatureDescriptor<AnnotationFeature> featureDescriptor;

    public NounAnnotator(FeatureDescriptor<AnnotationFeature> featureDescriptor) {
        super();

        Validate.notNull(featureDescriptor, "questionNounFeatureDescriptor must not be null");

        this.featureDescriptor = featureDescriptor;
    }

    @Override
    public void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException {
        AnnotationFeature sentencesFeature = document.getFeature(LingPipeSentenceDetector.PROVIDED_FEATURE_DESCRIPTOR);
        AnnotationFeature tokenFeature = document.getFeature(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);

        List<Annotation> ret = new ArrayList<Annotation>();
        @SuppressWarnings("unchecked")
        List<String> nounTagList = Arrays.asList(NOUN_TAGS);
        for (Annotation sentence : sentencesFeature.getValue()) {
            List<Annotation> tokens = tokenFeature.getAnnotations(sentence.getStartPosition(),
                    sentence.getEndPosition());
            for (Annotation token : tokens) {
                NominalFeature posTag = token.getFeature(BasePosTagger.PROVIDED_FEATURE_DESCRIPTOR);
                if (nounTagList.contains(posTag.getValue())) {
                    ret.add(new PositionAnnotation(document, token.getStartPosition(), token.getEndPosition()));
                }
            }
        }

        document.addFeature(new AnnotationFeature(featureDescriptor, ret));
    }

}
