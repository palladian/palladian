package ws.palladian.helper.collection;

public interface Filter<T> {
    
    boolean accept(T item);

}
