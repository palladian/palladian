/**
 * 
 */
package ws.palladian.classification;

import java.io.File;
import java.net.URL;
import java.util.List;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
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
    
    private static final String SEPARATOR = ";";

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
     * Create instances from a file. The instances must be given in a CSV file in the following format:
     * <code>feature1;..;featureN;NominalClass</code>. Each line is one training instance.
     * </p>
     * 
     * @param filePath The path to the CSV file to load either specified as path on the file system or as Java resource
     *            path.
     * @param readHeader <code>true</code> to treat the first line as column headers, <code>false</code> otherwise
     *            (column names are generated automatically).
     */
    public static List<NominalInstance> createInstances(String filePath, final boolean readHeader) {
        File csvFile = new File(filePath);
        if (!csvFile.exists()) {
            URL fileUrl = ClassificationUtils.class.getResource(filePath);
            csvFile = fileUrl == null ? csvFile : new File(fileUrl.getFile());
        }

        final List<NominalInstance> instances = CollectionHelper.newArrayList();

        FileHelper.performActionOnEveryLine(csvFile.getAbsolutePath(), new LineAction() {

            String[] headNames;

            @Override
            public void performAction(String line, int lineNumber) {
                String[] parts = line.split(SEPARATOR);

                if (readHeader && lineNumber == 0) {
                    headNames = parts;
                    return;
                }

                NominalInstance instance = new NominalInstance();
                instance.featureVector = new FeatureVector();

                for (int f = 0; f < parts.length - 1; f++) {
                    String name = headNames == null ? "col" + f : headNames[f];
                    String value = parts[f];
                    Double doubleValue;
                    // FIXME make better.
                    try {
                        doubleValue = Double.valueOf(value);
                        instance.featureVector.add(new NumericFeature(name, doubleValue));
                    } catch (NumberFormatException e) {
                        instance.featureVector.add(new NominalFeature(name, value));
                    }

                }

                instance.target = parts[parts.length - 1];
                instances.add(instance);
            }
        });

        return instances;
    }

}
