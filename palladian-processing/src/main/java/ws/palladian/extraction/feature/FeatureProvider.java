/**
 * Created on: 16.06.2012 16:49:52
 */
package ws.palladian.extraction.feature;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.model.features.Feature;
import ws.palladian.model.features.FeatureDescriptor;

/**
 * <p>
 * Describes a {@link PipelineProcessor} that adds a {@link Feature} to processed {@link PipelineDocument}s.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public interface FeatureProvider<F extends Feature<?>> extends PipelineProcessor {
    /**
     * @return The {@link FeatureDescriptor} for the {@link Feature} extracted by this {@link PipelineProcessor}.
     */
    FeatureDescriptor<F> getDescriptor();
}
