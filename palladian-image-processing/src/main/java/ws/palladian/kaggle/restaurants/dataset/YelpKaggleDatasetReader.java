package ws.palladian.kaggle.restaurants.dataset;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.value.ImmutableIntegerValue;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.dataset.ImageValue;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.kaggle.restaurants.utils.Config;
import ws.palladian.utils.ModuloFilter;

public class YelpKaggleDatasetReader extends AbstractDataset {
	
	/** Denotes a subset of the training data in different sizes. */
	public static enum Subset {
		/** Approx. 1,000 images per set. */
		SMALL(200), 
		/** Approx. 10,000 images per set. */
		MEDIUM(20), 
		/** Approx. 100,000 images per set. */
		FULL(2);
		public final int mod;
		Subset(int mod) {
			this.mod = mod;
		}
		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}
	
	/**
	 * Splits the labeled data into a training and a validation set, based on
	 * the businesses, i.e. one businesses photos are either in the training or
	 * the validation set.
	 */
	public static enum BusinessFilter implements Filter<Instance> {
		TRAIN(true), VALIDATE(false);
		private final boolean b;
		private BusinessFilter(boolean b) {
			this.b = b;
		}
		@Override
		public boolean accept(Instance item) {
			Value businessIdValue = item.getVector().get("businessId");
//			if (businessIdValue == null || businessIdValue == NullValue.NULL) { // XXX inconsistent naming
//				businessIdValue = item.getVector().get("business_id");
//			}
//			return ((NumericValue) businessIdValue).getLong() % 2 == 0 ^ b;
			if (businessIdValue == null || businessIdValue == NullValue.NULL) {
				throw new IllegalArgumentException("businessId is missing");
			}
			return Integer.valueOf(businessIdValue.toString()) % 2 == 0 ^ b;
		}
	}

	private final File photoToBizCsv;

	private final File baseImagePath;

	private final Map<Integer, int[]> businessIdToLabels = new HashMap<>();

	private final long numPhotos;
	
	/**
	 * For the training data, we parse the labels, which are obviously not
	 * available for the testing data.
	 */
	private final boolean training;

	/**
	 * Read the Yelp training data.
	 * 
	 * @param photoToBizCsv
	 *            Path to the "train_photo_to_biz_ids.csv" file.
	 * @param trainCsv
	 *            Path to the "train.csv" file.
	 * @param baseImagePath
	 *            Path to the directory "train_photos"
	 * @throws IOException
	 */
	public YelpKaggleDatasetReader(File photoToBizCsv, File trainCsv, File baseImagePath) throws IOException {
		this.photoToBizCsv = Objects.requireNonNull(photoToBizCsv);
		this.baseImagePath = Objects.requireNonNull(baseImagePath);
		this.numPhotos = Files.lines(photoToBizCsv.toPath()).count() - 1;
		this.training = trainCsv != null;
		if (trainCsv != null) {
			try (Stream<String> lines = Files.lines(trainCsv.toPath())) {
				boolean[] first = { true };
				lines.forEach(line -> {
					if (first[0]) {
						first[0] = false;
						return;
					}
					String[] split = line.split(",");
					int businessId = Integer.valueOf(split[0]);
					int[] labels = split.length == 2
							? Arrays.stream(split[1].split(" ")).mapToInt(Integer::valueOf).toArray() : new int[0];
					businessIdToLabels.put(businessId, labels);
				});
			}
		}
	}
	
	/**
	 * Read the Yelp testing data (in this case, we have no "train.csv" file).
	 * 
	 * @param photoToBizCsv
	 *            Path to the "test_photo_to_biz.csv" file.
	 * @param baseImagePath
	 *            Path to the directory "test_photos"
	 * @throws IOException
	 */
	@Deprecated
	public YelpKaggleDatasetReader(File photoToBizCsv, File baseImagePath) throws IOException {
		this(photoToBizCsv, null, baseImagePath);
	}

	@Override
	public CloseableIterator<Instance> iterator() {
		return new DatasetIterator();
	}

