package ws.palladian.classification.evaluation;

import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;
import ws.palladian.core.dataset.Dataset;

public interface ClassificationEvaluator<R> {
	/** @deprecated Use {@link #evaluate(Learner, Classifier, Dataset, Dataset)} instead. */
	@Deprecated
	<M extends Model> R evaluate(Learner<M> learner, Classifier<M> classifier, Iterable<? extends Instance> trainData, Iterable<? extends Instance> testData);
	/** @deprecated Use {@link #evaluate(Classifier, Model, Dataset)} instead. */
	@Deprecated
	<M extends Model> R evaluate(Classifier<M> classifier, M model, Iterable<? extends Instance> data);

	<M extends Model> R evaluate(Learner<M> learner, Classifier<M> classifier, Dataset trainData, Dataset testData);
	<M extends Model> R evaluate(Classifier<M> classifier, M model, Dataset data);
	
	// TODO probably move this logic into the evaluation result; introduce
	// EvaluationResult class for that (i.e. type parameter R)
	
	/**
	 * Get the header for writing the evaluation result to a CSV file.
	 * 
	 * @param result
	 *            The result.
	 * @return The CSV header.
	 */
	String getCsvHeader(R result);

	/**
	 * Get summarized string result for adding the evaluation result to a CSV
	 * file.
	 * 
	 * @param result
	 *            The result.
	 * @return The CSV line.
	 * @see #getCsvHeader(Object)
	 */
	String getCsvLine(R result);
}
