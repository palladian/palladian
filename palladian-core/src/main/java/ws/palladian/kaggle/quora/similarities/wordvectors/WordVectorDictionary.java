package ws.palladian.kaggle.quora.similarities.wordvectors;

public interface WordVectorDictionary {
	float[] getVector(String word);
	int size();
	int vectorSize();
}
