package ws.palladian.persistence.json;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Iterator through a json database.
 *
 * @author David Urbansky
 * @since 25-Feb-22 at 15:28
 **/
public abstract class JsonDbIterator<T> implements Iterator<T> {
    protected AtomicInteger index = new AtomicInteger(0);
    protected int totalCount = 0;

    public void setIndex(int index) {
        this.index.set(index);
    }

    public int getIndex() {
        return index.get();
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getTotalCount() {
        return totalCount;
    }
}
