package ws.palladian.helper.functional;

/**
 * A generic action which is performed on a given item.
 * 
 * @author pk
 * @param <T> Type of the item for which to perform actions.
 */
public interface Consumer<T> {

    /**
     * @param item The item to process.
     */
    void process(T item);

}
