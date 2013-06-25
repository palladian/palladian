/**
 * Created on: 24.06.2013 13:06:53
 */
package ws.palladian.processing.features;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * <p>
 * A {@code ListFeature} groups features belonging to the same type such as tokens from a document. {@code ListFeature}s
 * are usually sparse features.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.2
 * @param <T>
 */
public final class ListFeature<T extends Feature<?>> extends AbstractFeature<List<T>> implements List<T> {

    /**
     * <p>
     * Creates a new completely initialized {@link ListFeature} with the provided name and value.
     * </p>
     * 
     * @param name The name of the new {@link ListFeature}. This is later used to retrieve this feature from a
     *            {@link FeatureVector}.
     * @param value The value of this {@link ListFeature}.
     */
    public ListFeature(String name, List<T> value) {
        super(name, new ArrayList<T>(value));
    }

    public ListFeature(String name) {
        super(name, new ArrayList<T>());
    }

    @Override
    public int size() {
        return getValue().size();
    }

    @Override
    public boolean isEmpty() {
        return getValue().isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return getValue().contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return getValue().iterator();
    }

    @Override
    public Object[] toArray() {
        return getValue().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return getValue().toArray(a);
    }

    @Override
    public boolean add(T e) {
        return getValue().add(e);
    }

    @Override
    public boolean remove(Object o) {
        return getValue().remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return getValue().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return getValue().addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return getValue().addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return getValue().removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return getValue().retainAll(c);
    }

    @Override
    public void clear() {
        getValue().clear();

    }

    @Override
    public T get(int index) {
        return getValue().get(index);
    }

    @Override
    public T set(int index, T element) {
        return getValue().set(index, element);
    }

    @Override
    public void add(int index, T element) {
        getValue().add(index, element);

    }

    @Override
    public T remove(int index) {
        return getValue().remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return getValue().indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return getValue().lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return getValue().listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return getValue().listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return getValue().subList(fromIndex, toIndex);
    }
}
