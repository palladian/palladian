/**
 * 
 */
package ws.palladian.classification;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * A utility class providing convenience methods for working with classifiers
 * and their results.
 * </p>
 * 
 * @author Klemens Muthmann
 * 
 */
public final class ClassificationUtils {

	/**
	 * <p>
	 * Should not be instantiated.
	 * </p>
	 */
	private ClassificationUtils() {
		throw new UnsupportedOperationException(
				"Unable to instantiate ClassificationUtils. This class is a utility class. It makes no sense to instantiate it.");
	}

	public static CategoryEntry getSingleBestCategoryEntry(
			CategoryEntries entries) {
		CategoryEntries limitedCategories = limitCategories(entries, 1, 0.0);
		if (!limitedCategories.isEmpty()) {
			return limitedCategories.get(0);
		} else {
			return null;
		}
	}

	/**
	 * <p>
	 * Creates a new CategoryEntries object by limiting an existing one to a
	 * number of different categories, which need to have a relevance score
	 * above a provided threshold.
	 * </p>
	 * 
	 * @param number
	 *            Number of categories to keep.
	 * @param relevanceThreshold
	 *            Categories must have at least this much relevance to be kept.
	 */
	public static CategoryEntries limitCategories(CategoryEntries categories,
			int number, double relevanceThreshold) {
		CategoryEntries limitedCategories = new CategoryEntries();
		categories.sortByRelevance();
		int n = 0;
		for (CategoryEntry c : categories) {
			if (n < number && c.getRelevance() >= relevanceThreshold) {
				// XXX added by Philipp, lower memory consumption.
				c.setCategoryEntries(limitedCategories);
				limitedCategories.add(c);
			}
			n++;
		}
		return limitedCategories;
	}

	/**
	 * <p>
	 * Create instances from a file. The instances must be given in a CSV file
	 * in the following format:<br>
	 * feature1;..;featureN;NominalClass
	 * </p>
	 * <p>
	 * All features must be real values and the class must be nominal. Each line
	 * is one training instance.
	 * </p>
	 * 
	 * @param The
	 *            path to the CSV file to load either specified as path on the
	 *            file system or as Java resource path.
	 */
	public static List<NominalInstance> createInstances(String filePath) {
		List<NominalInstance> instances = new LinkedList<NominalInstance>();
		File csvFile = new File(filePath);
		if(!csvFile.exists()) {
			URL fileUrl = ClassificationUtils.class.getResource(filePath);
			csvFile = fileUrl==null ? csvFile : new File(fileUrl.getFile());
		}
		List<String> trainingLines = FileHelper.readFileToArray(csvFile);

		NominalInstance instance = null;

		for (String trainingLine : trainingLines) {
			String[] parts = trainingLine.split(";");

			instance = new NominalInstance();// (instances);
			instance.featureVector = new FeatureVector();

			for (int f = 0; f < parts.length - 1; f++) {
				instance.featureVector.add(new NumericFeature(
						String.valueOf(f), Double.valueOf(parts[f])));
			}

			instance.target = parts[parts.length - 1];
			instances.add(instance);
		}

		return instances;
	}

}