	private final class DatasetIterator extends AbstractIterator<Instance> implements CloseableIterator<Instance> {
		private final Stream<String> lines;
		private final Iterator<String> iterator;
		private final ProgressReporter progress;

		DatasetIterator() {
			try {
				lines = Files.lines(photoToBizCsv.toPath());
				iterator = lines.iterator();
				iterator.next(); // skip header line
				progress = new ProgressMonitor(0.5);
				progress.startTask(photoToBizCsv.toString(), numPhotos);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public void close() throws IOException {
			lines.close();
		}

		@Override
		protected Instance getNext() throws ws.palladian.helper.collection.AbstractIterator.Finished {
			if (iterator.hasNext()) {
				progress.increment();
				String[] split = iterator.next().split(",");
				int photoId = Integer.valueOf(split[0]);
				String businessIdString = split[1]; // int in train set; String in test set
				InstanceBuilder instanceBuilder = new InstanceBuilder();
				instanceBuilder.set("photoId", photoId);
				instanceBuilder.set("businessId", businessIdString);
				if (training) {
					int[] labels = businessIdToLabels.get(Integer.valueOf(businessIdString));
					if (labels == null) {
						throw new IllegalStateException("No entry for businessId " + businessIdString);
					}
					for (int idx[] = { 0 }; idx[0] < Label.values().length; idx[0]++) {
						String label = Label.getById(idx[0]).toString();
						boolean value = Arrays.stream(labels).anyMatch(v -> v == idx[0]);
						instanceBuilder.set(label, value);
					}
				}
				instanceBuilder.set("image", new ImageValue(new File(baseImagePath, photoId + ".jpg")));
				return instanceBuilder.create(false);
			}
			throw FINISHED;
		}
	}
	
	/**
	 * Get a subset of the dataset.
	 * 
	 * @param subset
	 *            The size of the subset.
	 * @param train
	 *            <code>true</code> for training split, <code>false</code> for
	 *            testing split.
	 * @return The subset.
	 */
	public Iterable<Instance> subset(Subset subset, boolean train) {
		Objects.requireNonNull(subset);
		return CollectionHelper.filter(this, new ModuloFilter(subset.mod, train ? 0 : 1));
	}
	
	@Override
	public FeatureInformation getFeatureInformation() {
		FeatureInformationBuilder builder = new FeatureInformationBuilder();
		builder.set("photoId", ImmutableIntegerValue.class);
		builder.set("businessId", ImmutableStringValue.class);
		return builder.create();
	}

	@Override
	public long size() {
		return numPhotos;
	}

	public static void main(String[] args) throws IOException {
		// (1) reading of training data
		File trainCsv = Config.getFilePath("dataset.yelp.restaurants.train.csv");
		File trainPhotoToBizCsv = Config.getFilePath("dataset.yelp.restaurants.train.photoToBizCsv");
		File baseTrainPath = Config.getFilePath("dataset.yelp.restaurants.train.photos");
		YelpKaggleDatasetReader trainSet = new YelpKaggleDatasetReader(trainPhotoToBizCsv, trainCsv, baseTrainPath);
		
		Iterable<Instance> trainingInstances = trainSet.subset(BusinessFilter.TRAIN);
		Iterable<Instance> validationInstances = trainSet.subset(BusinessFilter.VALIDATE);
		System.out.println("# training = " + CollectionHelper.count(trainingInstances.iterator()));
		System.out.println("# validation = " + CollectionHelper.count(validationInstances.iterator()));
		
//		for (Instance instance : trainSet) {
//			assert instance != null;
//		}
//		
//		// (2) reading of testing data
//		File testPhotoToBizCsv = Config.getFilePath("dataset.yelp.restaurants.test.photoToBizCsv");
//		File baseTestPath = Config.getFilePath("dataset.yelp.restaurants.test.photos");
//		YelpKaggleDatasetReader testSet = new YelpKaggleDatasetReader(testPhotoToBizCsv, baseTestPath);
//		for (Instance instance : testSet) {
//			assert instance != null;
//		}
	}

}
