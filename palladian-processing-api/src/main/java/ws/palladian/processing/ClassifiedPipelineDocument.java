/**
 * 
 */
package ws.palladian.processing;

import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * A {@link PipelineDocument} which may be used as training example for a {@link Classifier}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0.0
 * @since 0.1.8
 */
public final class ClassifiedPipelineDocument<T> extends PipelineDocument<T> implements Classified {

    /**
     * <p>
     * The target class this {@code Instance} belongs to.
     * </p>
     */
    private String targetClass;
    
    /**
     * <p>
     * Creates a new completely initialized {@link PipelineDocument}.
     * </p>
     * 
     * @param targetClass The target class this {@code Instance} belongs to.
     * @param content The content of this {@code PipelineDocument}.
     */
    public ClassifiedPipelineDocument(String targetClass, T content) {
        super(content);
        
        this.targetClass = targetClass; 
    }

    @Override
    public FeatureVector getFeatureVector() {
        return super.getFeatureVector();
    }

    @Override
    public String getTargetClass() {
        return targetClass;
    }

}
