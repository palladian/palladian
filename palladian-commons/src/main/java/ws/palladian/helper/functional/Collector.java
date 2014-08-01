package ws.palladian.helper.functional;

import java.util.Collection;

public class Collector<T> implements Consumer<T> {

    private final Collection<T> collection;

    public Collector(Collection<T> collection) {
        this.collection = collection;
    }

    @Override
    public void process(T item) {
        collection.add(item);
    }

}
