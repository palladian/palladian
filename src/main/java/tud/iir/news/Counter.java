package tud.iir.news;

/**
 * Simple and thread safe up/down counter
 */
public class Counter {

    private int count = 0;

    public synchronized void increment() {
        count++;
    }

    public synchronized void decrement() {
        count--;
    }

    public synchronized void increment(int by) {
        count += by;
    }

    public synchronized int getCount() {
        return count;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return String.valueOf(getCount());
    }
}