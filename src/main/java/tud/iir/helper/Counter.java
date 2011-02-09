package tud.iir.helper;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.mutable.MutableInt;

/**
 * Simple and thread safe up/down counter
 * 
 * We could eliminate this class;
 * a) For simple applications, use int.
 * b) In callbacks use {@link MutableInt}.
 * c) If we need Thread-safety, use {@link AtomicInteger}.
 * 
 * @author Philipp Katz
 */
public class Counter {

    // private int count = 0;
    private AtomicInteger count = new AtomicInteger();

    public /*synchronized*/ int increment() {
        // return ++count;
        return count.incrementAndGet();
    }

    public /*synchronized*/ int decrement() {
        // return --count;
        return count.decrementAndGet();
    }

    public /*synchronized*/ int increment(int by) {
        // count += by;
        // return count;
        return count.addAndGet(by);
    }

    public /*synchronized*/ int getCount() {
        // return count;
        return count.get();
    }

    public /*synchronized*/ void reset() {
        // count = 0;
        count.set(0);
    }
    
    @Override
    public String toString() {
        return String.valueOf(getCount());
    }

}