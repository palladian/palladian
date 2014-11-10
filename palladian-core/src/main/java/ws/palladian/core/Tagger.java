package ws.palladian.core;

import java.util.List;

/**
 * <p>
 * A {@link Tagger} produces annotations in form of {@link Annotation} instances for a text. Examples for concrete
 * taggers are PoS taggers, Named Entity Recognizers, etc.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface Tagger {

    /**
     * <p>
     * Create annotations for the specified text. Implementations of this interface can (and should, if it makes sense)
     * narrow down the type of annotations which are created by providing a more concrete type than {@link Annotation} in
     * this method's return type.
     * </p>
     * 
     * @param text The text for which to create annotations, not <code>null</code>.
     * @return A list of {@link Annotation} instances.
     */
    public List<? extends Annotation> getAnnotations(String text);

}
