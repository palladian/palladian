package ws.palladian.helper.functional;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import ws.palladian.helper.collection.AbstractIterator;

/**
 * Adapter between a producer which uses a {@link Consumer} callback and an {@link Iterator}.
 * 
 * @author pk
 * 
 * @param <T>
 */
public abstract class ConsumerIteratorAdapter<T> {

    /** The poison pill to signal the consumer to stop. */
    private static final Object POISON = new Object();

    /** Exception thrown when the producer should stop. */
    private static final RuntimeException STOP_PRODUCING = new RuntimeException();

    /** The sleep time in milliseconds between each attempt to put to the transfer queue. */
    private static final int SLEEP_MS_BETWEEN_QUEUE_PUT = 10;

    /** Size of the transfer queue. */
    private static final int QUEUE_SIZE = 10;

    /** State to signal the producer when to stop. */
    private volatile boolean producing = true;
    
    /** Count the number of threads, only for debugging purposes. */
    private int threadCount = 0;

    public ConsumerIteratorAdapter() {
        try {
            consume(new QueueIterable());
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
    protected abstract void produce(Consumer<T> action) throws Exception;

    /**
     * <p>
     * Implementors override this method and use the provided iterator as desired.
     * 
     * @param iterator The iterator with data from the producer.
     */
    protected abstract void consume(Iterable<T> iterable);

    private final class ProducerThread extends Thread {
        private final BlockingQueue<T> queue;

        private ProducerThread(BlockingQueue<T> queue) {
            super(ConsumerIteratorAdapter.class.getSimpleName() + "-ProducerThread-" + threadCount++);
            this.queue = queue;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            try {
                produce(new QueueAction(queue));
            } catch (Exception e) {
                if (e == STOP_PRODUCING || e.getCause() == STOP_PRODUCING) {
                    return;
                }
                throw new IllegalStateException(e);
            }
            try {
                queue.put((T)POISON);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private final class QueueIterable implements Iterable<T> {
        @Override
        public Iterator<T> iterator() {
            final BlockingQueue<T> queue = new LinkedBlockingQueue<T>(QUEUE_SIZE);
            new ProducerThread(queue).start();;
            return new AbstractIterator<T>() {
                @Override
                protected T getNext() throws Finished {
                    try {
                        T element = queue.take();
                        if (element == POISON) {
                            throw FINISHED;
                        }
                        return element;
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                }
            };
        }
    }

    private final class QueueAction implements Consumer<T> {
        private final BlockingQueue<T> queue;

        public QueueAction(BlockingQueue<T> queue) {
            this.queue = queue;
        }

        @Override
        public void process(T item) {
            try {
                while (!queue.offer(item, SLEEP_MS_BETWEEN_QUEUE_PUT, TimeUnit.MILLISECONDS)) {
                    if (!producing) {
                        throw STOP_PRODUCING;
                    }
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

}
