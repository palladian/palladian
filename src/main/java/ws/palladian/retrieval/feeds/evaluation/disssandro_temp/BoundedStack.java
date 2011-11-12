package ws.palladian.retrieval.feeds.evaluation.disssandro_temp;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * A bounded stack with limited capacity, based on a {@link LinkedList}. In case the stack is full and a new item is
 * pushed to the stack, the oldest item is silently removed.
 * 
 * implementation based on posting by Zarkonnen at
 * http://stackoverflow.com/questions/4003203/bounded-auto-discarding-non-blocking-concurrent-collection
 * 
 * @author Sandro Reichert
 * 
 * @param <T>
 */
public class BoundedStack<T> implements Iterable<T> {
    private final LinkedList<T> linkedList = new LinkedList<T>();

    /**
     * The stack has a maximum of that many elements.
     */
    private final int bound;

    public BoundedStack(int bound) {
        this.bound = bound;
    }

    /**
     * Push item on top of the stack. In case the stack is full and a new item is pushed, the oldest item is
     * silently removed.
     * 
     * @param item
     */
    public void push(T item) {
        linkedList.push(item);
        if (linkedList.size() > bound) {
            linkedList.remove(linkedList.size() - 1);
        }
    }

    /**
     * Pops an element from the stack represented by this list. In other words, removes and returns the first element of
     * this list. In case the stack is empty, <code>null</code> is returned.
     * 
     * @return The element on top of the stack or <code>null</code> in case the stack is empty.
     */
    public T pop() {
        return linkedList.isEmpty() ? null : linkedList.pop();
    }

    /**
     * Get the element from this position on the stack. The most recently added element is at position 0. The element is
     * not removed from the stack.
     * 
     * @param position The position on the stack to get the element from.
     * @return The element at the specified position or <code>null</code> if there is no such position on the stack.
     */
    public T getElement(int position) {
        T element = null;
        try{
            element = linkedList.get(position);
        } catch (IndexOutOfBoundsException e) {
            element = null;
        }
        return element;
    }

    /**
     * Get the top element from the stack. The element is not removed from the stack.
     * 
     * @return The top element from the stack, or <code>null</code> if the stack is empty.
     */
    public T getFirst() {
        return getElement(0);
    }

    /**
     * Get the bottom element from the stack. The element is not removed from the stack.
     * 
     * @return The bottom element from the stack, or <code>null</code> if the stack is empty.
     */
    public T getLast() {
        return getElement(linkedList.size() - 1);
    }

    /**
     * Gets the maximum size of the stack (the bound).
     * 
     * @return the maximum number of elements the stack can hold.
     */
    public int maxSize() {
        return bound;
    }

    /**
     * Returns the number of elements stored in the stack.
     * 
     * @return this stack's size
     */
    public int size() {
        return linkedList.size();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<T> iterator() {
        return linkedList.iterator();
    }

    // public static void main(String[] args) {
    // BoundedStack<FeedItem> stack = new BoundedStack<FeedItem>(10);
    // for (int i = 1; i <= 1000; i++) {
    // FeedItem item = new FeedItem();
    // item.setId(i);
    // stack.push(item);
    // }
    // stack.pop();
    // FeedItem item2 = new FeedItem();
    // item2.setId(5000);
    // stack.push(item2);
    //
    // for (FeedItem item : stack) {
    // System.out.println(item.getId());
    // }
    //
    // System.out.println("First Element: " + stack.getFirst());
    // System.out.println("Last Element: " + stack.getLast());
    //
    // System.out.println("Element at position 2: " + stack.getElement(0));
    //
    // // CircularFifoBuffer<FeedItem> itemBuffer = new CircularFifoBuffer<FeedItem>(1000);
    // // for (int i = 1; i <= 1000; i++) {
    // // FeedItem item = new FeedItem();
    // // item.setId(i);
    // // itemBuffer.add(item);
    // // }
    // // Iterator<FeedItem> it = itemBuffer.iterator();
    //
    // // for (FeedItem item : itemBuffer) {
    // // System.out.println(item.getId());
    // // }
    // }

}