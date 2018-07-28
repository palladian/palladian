//package ws.palladian.kaggle.restaurants.clusterer;
//
//import static java.nio.charset.StandardCharsets.UTF_8;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.OutputStreamWriter;
//import java.io.Writer;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.Objects;
//import java.util.UUID;
//
//import org.apache.commons.lang3.StringUtils;
//import org.apache.spark.api.java.JavaRDD;
//import org.apache.spark.api.java.JavaSparkContext;
//import org.apache.spark.api.java.function.Function;
//import org.apache.spark.mllib.clustering.KMeans;
//import org.apache.spark.mllib.clustering.KMeansModel;
//import org.apache.spark.mllib.linalg.Vector;
//import org.apache.spark.mllib.linalg.Vectors;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * k-means clustering via <a href="http://spark.apache.org">Apache Spark</a>.
// * Open <a href="http://localhost:4040">http://localhost:4040</a> for progress
// * monitoring.
// * 
// * @author pk
// */
//public final class SparkKMeansClusterer implements Clusterer {
//	
//    public static final class CsvLineParser implements Function<String, Vector> {
//		private static final long serialVersionUID = 1L;
//
//		public Vector call(String s) {
//			return Vectors.dense(
//					Arrays.stream(s.split(CSV_SEPARATOR_STRING))
//						.mapToDouble(Double::valueOf)
//						.toArray());
//		}
//	}
//
//	/** The logger for this class. */
//    private static final Logger LOGGER = LoggerFactory.getLogger(SparkKMeansClusterer.class);
//	
//	private static final char CSV_SEPARATOR = ';';
//	
//	private static final String CSV_SEPARATOR_STRING = String.valueOf(CSV_SEPARATOR);
//	
//	private final JavaSparkContext sparkContext;
//	private final int numClusters;
//	private final int numIterations;
//
//	/**
//	 * Create a new Spark-based k-means clusterer.
//	 * 
//	 * @param sparkContext
//	 *            The Spark context; define this once for the lifetime of the
//	 *            application.
//	 * @param numClusters
//	 *            The number of clusters to create (i.e. k)
//	 * @param numIterations
//	 *            The number of iterations to perform.
//	 */
//	public SparkKMeansClusterer(JavaSparkContext sparkContext, int numClusters, int numIterations) {
//		this.sparkContext = Objects.requireNonNull(sparkContext);
//		this.numClusters = numClusters;
//		this.numIterations = numIterations;
//	}
//
//	@Override
//	public Collection<Cluster> cluster(Iterable<double[]> vectors) {
//		File dataFile = writeVectorsToFile(vectors);
//
//		JavaRDD<String> data = sparkContext.textFile(dataFile.getAbsolutePath());
//
//		JavaRDD<Vector> parsedData = data.map(new CsvLineParser());
//		
//		parsedData.cache();
//		KMeansModel clusters = KMeans.train(parsedData.rdd(), numClusters, numIterations);
//		Collection<Cluster> result = new ArrayList<>();
//		for (Vector vector : clusters.clusterCenters()) {
//			result.add(new ImmutableCluster(vector.toArray()));
//			// FileHelper.appendFile("/Users/pk/Desktop/SURF-clusters.csv", StringUtils.join(vector.toArray(), ',')+"\n");
//			// System.out.println(vector.toString());
//		}
//		return result;
//	}
//
//	/**
//	 * Writes all data to a CSV file, as the Spark input must be a file.
//	 * 
//	 * @param vectors
//	 *            The vectors to write.
//	 * @return The file which contains the written CSV data; this will be marked
//	 *         as temporary and be deleted upon VM termination.
//	 */
//	private static File writeVectorsToFile(Iterable<double[]> vectors) {
//		try {
//			Path tempDirectory = Files.createTempDirectory(SparkKMeansClusterer.class.getName());
//			tempDirectory.toFile().deleteOnExit();
//			File outputFile = new File(tempDirectory.toFile(), UUID.randomUUID().toString());
//			try (Writer writer = new BufferedWriter(
//					new OutputStreamWriter(new FileOutputStream(outputFile, true), UTF_8))) {
//				int count = 0;
//				for (double[] vector : vectors) {
//					writer.append(StringUtils.join(vector, CSV_SEPARATOR));
//					writer.append('\n');
//					count++;
//					if (count % 10000 == 0) {
//						LOGGER.debug("Wrote {} instances to {}", count, outputFile);
//					}
//				}
//				LOGGER.info("Wrote {} instances to {}", count, outputFile);
//			}
//			return outputFile;
//		} catch (IOException e) {
//			throw new IllegalStateException(e);
//		}
//	}
//
//}
