package ws.palladian.processing;

import ws.palladian.classification.Classifier;


/**
 * <p>
 * A {@link PipelineDocument} which may be used as training example for a {@link Classifier}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0.0
 * @since 0.1.8
 */
public class ClassifiedTextDocument extends TextDocument implements Trainable {

    // XXX replace by TrainableWrap

    /**
     * <p>
     * The target class this {@code Instance} belongs to.
     * </p>
     */
    private final String targetClass;
    
    /**
     * <p>
     * Creates a new completely initialized {@link PipelineDocument}.
     * </p>
     * 
     * @param targetClass The target class this {@code Instance} belongs to.
     * @param content The content of this {@code PipelineDocument}.
     */
    public ClassifiedTextDocument(String targetClass, String content) {
        super(content);
        this.targetClass = targetClass; 
    }

    @Override
    public String getTargetClass() {
        return targetClass;
    }

}
