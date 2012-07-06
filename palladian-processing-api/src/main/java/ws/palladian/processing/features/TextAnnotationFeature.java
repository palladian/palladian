package ws.palladian.processing.features;

import java.util.List;

public final class TextAnnotationFeature extends AnnotationFeature<String> {

    /**
     * @param descriptor
     * @param annotations
     */
    public TextAnnotationFeature(FeatureDescriptor<TextAnnotationFeature> descriptor,
            List<Annotation<String>> annotations) {
        super(descriptor, annotations);
    }

    /**
     * @param descriptor
     */
    public TextAnnotationFeature(FeatureDescriptor<TextAnnotationFeature> descriptor) {
        super(descriptor);
    }

    /**
     * @param name
     * @param annotations
     */
    public TextAnnotationFeature(String name, List<Annotation<String>> annotations) {
        super(name, annotations);
    }

    /**
     * @param name
     */
    public TextAnnotationFeature(String name) {
        super(name);
    }

}
