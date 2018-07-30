package ws.palladian.classification.evaluation;

import static ws.palladian.helper.functional.Filters.not;

import java.util.Iterator;
import java.util.Objects;
import java.util.Random;

import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Factory;
import java.util.function.Predicate;

public class RandomCrossValidator implements CrossValidator {

	public final class RandomFold implements Fold {
		private final int fold;
		private RandomFold(int fold) {
			this.fold = fold;
		}
		@Override
		public Dataset getTrain() {
			return data.subset(new Factory<Predicate<Object>>() {
				@Override
				public Predicate<Object> create() {
					 return not(new FoldAssignmentFilter(fold));
				}
			});
		}
		@Override
		public Dataset getTest() {
			return data.subset(new Factory<Predicate<Object>>() {
				@Override
				public Predicate<Object> create() {
					 return new FoldAssignmentFilter(fold);
				}
			});
		}
		/**
		 * @return Index of the current fold.
		 */
		@Override
		public int getFold() {
			return fold;
		}
		@Override
		public String toString() {
			return "Fold " + fold;
		}
	}
	
	private final class FoldIterator extends AbstractIterator2<Fold> {
		private int currentFold = 0;
		@Override
		protected Fold getNext() {
			if (currentFold == numFolds) {
				return finished();
			}
			return new RandomFold(currentFold++);
		}
	}
	
	/**
	 * Index-based filter which accepts items based on the pre-calculated
	 * foldAssigments array.
	 */
	private final class FoldAssignmentFilter implements Predicate<Object> {
		private final int fold;
		private int currentIndex;
		public FoldAssignmentFilter(int fold) {
			this.fold = fold;
		}
		@Override
		public boolean test(Object item) {
			return foldAssignments[currentIndex++] == fold;
		}
	}

	private final Dataset data;
	
	private final int numFolds;
	
	private final int[] foldAssignments;
	
	/**
	 * Create a cross-validator for leave-one-out validation. (i.e. number of
	 * folds == num items in dataset).
	 * 
	 * @param data
	 *            The dataset.
	 */
	public RandomCrossValidator(Dataset data) {
		this(data, CollectionHelper.count(data.iterator()));
	}

	public RandomCrossValidator(Dataset data, int numFolds) {
		this.data = Objects.requireNonNull(data);
		if (numFolds <= 2) {
			throw new IllegalArgumentException("numFolds must be at least 2");
		}
		this.numFolds = numFolds;
		foldAssignments = calculateFoldAssignments(data, numFolds);
	}

	private static int[] calculateFoldAssignments(Iterable<? extends Instance> data, int numFolds) {
		int numInstances = CollectionHelper.count(data.iterator());
		int[] foldAssignments = new int[numInstances];
		for (int i = 0; i < numInstances; i++) {
			foldAssignments[i] = i % numFolds;
		}
		shuffle(foldAssignments);
		return foldAssignments;
	}
	
//	private static int[] calculateFoldAssignments(Iterable<? extends Instance> data, int numFolds) {
//		int numInstances = CollectionHelper.count(data.iterator());
//		int[] foldAssignments = new int[numInstances];
//		Random random = new Random();
//		for (int i = 0; i < numInstances; i++) {
//			foldAssignments[i] = random.nextInt(numFolds);
//		}
//		return foldAssignments;
//	}

	private static void shuffle(int[] array) {
        Random rnd = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            int item = array[index];
            array[index] = array[i];
            array[i] = item;
        }
	}

	@Override
	public Iterator<Fold> iterator() {
		return new FoldIterator();
	}
	
	@Override
	public int getNumFolds() {
		return numFolds;
	}

}
