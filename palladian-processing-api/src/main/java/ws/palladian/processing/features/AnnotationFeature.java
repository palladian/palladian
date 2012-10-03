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
public abstract class AnnotationFeature<T> extends Feature<List<Annotation<T>>> {

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
        super(name, new ArrayList<Annotation<T>>());
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
    public AnnotationFeature(FeatureDescriptor<? extends AnnotationFeature<T>> descriptor) {
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
    public AnnotationFeature(String name, List<Annotation<T>> annotations) {
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
    public AnnotationFeature(FeatureDescriptor<? extends AnnotationFeature<T>> descriptor,
            List<Annotation<T>> annotations) {
        this(descriptor.getIdentifier(), annotations);
    }

    /**
     * <p>
     * Add an {@link Annotation} to this Feature.
     * </p>
     * 
     * @param annotation The Annotation to add.
     */
    public void add(Annotation<T> annotation) {
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
    public List<Annotation<T>> getAnnotations(int startPosition, int endPosition) {
        List<Annotation<T>> result = new ArrayList<Annotation<T>>();
        for (Annotation<T> current : getValue()) {
            if (current.getStartPosition() >= startPosition && current.getEndPosition() <= endPosition) {
                result.add(current);
            }
        }
        return result;
    }

    public List<? extends Feature<T>> getFeatures(Class<? extends Feature<T>> class1, String identifier) {
        List features = new ArrayList();
        for (Annotation<T> current : getValue()) {
            features.add(current.getFeatureVector().get(class1, identifier));
        }

        return features;
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
        List<Annotation<T>> annotations = getValue();
        for (Annotation<T> annotation : annotations) {
            sb.append(annotation).append(NEWLINE);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        List<Annotation<T>> annotations = getValue();
        for (int i = 0; i < annotations.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(annotations.get(i).getValue());
        }
        return sb.toString();
    }

}
