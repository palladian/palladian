/**
 * 
 */
package ws.palladian.classification;

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

}
