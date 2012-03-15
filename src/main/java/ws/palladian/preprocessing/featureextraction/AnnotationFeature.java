package ws.palladian.preprocessing.featureextraction;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.model.features.Feature;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

/**
 * <p>
 * An {@link AnnotationFeature} is a feature consisting of a List of {@link Annotation}s. Annotations are pointers to
 * parts of text in a {@link PipelineDocument} which can be characterized by a {@link FeatureVector}.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class AnnotationFeature extends Feature<List<Annotation>> {

    /**
     * <p>
     * Create a new {@link AnnotationFeature}.
     * </p>
     * 
     * @param name A world wide unique identifier for all similar {@code AnnotationFeature}s. This is usually used to
     *            identify the {@link PipelineProcessor} that extracted this {@code AnnotationFeature}.
     */
    public AnnotationFeature(String name) {
        super(name, new ArrayList<Annotation>());
    }

    /**
     * <p>
     * Create a new {@link AnnotationFeature}.
     * </p>
     * 
     * @param descriptor The {@link FeatureDescriptor} with a world wide unique identifier for all similar
     *            {@code AnnotationFeature}s. This is usually used to identify the {@link PipelineProcessor} that
     *            extracted this {@code AnnotationFeature}.
     */
    public AnnotationFeature(FeatureDescriptor<AnnotationFeature> descriptor) {
        this(descriptor.getIdentifier());
    }

    /**
     * <p>
     * Creates a new {@code AnnotationFeature} initialized with all {@code Annotation}s from the provided {@code List}
     * of annotations.
     * </p>
     * 
     * @param name A world wide unique identifier for all similar {@code AnnotationFeature}s. This is usually used to
     *            identify the {@link PipelineProcessor} that extracted this {@code AnnotationFeature}.
     * @param annotations The initial {@code List} of {@code Annotation}s of this feature.
     */
    public AnnotationFeature(String name, List<Annotation> annotations) {
        super(name, annotations);
    }

    /**
     * <p>
     * Creates a new {@code AnnotationFeature} initialized with all {@code Annotation}s from the provided {@code List}
     * of annotations.
     * </p>
     * 
     * @param descriptor The {@link FeatureDescriptor} with a world wide unique identifier for all similar
     *            {@code AnnotationFeature}s. This is usually used to identify the {@link PipelineProcessor} that
     *            extracted this {@code AnnotationFeature}.
     * @param annotations The initial {@code List} of {@code Annotation}s of this feature.
     */
    public AnnotationFeature(FeatureDescriptor<AnnotationFeature> descriptor, List<Annotation> annotations) {
        this(descriptor.getIdentifier(), annotations);
    }

    /**
     * Add an {@link Annotation} to this Feature.
     * 
     * @param annotation
     */
    public void add(Annotation annotation) {
        getValue().add(annotation);
    }

    /**
     * <p>
     * Gives all {@link Annotation}s within the specified range.
     * </p>
     * 
     * @param startPosition
     * @param endPosition
     * @return
     */
    public List<Annotation> getAnnotations(int startPosition, int endPosition) {
        List<Annotation> result = new ArrayList<Annotation>();
        for (Annotation current : getValue()) {
            if (current.getStartPosition() >= startPosition && current.getEndPosition() <= endPosition) {
                result.add(current);
            }
        }
        return result;
    }

    /**
     * Return a human-readable list of all contained {@link Annotation}s.
     * 
     * @return
     */
    public String toStringList() {
        StringBuilder sb = new StringBuilder();
        List<Annotation> annotations = getValue();
        for (Annotation annotation : annotations) {
            sb.append(annotation).append("\n");
        }
        return sb.toString();
    }

}
