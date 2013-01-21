package ws.palladian.processing;

/**
 * <p>
 * The {@link Trainable} interface combines the {@link Classifiable} and {@link Classified} interfaces. This means, that
 * implementations of this interface can be serve as training instances, e.g. for classifiers. They provide descriptive
 * features via {@link Classifiable#getFeatureVector()} and have an assigned class which can be retrieved using
 * {@link Classified#getTargetClass()}.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface Trainable extends Classifiable, Classified {

}
