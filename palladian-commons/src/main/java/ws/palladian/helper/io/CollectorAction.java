package ws.palladian.helper.io;

import java.util.Collection;

public class CollectorAction<T> implements Action<T> {

    private final Collection<T> collection;

    public CollectorAction(Collection<T> collection) {
        this.collection = collection;
    }

    @Override
    public void process(T item) {
        collection.add(item);
    }

}
