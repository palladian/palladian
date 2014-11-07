package ws.palladian.helper.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * A simple, thread-safe round robin list.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class RoundRobinList<E> {

    private List<E> syncList = Collections.synchronizedList(new ArrayList<E>());

    private AtomicInteger index = new AtomicInteger();

    public synchronized E getNextItem() {
        E item = syncList.get(index.getAndIncrement());
        if (index.get() >= syncList.size()) {
            index.set(0);
        }

        return item;
    }

    public synchronized boolean remove(Object o) {
        Boolean remove = syncList.remove(o);
        if (remove && index.get() > 0) {
            index.decrementAndGet();
        }
        return remove;
    }

    public void add(E item) {
        syncList.add(item);
    }

    public boolean contains(E item) {
        return syncList.contains(item);
    }

    public boolean isEmpty() {
        return syncList.isEmpty();
    }

    public int size() {
        return syncList.size();
    }
}
