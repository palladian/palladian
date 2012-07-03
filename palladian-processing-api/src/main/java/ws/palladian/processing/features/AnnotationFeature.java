package ws.palladian.processing.features;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;

/**
 * <p>
 * An {@link AnnotationFeature} is a feature consisting of a List of {@link Annotation}s. Annotations are pointers to
 * parts of text in a {@link PipelineDocument} which can be characterized by a {@link FeatureVector}.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class AnnotationFeature extends Feature<List<Annotation>> {

    private static final char NEWLINE = '\n';

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
     * <p>
     * Add an {@link Annotation} to this Feature.
     * </p>
     * 
     * @param annotation The Annotation to add.
     */
    public void add(Annotation annotation) {
        getValue().add(annotation);
    }

    /**
     * <p>
     * Retrieve all {@link Annotation}s within the specified range.
     * </p>
     * 
     * @param startPosition The start position from where to retrieve Annotations (inclusive).
     * @param endPosition The end position until where to retrieve Annotations (inclusive).
     * @return All Annotations within the specified range, or empty List.
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
     * <p>
     * Get a human-readable list of all contained {@link Annotation}s, separated by new lines.
     * </p>
     * 
     * @return
     */
    public String toStringList() {
        StringBuilder sb = new StringBuilder();
        List<Annotation> annotations = getValue();
        for (Annotation annotation : annotations) {
            sb.append(annotation).append(NEWLINE);
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        List<Annotation> annotations = getValue();
        for (int i = 0; i < annotations.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(annotations.get(i).getValue());
        }
        return sb.toString();
    }

}
