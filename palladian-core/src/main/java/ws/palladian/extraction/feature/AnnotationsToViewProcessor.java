/**
 * Created on: 17.04.2012 23:59:46
 */
package ws.palladian.extraction.feature;

import java.util.List;

import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;

/**
 * <p>
 * Takes all {@link Feature}s and transforms them to the output of this processor. So for example if we have the text
 * "The quick brown fox" which has an annotation on "The" and "fox" the processor concatenates "The" and "fox" to
 * "The fox".
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 * 
 */
public final class AnnotationsToViewProcessor extends TextDocumentPipelineProcessor {

    private final String featureIdentifier;

    /**
     * {@see AbstractPipelineProcessor#AbstractPipelineProcessor()}
     */
    public AnnotationsToViewProcessor(String featureIdentifier) {
        this.featureIdentifier = featureIdentifier;
    }

    @Override
    public void processDocument(TextDocument document) {
        FeatureVector vector = document.getFeatureVector();
        List<Feature<?>> features = vector.get(ListFeature.class,featureIdentifier);

        StringBuilder ret = new StringBuilder();
        for (Feature<?> feature : features) {
            ret.append(feature.getValue());
            ret.append(" ");
        }
        ret.replace(ret.length() - 1, ret.length(), "");
        document.setContent(ret.toString());
    }
}
