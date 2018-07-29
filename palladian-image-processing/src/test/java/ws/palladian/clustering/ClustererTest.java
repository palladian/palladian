package ws.palladian.clustering;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Iterator;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.junit.Test;

import ws.palladian.clustering.Cluster;
import ws.palladian.clustering.Clusterer;
import ws.palladian.clustering.CommonsKMeansClusterer;
import ws.palladian.clustering.WekaKMeansClusterer;
import ws.palladian.helper.collection.AbstractIterator;

public class ClustererTest {
	private static final int NUM_CLUSTERS = 10;

//	@Test
//	public void testSparkKMeansClusterer() {
//		SparkConf sparkConfig = new SparkConf().setAppName("test").setMaster("local[1]");
//		try (JavaSparkContext sparkContext = new JavaSparkContext(sparkConfig)) {
//			test(new SparkKMeansClusterer(sparkContext, NUM_CLUSTERS, 10));
//		}
//	}
	@Test
	public void testCommonsKMeansClusterer() {
		test(new CommonsKMeansClusterer(NUM_CLUSTERS));
	}
	@Test
	public void testWekaKMeansClusterer() {
		test(new WekaKMeansClusterer(NUM_CLUSTERS));
	}
	
	private static void test(Clusterer clusterer) {
		Collection<Cluster> clusters = clusterer.cluster(new RandomVectorGenerator(1000, 100));
		assertEquals(NUM_CLUSTERS, clusters.size());
	}

	private static final class RandomVectorGenerator implements Iterable<double[]> {
		private final int num;
		private final int dimension;
		private int count = 0;

		public RandomVectorGenerator(int num, int dimension) {
			this.num = num;
			this.dimension = dimension;
		}

		@Override
		public Iterator<double[]> iterator() {
			return new AbstractIterator<double[]>() {
				@Override
				protected double[] getNext() throws ws.palladian.helper.collection.AbstractIterator.Finished {
					if (count++ >= num) {
						throw FINISHED;
					}
					double[] vector = new double[dimension];
					for (int i = 0; i < vector.length; i++) {
						vector[i] = Math.random();
					}
					return vector;
				}
			};
		}
	}
}
