package com.newsseecr.xperimental;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.AbstractBagDecorator;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.log4j.Logger;

/**
 * Decorates/wraps a {@link Bag} and allows polling of the object with highest occurrence.
 * Example: For a Bag containing {a, a, b, b, b, c} {@link #poll()} will return "b" and remove "b" from the Bag.
 * 
 * *** This is not used at the moment, but could be useful in the future. ***
 * 
 * @author Philipp Katz
 * 
 * @param <E>
 */
public class PollBag<E> extends AbstractBagDecorator<E> implements Queue<E>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(PollBag.class);

    public PollBag() {
        super(new HashBag<E>());
    }

    public PollBag(Bag<E> decorated) {
        super(decorated);
    }

    // -----------------------------------------------------------------------
    // Queue API
    // -----------------------------------------------------------------------

    /**
     * Polls the object from the Bag which has the highest count.
     * 
     * @return object with highest number of occurences, <code>null</code> if no objects left.
     */
    @Override
    public E poll() {
        E result = peek();
        if (result != null) {
            getBag().remove(result);
        }
        LOGGER.trace("poll: " + result);
        return result;
    }

    @Override
    public E peek() {
        int highest = 0;
        E result = null;
        Iterator<E> iterator = getBag().uniqueSet().iterator();
        while (iterator.hasNext()) {
            E temp = iterator.next();
            int current = getBag().getCount(temp);
            if (current > highest) {
                result = temp;
                highest = current;
            }
        }
        LOGGER.trace("peek: " + result + " @ " + highest);
        return result;
    }

    @Override
    public E element() {
        E result = peek();
        if (result == null) {
            throw new NoSuchElementException();
        }
        return result;
    }

    @Override
    public boolean offer(E e) {
        return getBag().add(e);
    }

    @Override
    public E remove() {
        E result = poll();
        if (result == null) {
            throw new NoSuchElementException();
        }
        return result;
    }

    @Override
    public int size() {
        return getBag().uniqueSet().size();
    }
    // -----------------------------------------------------------------------
    // Additional methods
    // -----------------------------------------------------------------------
    public Bag<E> poll(int numItems) {
        Bag<E> result = new HashBag<E>();
        for (int i = 0; i < numItems; i++) {
            E element = peek();
            int count = getBag().getCount(element);
            result.add(element, count);
            poll();
        }
        return result;
    }


    // -----------------------------------------------------------------------
    /**
     * Write the collection out using a custom routine.
     * 
     * @param out the output stream
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(collection);
    }

    /**
     * Read the collection in using a custom routine.
     * 
     * @param in the input stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        collection = (Collection<E>) in.readObject();
    }

    @Override
    public String toString() {
        return getBag().toString();
    }


}
