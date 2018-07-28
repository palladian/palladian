//package ws.palladian.kaggle.redhat.dataset;
//
//import static ws.palladian.helper.functional.Filters.not;
//
//import java.io.File;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Random;
//
//import ws.palladian.classification.evaluation.CrossValidator;
//import ws.palladian.core.Instance;
//import ws.palladian.core.dataset.Dataset;
//import ws.palladian.helper.collection.AbstractIterator2;
//import ws.palladian.helper.functional.Filter;
//import ws.palladian.kaggle.redhat.dataset.sparse.SparseDatasetReader;
//
//public class PeopleIdCrossValidator implements CrossValidator {
//
//	private static final int NUM_FOLDS = 10;
//
//	private final class PeopleIdFold implements Fold {
//
//		private final Filter<Instance> filter;
//		private final int currentIndex;
//
//		private PeopleIdFold(int currentIndex) {
//			filter = new Filter<Instance>() {
//				@Override
//				public boolean accept(Instance item) {
//					String peopleId = item.getVector().get("people_id").toString();
//					return foldAssignments.get(peopleId) == currentIndex;
//				}
//			};
//			this.currentIndex = currentIndex;
//		}
//
//		@Override
//		public Dataset getTrain() {
//			return dataset.subset(filter);
//		}
//
//		@Override
//		public Dataset getTest() {
//			return dataset.subset(not(filter));
//		}
//
//		@Override
//		public int getFold() {
//			return currentIndex;
//		}
//
//		@Override
//		public String toString() {
//			long trainSize = getTrain().size();
//			long testSize = getTest().size();
//			double ratio = (double) trainSize / testSize;
//			return String.format("Fold %d: |train| = %d |test| = %d (ratio = %.2f)", getFold(), trainSize, testSize,
//					ratio);
//		}
//
//	}
//
//	private final Dataset dataset;
//	private final Map<String, Integer> foldAssignments;
//
//	public PeopleIdCrossValidator(Dataset dataset) {
//		this.dataset = Objects.requireNonNull(dataset);
//
//		Map<String, Integer> foldAssignments = new HashMap<>();
//		Random random = new Random(1234567l);
//
//		for (Instance instance : dataset) {
//			String peopleId = instance.getVector().get("people_id").toString();
//			if (!foldAssignments.containsKey(peopleId)) {
//				foldAssignments.put(peopleId, random.nextInt(NUM_FOLDS));
//			}
//		}
//
//		this.foldAssignments = Collections.unmodifiableMap(foldAssignments);
//	}
//
//	@Override
//	public Iterator<Fold> iterator() {
//		return new AbstractIterator2<Fold>() {
//
//			int currentIndex = 0;
//
//			@Override
//			protected Fold getNext() {
//				if (currentIndex < NUM_FOLDS) {
//					return new PeopleIdFold(currentIndex++);
//				}
//				return finished();
//			}
//		};
//	}
//	
//	@Override
//	public int getNumFolds() {
//		return NUM_FOLDS;
//	}
//
//	public static void main(String[] args) {
//		String sparseTraining = "/Users/pk/temp/kaggle-red-hat-train.sparse";
//		Dataset trainSet = new SparseDatasetReader(new File(sparseTraining));
//		PeopleIdCrossValidator crossValidator = new PeopleIdCrossValidator(trainSet);
//
//		StringBuilder result = new StringBuilder();
//		for (Fold fold : crossValidator) {
//			result.append(fold.toString()).append('\n');
//		}
//		System.out.println(result);
//
//	}
//
//}
