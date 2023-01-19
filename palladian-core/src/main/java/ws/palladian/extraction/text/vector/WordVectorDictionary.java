package ws.palladian.extraction.text.vector;

public interface WordVectorDictionary {
    float[] getVector(String word);

    int size();

    int vectorSize();

    boolean isCaseSensitive();
}
