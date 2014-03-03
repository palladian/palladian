package ws.palladian.helper.collection;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import ws.palladian.helper.io.Action;

/**
 * Adapter between a producer which uses an {@link Action} callback and an {@link Iterator}.
 * 
 * @author pk
 * 
 * @param <T>
 */
public abstract class ActionIteratorAdapter<T> {

    /** The poison pill to signal the consumer to stop. */
    private static final Object POISON = new Object();

    /** Exception thrown when the producer should stop. */
    private static final RuntimeException STOP_PRODUCING = new RuntimeException();

    /** The sleep time in milliseconds between each attempt to put to the transfer queue. */
    private static final int SLEEP_MS_BETWEEN_QUEUE_PUT = 10;

    /** Size of the transfer queue. */
    private static final int QUEUE_SIZE = 10;

    /** A blocking queue with capacity of one, servers as transfer mechanism between producer and consumer. */
    private final BlockingQueue<T> transferQueue = new LinkedBlockingQueue<T>(QUEUE_SIZE);

    /** State to signal the producer when to stop. */
    private volatile boolean producing = true;

    public ActionIteratorAdapter() {
        new Thread(ActionIteratorAdapter.class.getSimpleName() + "-ProducerThread") {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                try {
                    produce(new QueueAction());
                } catch (Exception e) {
                    if (e == STOP_PRODUCING || e.getCause() == STOP_PRODUCING) {
                        return;
                    }
                    throw new IllegalStateException(e);
                }
                try {
                    transferQueue.put((T)POISON);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        }.start();
        try {
            consume(new QueueIterator());
        } finally {
            producing = false;
        }
    }

    /**
     * <p>
     * Implementors override this method and connect the given action to the producer.
     * 
     * @param action The action.
     * @throws Exception In case, something goes wrong.
     */
    protected abstract void produce(Action<T> action) throws Exception;

    /**
     * <p>
     * Implementors override this method and use the provided iterator as desired.
     * 
     * @param iterator The iterator with data from the producer.
     */
    protected abstract void consume(Iterator<T> iterator);
    
    private class QueueIterator extends AbstractIterator<T> {
        @Override
        protected T getNext() throws Finished {
            try {
                T element = transferQueue.take();
                if (element == POISON) {
                    throw new Finished();
                }
                return element;
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private class QueueAction implements Action<T> {

        @Override
        public void process(T item) {
            try {
                while (!transferQueue.offer(item, SLEEP_MS_BETWEEN_QUEUE_PUT, TimeUnit.MILLISECONDS)) {
                    if (!producing) {
                        throw STOP_PRODUCING;
                    }
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
//            if (!producing) {
//                throw STOP_PRODUCING;
//            }
//            try {
//                transferQueue.put(item);
//            } catch (InterruptedException e) {
//                throw new IllegalStateException(e);
//            }
        }
    }


}
