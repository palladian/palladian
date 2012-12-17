/**
 * Created on: 16.06.2012 09:24:59
 */
package ws.palladian.extraction.feature;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.Validate;

import ws.palladian.extraction.pos.BasePosTagger;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.FeatureProvider;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.PositionAnnotationFactory;

/**
 * <p>
 * Annotates all nouns in a text. The text must have been processed by a {@link BaseTokenizer} and a
 * {@link BasePosTagger}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class NounAnnotator extends TextDocumentPipelineProcessor implements FeatureProvider {

    private final static String[] NOUN_TAGS = new String[] {"NN", "NN$", "NNS", "NNS$", "NP", "NP$", "NPS", "NPS$"};

    private final String featureIdentifier;

    /**
     * <p>
     * Create a new NounAnnotator with the specified identifier used for all created noun annotations.
     * </p>
     * 
     * @param featureIdentifier The identifier of the noun annotations, not <code>null</code> or empty.
     */
    public NounAnnotator(String featureIdentifier) {
        Validate.notEmpty(featureIdentifier, "featureIdentifier must not be empty");
        this.featureIdentifier = featureIdentifier;
    }

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        List<PositionAnnotation> ret = CollectionHelper.newArrayList();
        List<String> nounTagList = Arrays.asList(NOUN_TAGS);
        PositionAnnotationFactory annotationFactory = new PositionAnnotationFactory(featureIdentifier, document);
        for (PositionAnnotation token : document.getFeatureVector().getAll(PositionAnnotation.class,
                BaseTokenizer.PROVIDED_FEATURE)) {
            NominalFeature posTag = token.getFeatureVector().getFeature(NominalFeature.class,
                    BasePosTagger.PROVIDED_FEATURE);
            if (posTag == null) {
                throw new DocumentUnprocessableException(
                        "At least one token has not PoS tag. The noun annotator requires the pipeline to call a PoSTagger in advance.");
            } else if (nounTagList.contains(posTag.getValue())) {
                ret.add(annotationFactory.create(token.getStartPosition(), token.getEndPosition()));
            }
        }
        document.getFeatureVector().addAll(ret);
    }

    @Override
    public String getCreatedFeatureName() {
        return featureIdentifier;
    }

}
