/**
 * Created on: 16.06.2012 16:49:52
 */
package ws.palladian.processing.features;

import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;

/**
 * <p>
 * Describes a {@link PipelineProcessor} that adds a {@link Feature} to processed {@link PipelineDocument}s.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public interface FeatureProvider extends PipelineProcessor {
    /**
     * @return The {@link FeatureDescriptor} for the {@link Feature} extracted by this {@link PipelineProcessor}.
     */
    String getDescriptor();
}
