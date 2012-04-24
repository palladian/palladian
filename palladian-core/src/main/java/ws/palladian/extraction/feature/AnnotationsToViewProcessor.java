/**
 * Created on: 17.04.2012 23:59:46
 */
package ws.palladian.extraction.feature;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.extraction.AbstractPipelineProcessor;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.model.features.Feature;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureVector;

/**
 * <p>
 * Takes all {@link Feature}s and transforms them to the output of this processor. So for example if we have the text
 * "The quick brown fox" which has an annotation on "The" and "fox" the processor concatenates "The" and "fox" to
 * "The fox".
 * </p>
 * 
 * @author Klemens
 * @version 1.0
 * @since 0.1.7
 * 
 */
public final class AnnotationsToViewProcessor<F extends Feature<?>> extends AbstractPipelineProcessor {

    private final FeatureDescriptor<F> featureDescriptor;

    /**
     * {@see AbstractPipelineProcessor#AbstractPipelineProcessor()}
     */
    public AnnotationsToViewProcessor(FeatureDescriptor<F> featureDescriptor) {
        super();
        this.featureDescriptor = featureDescriptor;
    }

    /**
     * {@see AbstractPipelineProcessor#AbstractPipelineProcessor()}
     * 
     * @param documentToInputMapping {@see AbstractPipelineProcessor#AbstractPipelineProcessor()}
     */
    public AnnotationsToViewProcessor(Collection<Pair<String, String>> documentToInputMapping,
            FeatureDescriptor<F> featureDescriptor) {
        super(documentToInputMapping);
        this.featureDescriptor = featureDescriptor;
    }

    @Override
    protected void processDocument(PipelineDocument document) {
        FeatureVector vector = document.getFeatureVector();
        List<Feature<F>> features = vector.getAll(featureDescriptor.getType());

        StringBuilder ret = new StringBuilder();
        for (Feature<F> feature : features) {
            ret.append(feature.getValue());
            ret.append(" ");
        }
        document.setModifiedContent(ret.toString());
    }
}
