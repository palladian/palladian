package ws.palladian.clustering;

import java.util.Collection;

public interface Clusterer {
    Collection<Cluster> cluster(Iterable<double[]> vectors);
}
