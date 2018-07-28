//package ws.palladian.kaggle.restaurants.classifier.nn;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Objects;
//
//import org.deeplearning4j.datasets.iterator.DataSetIterator;
//import org.nd4j.linalg.api.ndarray.INDArray;
//import org.nd4j.linalg.dataset.DataSet;
//import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
//import org.nd4j.linalg.factory.Nd4j;
//
//import ws.palladian.core.FeatureVector;
//import ws.palladian.core.Instance;
//import ws.palladian.core.dataset.Dataset;
//import ws.palladian.core.dataset.statistics.DatasetStatistics;
//import ws.palladian.core.value.NumericValue;
//import ws.palladian.core.value.Value;
//import ws.palladian.helper.collection.CollectionHelper;
//
//public class DataSetIteratorAdapter implements DataSetIterator {
//
//	// implemented according to the following examples:
//
//	// org.deeplearning4j.datasets.canova.RecordReaderDataSetIterator
//	// org.deeplearning4j.examples.recurrent.word2vecsentiment
//
//	// XXX should be configurable?
//	private static final int NULL_VALUE_REPLACEMENT = -999;
//
//	private static final long serialVersionUID = 1L;
//
//	private final Dataset dataset;
//
//	private final int batchSize;
//
//	private final int totalExamples;
//
//	private final List<String> featureNames;
//
//	private final List<String> labelNames;
//
//	private int cursor = 0;
//
//	private Iterator<? extends Instance> currentIterator;
//
//	/**
//	 * Create a new {@link DataSetIteratorAdapter}.
//	 * 
//	 * @param dataset
//	 *            The dataset.
//	 * @param batchSize
//	 *            The default batch size to use. When invoking {@link #next()}
//	 *            the specified number of instances is returned.
//	 */
//	public DataSetIteratorAdapter(Dataset dataset, int batchSize) {
//		this.dataset = Objects.requireNonNull(dataset, "instances must not be null");
//		if (batchSize < 1) {
//			throw new IllegalArgumentException("batchSize must be greater zero");
//		}
//		this.batchSize = batchSize;
//		totalExamples = CollectionHelper.count(dataset.iterator());
//		featureNames = new ArrayList<>(dataset.getFeatureInformation().getFeatureNamesOfType(NumericValue.class));
//		labelNames = new ArrayList<>(new DatasetStatistics(dataset).getCategoryStatistics().getValues());
//		Collections.sort(labelNames);
//	}
//
//	@Override
//	public boolean hasNext() {
//		return currentIterator.hasNext();
//	}
//
//	@Override
//	public DataSet next() {
//		return next(batchSize);
//	}
//
//	@Override
//	public DataSet next(int num) {
//		List<INDArray> inputs = new ArrayList<>();
//		List<INDArray> labels = new ArrayList<>();
//		for (int i = 0; i < num && hasNext(); i++, cursor++) {
//			Instance currentInstance = currentIterator.next();
//			inputs.add(createFvArray(currentInstance.getVector(), featureNames));
//			labels.add(createLabelArray(currentInstance.getCategory()));
//		}
//		return new DataSet(Nd4j.vstack(inputs.toArray(new INDArray[0])), Nd4j.vstack(labels.toArray(new INDArray[0])));
//	}
//
//	@Override
//	public int totalExamples() {
//		return totalExamples;
//	}
//
//	@Override
//	public int inputColumns() {
//		return featureNames.size();
//	}
//
//	@Override
//	public int totalOutcomes() {
//		return labelNames.size();
//	}
//
//	@Override
//	public void reset() {
//		currentIterator = dataset.iterator();
//	}
//
//	@Override
//	public int batch() {
//		return batchSize;
//	}
//
//	@Override
//	public int cursor() {
//		return cursor;
//	}
//
//	@Override
//	public int numExamples() {
//		return totalExamples();
//	}
//
//	@Override
//	public void setPreProcessor(DataSetPreProcessor preProcessor) {
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	public List<String> getLabels() {
//		// TODO check whether this is correct
//		return Collections.unmodifiableList(labelNames);
//	}
//
//	public List<String> getFeatureNames() {
//		return Collections.unmodifiableList(featureNames);
//	}
//
//	static INDArray createFvArray(FeatureVector featureVector, List<String> featureNames) {
//		double[] vectorArray = new double[featureNames.size()];
//		for (int idx = 0; idx < featureNames.size(); idx++) {
//			String featureName = featureNames.get(idx);
//			Value value = featureVector.get(featureName);
//			if (value.isNull()) {
//				vectorArray[idx] = NULL_VALUE_REPLACEMENT;
//			} else {
//				NumericValue featureValue = (NumericValue) value;
//				vectorArray[idx] = featureValue.getDouble();
//			}
//		}
//		return Nd4j.create(vectorArray);
//	}
//
//	private INDArray createLabelArray(String categoryName) {
//		double[] labelVector = new double[getLabels().size()];
//		labelVector[getLabels().indexOf(categoryName)] = 1;
//		return Nd4j.create(labelVector);
//	}
//
//}
