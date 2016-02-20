package ws.palladian.classification.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringPool;

/**
 * <p>
 * Read for CSV files containing datasets in form of instances. The instances must be given in the CSV file in the
 * following format: <code>feature1;..;featureN;NominalClass</code>. Each line is one training instance.
 * <b>Important</b>: If you use the {@link #iterator()} and do not fully iterate over the dataset, you <b>must</b>
 * {@link CloseableIterator#close()} the iterator!
 * </p>
 * 
 * @author Philipp Katz
 */
public class CsvDatasetReader implements Iterable<Instance> {

    private static final class CsvDatasetIterator implements CloseableIterator<Instance> {
    	private final CsvDatasetReaderConfig config;
        String line;
        String[] headNames;
        int expectedColumns;
        BufferedReader reader;
        int lineNumber;
        boolean closed;
        /** Save some memory when reading datasets with nominal values. */
        final StringPool stringPool;

        public CsvDatasetIterator(CsvDatasetReaderConfig config) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(config.filePath());
                reader = new BufferedReader(new InputStreamReader(inputStream));
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(config.filePath() + " not found.");
            }
            this.config = config;
            this.closed = false;
            this.stringPool = new StringPool();
        }

        @Override
        public boolean hasNext() {
            if (closed) {
                return false;
            }
            if (line != null) {
                return true;
            }
            return read();
        }

        private boolean read() {
            try {
                line = reader.readLine();
                if (line == null) {
                    close();
                    LOGGER.debug("Finished reading {} lines", config.readHeader() ? lineNumber - 1 : lineNumber);
                    return false;
                }
                String[] parts = line.split(config.fieldSeparator());
                if (parts.length < 2) {
                    throw new IllegalStateException("Separator '" + config.fieldSeparator()
                            + "' was not found, lines cannot be split ('" + line + "').");
                }
                if (lineNumber == 0) {
                    expectedColumns = parts.length;
                    if (config.readHeader()) {
                        headNames = parts;
                        lineNumber++;
                        line = null;
                        return hasNext();
                    }
                } else {
                    if (expectedColumns != parts.length) {
                        throw new IllegalStateException("Unexpected number of entries in line " + lineNumber + "("
                                + parts.length + ", but should be " + expectedColumns + ")");
                    }
                }
                lineNumber++;
                return true;
            } catch (IOException e) {
                throw new IllegalStateException("I/O exception while trying to read from file", e);
            }
        }

        @Override
        public Instance next() {
            if (closed) {
                throw new IllegalStateException("Already closed.");
            }
            if (line == null) {
                read();
            }
            String[] parts = line.split(config.fieldSeparator());
            line = null;
            InstanceBuilder builder = new InstanceBuilder();
            for (int f = 0; f < parts.length - (config.readClassFromLastColumn() ? 1 : 0); f++) {
                String name = headNames == null ? String.valueOf(f) : headNames[f];
                String value = parts[f];
                if (value.equals("?")) {
                    builder.setNull(name);
                    continue;
                }
                try { // XXX make better.
                	if (value.contains(".")) {
                		double doubleValue = Double.parseDouble(value);
                		builder.set(name, doubleValue);
                	} else if (value.equals("NaN")) {
						// XXX hotfix, where NaN was parsed as string; better
						// would be to detect an implicit data schema before
						// parsing
                		builder.set(name, Double.NaN);
					} else if (value.equals("Infinity")) {
						builder.set(name, Double.POSITIVE_INFINITY);
					} else if (value.equals("-Infinity")) {
						builder.set(name, Double.NEGATIVE_INFINITY);
					} else {
						long longValue = Long.parseLong(value);
						builder.set(name, longValue);
					}
                } catch (NumberFormatException e) {
                    String stringValue = stringPool.get(value);
                    builder.set(name, stringValue);
                }
            }
            String targetClass = config.readClassFromLastColumn() ? stringPool.get(parts[parts.length - 1]) : "dummy";
            if (lineNumber % 100000 == 0) {
                LOGGER.debug("Read {} lines", lineNumber);
            }
            return builder.create(targetClass);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Modifications are not supported.");
        }

        @Override
        public void close() throws IOException {
            FileHelper.close(reader);
            closed = true;
        }
    }

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvDatasetReader.class);

	private final CsvDatasetReaderConfig config;

    /**
     * <p>
     * Create a {@link CsvDatasetReader} for a CSV file with a header line and a default separator character
     * {@value ClassificationUtils#DEFAULT_SEPARATOR}.
     * </p>
     * 
     * @param filePath Path to the CSV file, not <code>null</code>.
     * @deprecated Use {@link #CsvDatasetReader(CsvDatasetReaderConfig)} instead.
     */
    public CsvDatasetReader(File filePath) {
        this(filePath, true);
    }

    /**
     * <p>
     * Create a {@link CsvDatasetReader} for a CSV file with a default separator character
     * {@value ClassificationUtils#DEFAULT_SEPARATOR}.
     * </p>
     * 
     * @param filePath Path to the CSV file, not <code>null</code>.
     * @param readHeader <code>true</code> to read the feature's names from a header line at the beginning,
     *            <code>false</code> otherwise.
     * @deprecated Use {@link #CsvDatasetReader(CsvDatasetReaderConfig)} instead.            
     */
    @Deprecated
    public CsvDatasetReader(File filePath, boolean readHeader) {
        this(filePath, readHeader, ClassificationUtils.DEFAULT_SEPARATOR);
    }

    /**
     * <p>
     * Create a {@link CsvDatasetReader} for a CSV file.
     * </p>
     * 
     * @param filePath Path to the CSV file, not <code>null</code>.
     * @param readHeader <code>true</code> to read the feature's names from a header line at the beginning,
     *            <code>false</code> otherwise.
     * @param fieldSeparator Separator between entries, not <code>null</code>.
     * @deprecated Use {@link #CsvDatasetReader(CsvDatasetReaderConfig)} instead.            
     */
    @Deprecated
    public CsvDatasetReader(File filePath, boolean readHeader, String fieldSeparator) {
    	CsvDatasetReaderConfig.Builder configBuilder = CsvDatasetReaderConfig.filePath(filePath);
    	configBuilder.readHeader(readHeader);
    	configBuilder.fieldSeparator(fieldSeparator);
    	config = configBuilder.create();
    }
    
    /**
     * <p>
     * Create a {@link CsvDatasetReader} for a CSV file.
     * </p>
     * 
     * @param config The config, not <code>null</code>.
     */
    public CsvDatasetReader(CsvDatasetReaderConfig config) {
    	Validate.notNull(config, "config must not be null");
    	this.config = config;
	}

    @Override
    public CloseableIterator<Instance> iterator() {
        return new CsvDatasetIterator(config);
    }

    /**
     * <p>
     * Read all instances into a {@link List}. (in case the dataset is big, you should consider using an iterator
     * instead, see {@link #iterator()}).
     * </p>
     * 
     * @return List with instances from the dataset.
     */
    public List<Instance> readAll() {
        CloseableIterator<Instance> iterator = iterator();
        try {
            return CollectionHelper.newArrayList(iterator);
        } finally {
            FileHelper.close(iterator);
        }
    }

}
