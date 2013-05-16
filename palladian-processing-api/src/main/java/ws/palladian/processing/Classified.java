package ws.palladian.processing;

/**
 * <p>
 * Interface implemented by all classes that are available as training instances for a {@link Classifier}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0.0
 * @since 0.1.8
 */
public interface Classified {
    /**
     * @return Provides the class this {@link Classified} belongs to and which should be used to train new
     *         {@link Model}s.
     */
    String getTargetClass();
}
