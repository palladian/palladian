/**
 * Created on: 16.06.2012 09:24:59
 */
package ws.palladian.extraction.feature;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.Validate;

import ws.palladian.extraction.pos.BasePosTagger;
import ws.palladian.extraction.sentence.AbstractSentenceDetector;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.PositionAnnotation;

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

    private final static String[] NOUN_TAGS = new String[] {"NN", "NN$", "NNS", "NNS$", "NP", "NP$", "NPS", "NPS$"};

    private final String featureIdentifier;

    public NounAnnotator(String featureIdentifier) {
        Validate.notNull(featureIdentifier, "featureIdentifier must not be null");
        this.featureIdentifier = featureIdentifier;
    }

    @Override
    public void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException {
        List<PositionAnnotation> ret = CollectionHelper.newArrayList();
        List<String> nounTagList = Arrays.asList(NOUN_TAGS);
        int index = 0;
        for (PositionAnnotation token : document.getFeatureVector().getAll(PositionAnnotation.class, BaseTokenizer.PROVIDED_FEATURE)) {
            NominalFeature posTag = token.getFeatureVector().getFeature(NominalFeature.class, BasePosTagger.PROVIDED_FEATURE);
            if (posTag == null) {
                throw new DocumentUnprocessableException(
                        "At least one token has not PoS tag. The noun annotator requires the pipeline to call a PoSTagger in advance.");
            } else if (nounTagList.contains(posTag.getValue())) {
                ret.add(new PositionAnnotation(featureIdentifier, token.getStartPosition(), token.getEndPosition(), index++, token.getValue()));
            }
        }
        document.getFeatureVector().addAll(ret);
    }

}
