package ws.palladian.kaggle.restaurants.clusterer;

public interface Cluster {
	int SIZE_UNKNOWN = -1;

	double[] center();

	/**
	 * @return The size of the cluster, or {@link #SIZE_UNKNOWN}, in case it is
	 *         not known.
	 */
	int size();
}
