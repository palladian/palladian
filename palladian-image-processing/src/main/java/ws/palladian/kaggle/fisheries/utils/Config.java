package ws.palladian.kaggle.fisheries.utils;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Config {

	/** The logger for this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

	private static final String FILE_NAME = "config.properties";
	
	/** A path where created files are written; e.g. extracted features, evaluation results, etc. */
	private static final String DATA_DIRECTORY = "ws.palladian.kaggle.fisheries.data";
	
//	/** The Apache Spark master. */
//	private static final String SPARK_MASTER = "spark.master";

	public static Configuration CONFIG;

	static {
		try {
			URL configurationLocation = ConfigurationUtils.locate(FILE_NAME);
			LOGGER.info("Configuration location = {}", configurationLocation);
			CONFIG = new PropertiesConfiguration(configurationLocation);
		} catch (ConfigurationException e) {
			throw new IllegalStateException();
		}
	}
	
	/**
	 * Get a path in the {@link #DATA_DIRECTORY}.
	 * 
	 * @param name
	 *            The name within the data directory, e.g.
	 *            <code>features.csv</code>.
	 * @return The full path within the {@link #DATA_DIRECTORY}.
	 */
	public static File getDataPath(String name) {
		Objects.requireNonNull(name);
		String dataDirectoryString = CONFIG.getString(DATA_DIRECTORY);
		if (StringUtils.isEmpty(dataDirectoryString)) {
			throw new IllegalStateException("Property " + DATA_DIRECTORY + " is not defined");
		}
		File dataDirectory = new File(dataDirectoryString);
		if (!dataDirectory.exists()) {
			LOGGER.info("{} does not exist and will be created", dataDirectory);
			if (!dataDirectory.mkdirs()) {
				throw new IllegalStateException(dataDirectory + " could not be created");
			}
		}
		if (!dataDirectory.isDirectory()) {
			throw new IllegalStateException(dataDirectory + " is not a directory");
		}
		return new File(dataDirectory, name);
	}
	
	/**
	 * Get a path which is defined in the configuration as property.
	 * 
	 * @param propertyName
	 *            The name of the property.
	 * @return The file path.
	 */
	public static File getFilePath(String propertyName) {
		Objects.requireNonNull(propertyName);
		String path = CONFIG.getString(propertyName);
		if (StringUtils.isEmpty(path)) {
			throw new IllegalArgumentException("Property " + propertyName + " is not defined.");
		}
		return new File(path);
	}
	
	public static File getRelativeFilePath(String propertyName) {
		return getDataPath(getFilePath(propertyName).getName());
	}

	/**
	 * Get a list of paths defined in the configuration as property.
	 * 
	 * @param propertyName
	 *            The name of the property.
	 * @return List of paths, or empty list.
	 */
	public static List<File> getFilePaths(String propertyName) {
		Objects.requireNonNull(propertyName);
		String[] paths = CONFIG.getStringArray(propertyName);
		return Arrays.stream(paths).map(File::new).collect(Collectors.toList());
	}

//	/**
//	 * Get the Spark configuration (specify the Spark master via
//	 * {@value #SPARK_MASTER} property).
//	 * 
//	 * @return The Spark configuration.
//	 */
//	public static SparkConf getSparkConf() {
//		String master = CONFIG.getString(SPARK_MASTER);
//		if (StringUtils.isEmpty(master)) {
//			throw new IllegalArgumentException("Property " + SPARK_MASTER + " is not defined.");
//		}
//		return new SparkConf().setAppName("Palladian Spark").setMaster(master);
//	}
	
	public static File getTrainingPath() {
		return getDataPath("train");
	}

	public static File getTestStg1Path() {
		return getDataPath("test_stg1");
	}
	
	/**
	 * @return The number of threads to use when running parallelized tasks.
	 */
	public static int getNumThreads() {
		return CONFIG.getInt("ws.palladian.numThreads", 1);
	}

	private Config() {
		// no instance
	}
	
}
