package ws.palladian.processing.features;

import java.util.List;

import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;

/**
 * <p>
 * An {@link TextAnnotationFeature} is a feature consisting of a List of {@link Annotation}s. Annotations are pointers
 * to parts of text in a {@link PipelineDocument} which can be characterized by a {@link FeatureVector}.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class TextAnnotationFeature extends AnnotationFeature<String> {

    /**
     * <p>
     * Create a new {@link TextAnnotationFeature}.
     * </p>
     * 
     * @param descriptor The {@link FeatureDescriptor} with a world wide unique identifier for all similar
     *            {@code AnnotationFeature}s. This is usually used to identify the {@link PipelineProcessor} that
     *            extracted this {@code TextAnnotationFeature}.
     * @param annotations The initial {@code List} of {@code Annotation}s of this feature.
     */
    public TextAnnotationFeature(FeatureDescriptor<TextAnnotationFeature> descriptor,
            List<Annotation<String>> annotations) {
        super(descriptor, annotations);
    }

    /**
     * <p>
     * Create a new {@link TextAnnotationFeature}.
     * </p>
     * 
     * @param descriptor The {@link FeatureDescriptor} with a world wide unique identifier for all similar
     *            {@code TextAnnotationFeature}s. This is usually used to identify the {@link PipelineProcessor} that
     *            extracted this {@code TextAnnotationFeature}.
     */
    public TextAnnotationFeature(FeatureDescriptor<TextAnnotationFeature> descriptor) {
        super(descriptor);
    }

    /**
     * <p>
     * Create a new {@link TextAnnotationFeature}.
     * </p>
     * 
     * @param name A world wide unique identifier for all similar {@code TextAnnotationFeature}s. This is usually used
     *            to identify the {@link PipelineProcessor} that extracted this {@code TextAnnotationFeature}.
     * @param annotations The initial {@code List} of {@code Annotation}s of this feature.
     */
    public TextAnnotationFeature(String name, List<Annotation<String>> annotations) {
        super(name, annotations);
    }

    /**
     * <p>
     * Create a new {@link TextAnnotationFeature}.
     * </p>
     * 
     * @param name A world wide unique identifier for all similar {@code TextAnnotationFeature}s. This is usually used
     *            to identify the {@link PipelineProcessor} that extracted this {@code TextAnnotationFeature}.
     */
    public TextAnnotationFeature(String name) {
        super(name);
    }

}
