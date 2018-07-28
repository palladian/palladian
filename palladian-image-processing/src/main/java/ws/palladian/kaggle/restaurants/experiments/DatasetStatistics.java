//package ws.palladian.kaggle.restaurants.experiments;
//
//import static ws.palladian.helper.collection.CollectionHelper.convert;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.security.DigestInputStream;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.util.LinkedHashMap;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Map.Entry;
//
//import ws.palladian.classification.utils.ClassificationUtils;
//import ws.palladian.classification.zeror.ZeroRLearner;
//import ws.palladian.classification.zeror.ZeroRModel;
//import ws.palladian.core.Instance;
//import ws.palladian.core.dataset.Dataset;
//import ws.palladian.core.dataset.DatasetWithFeatureAsCategory;
//import ws.palladian.core.value.Value;
//import ws.palladian.helper.collection.Bag;
//import ws.palladian.helper.collection.CollectionHelper;
//import ws.palladian.helper.collection.CollectionHelper.Order;
//import ws.palladian.helper.collection.DefaultMultiMap;
//import ws.palladian.helper.collection.MultiMap;
//import ws.palladian.helper.math.FatStats;
//import ws.palladian.kaggle.restaurants.Extractor;
//import ws.palladian.kaggle.restaurants.dataset.Label;
//import ws.palladian.kaggle.restaurants.dataset.YelpKaggleDatasetReader;
//import ws.palladian.kaggle.restaurants.utils.Config;
//
//@SuppressWarnings("unused")
//public class DatasetStatistics {
//
//	public static void main(String[] args) throws IOException {
//		File trainCsv = Config.getFilePath("dataset.yelp.restaurants.train.csv");
//		File photoToBizCsv = Config.getFilePath("dataset.yelp.restaurants.train.photoToBizCsv");
//		File baseImagePath = Config.getFilePath("dataset.yelp.restaurants.train.photos");
//		Iterable<Instance> dataset = new YelpKaggleDatasetReader(photoToBizCsv, trainCsv, baseImagePath);
//		// getPriors(dataset);
//		// getBusinessPhotoStatistics(dataset);
//		getDuplicateStatistics(dataset);
//
//		// Iterable<Instance> trainingSet = filter(dataset, new
//		// ModuloFilter(Subset.MEDIUM.mod, 0));
//		// Iterable<Instance> testingSet = filter(dataset, new
//		// ModuloFilter(Subset.MEDIUM.mod, 1));
//
//		// getPriors(trainingSet);
//		// getPriors(testingSet);
//	}
//
//	/**
//	 * Get statistics about relation between business and photo.
//	 * 
//	 * @param dataset
//	 *            The dataset.
//	 */
//	public static void getBusinessPhotoStatistics(Iterable<Instance> dataset) {
//		Bag<Value> businessPhotoCounts = Bag.create();
//		dataset.forEach(i -> businessPhotoCounts.add(i.getVector().get("businessId")));
//		for (Entry<Value, Integer> entry : businessPhotoCounts.createSorted(Order.DESCENDING).unique()) {
//			System.out.println(entry.getKey() + "\t" + entry.getValue());
//		}
//		System.out.println(new FatStats(businessPhotoCounts.toMap().values()));
//	}
//
//	/**
//	 * Get the priors for the individual labels.
//	 * 
//	 * @param dataset
//	 *            The dataset.
//	 */
//	public static void getPriors(Dataset dataset) {
//		Map<String, Double> categoryPrior = new LinkedHashMap<>();
//		for (Label label : Label.values()) {
//			Iterable<Instance> categoryDataset = new DatasetWithFeatureAsCategory(dataset, label.toString());
//			ZeroRModel zeroRModel = new ZeroRLearner().train(categoryDataset);
//			Double trueProbability = zeroRModel.getCategoryProbabilities().get("true");
//			categoryPrior.put(label.toString(), trueProbability);
//		}
//		CollectionHelper.print(categoryPrior);
//	}
//
//	/**
//	 * Give statistics about (exact) duplicate images in the dataset.
//	 * 
//	 * @param dataset
//	 *            The dataset.
//	 * @throws IOException
//	 *             In case an image cannot be loaded.
//	 */
//	public static void getDuplicateStatistics(Iterable<Instance> dataset) throws IOException {
//		Bag<String> hashes = Bag.create();
//		MultiMap<String, String> hashToFilename = DefaultMultiMap.createWithSet();
//		Iterable<File> images = convert(dataset, Extractor.getImageFile());
//		for (File image : images) {
//			String hash = md5(image);
//			hashes.add(hash);
//			hashToFilename.add(hash, image.getName());
//		}
//		for (Entry<String, Integer> entry : hashes.createSorted(Order.DESCENDING).unique()) {
//			Integer count = entry.getValue();
//			if (count > 1) {
//				String hash = entry.getKey();
//				System.out.println(count + "\t" + hash + "\t" + hashToFilename.get(hash));
//			}
//		}
//		System.out.println(new FatStats(hashes.toMap().values()));
//	}
//
//	public static final String md5(File file) throws IOException {
//		Objects.requireNonNull(file, "file must not be null");
//		MessageDigest digestAlgorithm;
//		try {
//			digestAlgorithm = MessageDigest.getInstance("MD5");
//		} catch (NoSuchAlgorithmException e) {
//			throw new IllegalStateException(e);
//		}
//		try (
//				InputStream inputStream = new FileInputStream(file); 
//				DigestInputStream digestInputStream = new DigestInputStream(inputStream, digestAlgorithm)) {
//			while (digestInputStream.read() != -1) {
//				// read the whole file
//			}
//			digestInputStream.close();
//		}
//		byte[] digest = digestAlgorithm.digest();
//		StringBuilder hexString = new StringBuilder();
//		for (byte b : digest) {
//			hexString.append(String.format("%02x", b & 0xff));
//		}
//		return hexString.toString();
//	}
//
//}
