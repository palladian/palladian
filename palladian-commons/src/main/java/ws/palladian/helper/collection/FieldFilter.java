package ws.palladian.helper.collection;

public interface FieldFilter<T, S> {

    S getField(T item);

}
