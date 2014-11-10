package ws.palladian.helper.functional;

public interface Similarity<T> {
    
    double getSimilarity(T i1, T i2);

}
