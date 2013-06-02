/**
 * Created on: 25.05.2013 13:29:41
 */
package ws.palladian.processing.features;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * <p>
 * A feature containing multiple keys and values per document. Not all keys need to be present for each document.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.1
 */
public final class SparseFeature<R> extends Feature<Pair<String, R>> {

    /**
     * <p>
     * Creates a new completely initialized {@link SparseFeature} with the provided name and key value pair as the
     * {@link Feature}s value.
     * </p>
     * 
     * @param name The name of the new feature.
     * @param value The key value pair of the new feature.
     */
    public SparseFeature(String name, String identifier, R value) {
        super(name, new ImmutablePair<String, R>(identifier, value));
    }

    public String getIdentifier() {
        return getValue().getKey();
    }

}
