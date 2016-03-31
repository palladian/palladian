package ws.palladian.core.dataset;

import java.util.Set;

import ws.palladian.core.Instance;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.io.CloseableIterator;

/**
 * Defines a dataset comprised of multiple Instances. Typically used as input
 * for training classifiers, performing evaluation, etc.
 * 
 * @author pk
 */
public interface Dataset extends Iterable<Instance> {
	
	/**
	 * Overridden to return a {@link CloseableIterator}, in order to indicate
	 * that the iterator usually needs to be closed.
	 */
	@Override
	CloseableIterator<Instance> iterator();

	/**
	 * @return The names of the features in this dataset.
	 * @deprecated Use {@link #getFeatureInformation()} instead.
	 */
	@Deprecated
	Set<String> getFeatureNames();
	
	/**
	 * @return Information about the features in this dataset.
	 */
	FeatureInformation getFeatureInformation();

	/**
	 * Get an estimate of the dataset's size (i.e. the number of rows). The
	 * estimate may be an upper bound, i.e. the dataset has at most the number
	 * of returned items (this is due to the reason, that determining the exact
	 * number of instances may be costly and involve explicit parsing, e.g. of a
	 * CSV file). In case, one needs the exact amount of instances, use the
	 * {@link #iterator()} and count.
	 * 
	 * @return An estimate of the dataset's size, or a value of -1 in case the
	 *         dataset's size cannot be determined.
	 */
	long size();

	/**
	 * Filter the features. The returned dataset will only contain the features
	 * which passed the provided filter.
	 * 
	 * @param nameFilter The filter to apply.
	 * @return The filtered dataset.
	 */
	Dataset filterFeatures(Filter<? super String> nameFilter);
	
	/**
	 * Get a subset of the dataset.
	 * 
	 * @param instanceFilter
	 *            The filter which defines the subset.
	 * @return The subset with instances matching the filter.
	 */
	Dataset subset(Filter<? super Instance> instanceFilter);
	
	/**
	 * Get a subset of the dataset.
	 * 
	 * @param instanceFilterFactory
	 *            The factory with the filter which defines the subset.
	 * @return The subset with instances matching the filter.
	 */
	Dataset subset(Factory<? extends Filter<? super Instance>> instanceFilterFactory);
	
	/**
	 * Read the whole dataset into memory. Only do that for small datasets and
	 * for performance reasons. Else wise prefer using the {@link #iterator()}.
	 * 
	 * @return The in-memory dataset.
	 */
	Dataset buffer();
	
}
