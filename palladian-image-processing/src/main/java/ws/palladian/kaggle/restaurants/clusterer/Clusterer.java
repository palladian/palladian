package ws.palladian.kaggle.restaurants.clusterer;

import java.util.Collection;

public interface Clusterer {
	Collection<Cluster> cluster(Iterable<double[]> vectors);
}
