package ws.palladian.persistence;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * A {@link BatchDataProvider} for a {@link Collection}. Only the conversion method needs to be implemented, in case,
 * one cares about the inserted IDs, optionally implement {@link #insertedItem(int, int)}.
 * </p>
 * 
 * @author pk
 * 
 * @param <T>
 */
public abstract class CollectionBatchDataProvider<T> implements BatchDataProvider {

    private final Iterator<? extends T> iterator;

    private final int count;

    public CollectionBatchDataProvider(Collection<? extends T> collection) {
        Validate.notNull(collection, "collection must not be null");
        this.iterator = collection.iterator();
        this.count = collection.size();
    }

    @Override
    public final List<? extends Object> getData(int number) {
        return getData(iterator.next());
    }

    /**
     * @param nextItem the item in the {@link Collection} which to convert.
     * @return List with parameters for the SQL statement, not <code>null</code>.
     */
    public abstract List<? extends Object> getData(T nextItem);

    /**
     * Override in case, you need to retrieve the generated IDs.
     */
    @Override
    public void insertedItem(int number, int generatedId) {
        // no operation per default.
    }

    @Override
    public final int getCount() {
        return count;
    }

}
