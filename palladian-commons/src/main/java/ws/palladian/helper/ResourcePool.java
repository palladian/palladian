package ws.palladian.helper;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ResourcePool<T> {
    protected final BlockingQueue<T> pool;
    private final ReentrantLock lock = new ReentrantLock();
    private int createdObjects = 0;
    protected final int size;

    protected ResourcePool(int size) {
        // enable the fairness; otherwise, some threads may wait forever.
        pool = new ArrayBlockingQueue<>(size, true);
        this.size = size;
        lock.lock();
    }

    public T acquire() throws Exception {
        if (!lock.isLocked()) {
            if (lock.tryLock()) {
                try {
                    ++createdObjects;
                    return createObject();
                } finally {
                    if (createdObjects < size) {
                        lock.unlock();
                    }
                }
            }
        }
        return pool.take();
    }

    public void recycle(T resource) {
        if (resource != null) {
            pool.add(resource);
        }
    }

    public void initializePool() {
        if (lock.isLocked()) {
            for (int i = 0; i < size; ++i) {
                pool.add(createObject());
                createdObjects++;
            }
        }
    }

    protected abstract T createObject();
}