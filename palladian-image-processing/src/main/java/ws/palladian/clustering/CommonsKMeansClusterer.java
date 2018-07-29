package ws.palladian.clustering;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.stream.StreamSupport;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

public final class CommonsKMeansClusterer implements Clusterer {
	
	private final int numClusters;

	public CommonsKMeansClusterer(int numClusters) {
		this.numClusters = numClusters;
	}

	@Override
	public Collection<Cluster> cluster(Iterable<double[]> vectors) {
		List<Clusterable> clusterables = StreamSupport.stream(vectors.spliterator(), false)
						.map(v -> { return new DoublePoint(v); })
						.collect(toList());
		KMeansPlusPlusClusterer<Clusterable> clusterer = new KMeansPlusPlusClusterer<>(numClusters);
		List<CentroidCluster<Clusterable>> clusters = clusterer.cluster(clusterables);
		return clusters.stream()
				.map(c -> { return new ImmutableCluster(c.getCenter().getPoint()); })
				.collect(toList());
	}

}
