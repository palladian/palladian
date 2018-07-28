package ws.palladian.classification.evaluation;

import static ws.palladian.helper.functional.Filters.not;

import java.util.Iterator;

import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.functional.Filter;

public class ChunkBasedCrossValidator implements CrossValidator {

	private final class FilterImplementation implements Filter<Instance> {
		private final int fold;
		FilterImplementation(int fold) {
			this.fold = fold;
		}
		@Override
		public boolean accept(Instance item) {
			String id = item.getVector().getNominal("Id").getString();
			return Integer.parseInt(id) / ChunkBasedSplit.CHUNK_SIZE % numFolds == fold;
		}
	}

	private final class FoldImplementation implements Fold {
		private final int fold;
		FoldImplementation(int fold) {
			this.fold = fold;
		}
		@Override
		public Dataset getTrain() {
			return dataset.subset(not(new FilterImplementation(fold)));
		}
		@Override
		public Dataset getTest() {
			return dataset.subset((new FilterImplementation(fold)));
		}
		@Override
		public int getFold() {
			return fold;
		}
	}
	
	private static final int DEFAULT_NUM_FOLDS = 5;
	private final Dataset dataset;
	private final int numFolds;

	public ChunkBasedCrossValidator(Dataset dataset, int numFolds) {
		this.dataset = dataset;
		this.numFolds = numFolds;
	}
	
	public ChunkBasedCrossValidator(Dataset dataset) {
		this(dataset, DEFAULT_NUM_FOLDS);
	}

	@Override
	public Iterator<Fold> iterator() {
		return new AbstractIterator2<Fold>() {
			int fold = 0;
			@Override
			protected Fold getNext() {
				if (fold < numFolds) {
					return new FoldImplementation(fold++);
				}
				return finished();
			}
		};
	}
	
	@Override
	public int getNumFolds() {
		return numFolds;
	}

//	public static void main(String[] args) {
//		Dataset trainingSet = Config.getTrain(10000).buffer();
//		ChunkBasedCrossValidator crossValidation = new ChunkBasedCrossValidator(trainingSet);
//		for (Fold fold : crossValidation) {
//			long trainSize = fold.getTrain().size();
//			long testSize = fold.getTest().size();
//			System.out.println(fold.getFold() + ": |train| = " + trainSize + ", |test| = " + testSize);
//		}
//		
//		Dataset trainSplit = crossValidation.iterator().next().getTrain();
//		for (Instance instance : trainSplit) {
//			System.out.println(instance.getVector().get("Id"));
//		}
//	}	

}
