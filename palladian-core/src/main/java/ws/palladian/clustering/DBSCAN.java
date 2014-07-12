package ws.palladian.clustering;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functions.Distance;

/**
 * <p>
 * Implementation of the DBSCAN clustering algorithm, as presented in
 * "A density-based algorithm for discovering clusters in large spatial databases with noise", Martin Ester, Hans-Peter
 * Kriegel, Jörg Sander, Xiaowei Xu, 1996.
 * 
 * @author pk
 * @param <T> Type of the objects to cluster.
 * @see <a href="http://en.wikipedia.org/wiki/DBSCAN">Wikipedia: DBSCAN</a>
 */
public class DBSCAN<T> {

    private final double eps;

    private final int minPts;

    private final Distance<? super T> distance;

    /**
     * <p>
     * Create a new DBSCAN clusterer.
     * 
     * @param eps Maximum distance for the epsilon neighborhood, greater zero.
     * @param minPts Minimum number of required points in the neighborhood to form a "dense region", greater zero.
     * @param distance The distance measure.
     */
    public DBSCAN(double eps, int minPts, Distance<? super T> distance) {
        Validate.isTrue(eps > 0, "eps must be greater zero");
        Validate.isTrue(minPts > 0, "minPts must be greater zero");
        Validate.notNull(distance, "distance must not be null");
        this.eps = eps;
        this.minPts = minPts;
        this.distance = distance;
    }

    /**
     * <p>
     * Clusters the given data items and returns a nested set representing the clusters.
     * 
     * @param data The data items to cluster, not <code>null</code>.
     * @return The clusters.
     */
    public Set<Set<T>> cluster(Iterable<? extends T> data) {
        Validate.notNull(data, "data must not be null");
        Set<Set<T>> clusters = CollectionHelper.newHashSet();
        Set<T> visited = CollectionHelper.newHashSet();
        Set<T> clustered = CollectionHelper.newHashSet();
        for (T d : data) {
            if (visited.contains(d)) {
                continue;
            }
            visited.add(d);
            Set<T> neighbors = regionQuery(d, data);
            if (neighbors.size() < minPts) {
                // modification to original algorithm; add noise points as singleton clusters
                clusters.add(Collections.singleton(d));
            } else {
                Set<T> currentCluster = expandCluster(d, neighbors, data, visited, clustered);
                clustered.addAll(currentCluster);
                clusters.add(currentCluster);
            }
        }
        return clusters;
    }

    private Set<T> expandCluster(T d, Set<T> neighbors, Iterable<? extends T> data, Set<T> visited, Set<T> clustered) {
        @SuppressWarnings("unchecked")
        Set<T> cluster = CollectionHelper.newHashSet(d);
        Queue<T> neighborQueue = new LinkedList<T>(neighbors);
        while (!neighborQueue.isEmpty()) {
            T n = neighborQueue.poll();
            if (!visited.contains(n)) {
                visited.add(n);
                Set<T> currentNeighbors = regionQuery(n, data);
                if (currentNeighbors.size() >= minPts) {
                    neighborQueue.addAll(currentNeighbors);
                }
            }
            if (!clustered.contains(n)) {
                cluster.add(n);
            }
        }
        return cluster;
    }

    private Set<T> regionQuery(T d, Iterable<? extends T> data) {
        Set<T> neighbors = CollectionHelper.newHashSet();
        for (T n : data) {
            if (distance.getDistance(d, n) < eps) {
                neighbors.add(n);
            }
        }
        return neighbors;
    }

    @Override
    public String toString() {
        return "DBSCAN [eps=" + eps + ", minPts=" + minPts + ", distance=" + distance + "]";
    }

}
