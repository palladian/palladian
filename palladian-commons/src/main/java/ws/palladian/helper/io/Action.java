package ws.palladian.helper.io;

/**
 * A generic action which is performed on a given item.
 * 
 * @author pk
 * @param <T> Type of the item for which to perform actions.
 */
public interface Action<T> {

    /**
     * @param item The item to process.
     */
    void process(T item);

}
