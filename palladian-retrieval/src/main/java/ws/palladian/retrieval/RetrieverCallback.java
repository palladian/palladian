package ws.palladian.retrieval;


/**
 * An interface for the RetrieverCallback.
 * 
 * @author David Urbansky
 */
public interface RetrieverCallback<T> {

    void onFinishRetrieval(T document);

}
