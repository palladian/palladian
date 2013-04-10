package ws.palladian.extraction.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.features.Annotated;

/**
 * <p>
 * A list of {@link Annotated}s.
 * </p>
 * 
 * @author David Urbansky
 */
public class Annotations<T extends Annotated> implements List<T> {

    private final List<T> annotations = CollectionHelper.newArrayList();

    public Annotations() {
    }

    public Annotations(Collection<? extends T> c) {
        annotations.addAll(c);
    }

    /**
     * Save the annotation list to a file.
     * 
     * @param outputFilePath The path where the annotation list should be saved to.
     */
    public void save(String outputFilePath) {
        String output = toString();
        FileHelper.writeToFile(outputFilePath, output);
    }

    @Override
    public String toString() {
        sort();
        StringBuilder output = new StringBuilder();
        for (Annotated annotation : this) {
            output.append(annotation.getStartPosition()).append(";");
            output.append(annotation.getValue().length()).append(";");
            output.append(annotation.getEndPosition()).append(";");
            output.append(annotation.getValue()).append(";");
            output.append(annotation.getTag()).append("\n");
        }
        return output.toString();
    }

    public void removeNested() {
        sort();
        Iterator<T> iterator = iterator();
        int lastEndIndex = 0;
        while (iterator.hasNext()) {
            T annotation = iterator.next();
            // ignore nested annotations
            if (annotation.getStartPosition() < lastEndIndex) {
                iterator.remove();
                continue;
            }
            lastEndIndex = annotation.getEndPosition();
        }
    }

    /**
     * <p>
     * The order of annotations is important. Annotations are sorted by their offsets in ascending order.
     * </p>
     */
    public void sort() {
        Collections.sort(this);
    }

    @Override
    public boolean add(T e) {
        for (Annotated a : this) {
            if (a.getStartPosition() == e.getStartPosition()) {
                return false;
            }
        }
        return annotations.add(e);
    }

    @Override
    public void add(int index, T element) {
        annotations.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return annotations.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return annotations.addAll(index, c);
    }

    @Override
    public void clear() {
        annotations.clear();
    }

    @Override
    public boolean contains(Object o) {
        return annotations.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return annotations.containsAll(c);
    }

    @Override
    public boolean equals(Object o) {
        return annotations.equals(o);
    }

    @Override
    public T get(int index) {
        return annotations.get(index);
    }

    @Override
    public int hashCode() {
        return annotations.hashCode();
    }

    @Override
    public int indexOf(Object o) {
        return annotations.indexOf(o);
    }

    @Override
    public boolean isEmpty() {
        return annotations.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        return annotations.iterator();
    }

    @Override
    public int lastIndexOf(Object o) {
        return annotations.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return annotations.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return annotations.listIterator(index);
    }

    @Override
    public T remove(int index) {
        return annotations.remove(index);
    }

    @Override
    public boolean remove(Object o) {
        return annotations.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return annotations.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return annotations.retainAll(c);
    }

    @Override
    public T set(int index, T element) {
        return annotations.set(index, element);
    }

    @Override
    public int size() {
        return annotations.size();
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return annotations.subList(fromIndex, toIndex);
    }

    @Override
    public Object[] toArray() {
        return annotations.toArray();
    }

    @Override
    public <A> A[] toArray(A[] a) {
        return annotations.toArray(a);
    }

}
