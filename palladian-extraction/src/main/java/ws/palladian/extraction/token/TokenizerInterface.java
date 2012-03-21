package ws.palladian.extraction.token;

import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.feature.AnnotationFeature;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;

public interface TokenizerInterface extends PipelineProcessor {

    /**
     * <p>
     * The identifier of the feature provided by this {@link PipelineProcessor}.
     * </p>
     */
    static final String PROVIDED_FEATURE = "ws.palladian.features.tokens";

    /**
     * <p>
     * The descriptor of the feature provided by this {@link PipelineProcessor}.
     * </p>
     */
    static final FeatureDescriptor<AnnotationFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder.build(
            PROVIDED_FEATURE, AnnotationFeature.class);

}
