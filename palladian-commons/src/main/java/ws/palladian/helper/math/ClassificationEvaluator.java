package ws.palladian.helper.math;

public interface ClassificationEvaluator {

	// TODO change to void add(String real, CategoryEntries predicted)
	void add(boolean relevant, double confidence);

}
