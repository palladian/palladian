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
import ws.palladian.core.value.ImmutableDoubleValue;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.core.value.io.ValueParser;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringPool;

/**
 * <p>
 * Read for CSV files containing datasets in form of instances. The instances
 * must be given in the CSV file in the following format:
 * <code>feature1;..;featureN;NominalClass</code>. Each line is one training
 * instance. <b>Important</b>: If you use the {@link #iterator()} and do not
 * fully iterate over the dataset, you <b>must</b>
 * {@link CloseableIterator#close()} the iterator!
 * </p>
 * 
 * <p>
 * Per default, the reader detects the data types in the CSV file automatically,
 * by looking at the first data line in the input CSV file. It will either parse
 * values as double values, or as strings. In case you need to customize, how
 * the data types should be parsed, use the
 * {@link CsvDatasetReaderConfig.Builder#parser(String, ValueParser)} method.
 * </p>
 * 
 * @author Philipp Katz
 */
public class CsvDatasetReader implements Iterable<Instance> {

    private static final class CsvDatasetIterator implements CloseableIterator<Instance> {
		private final CsvDatasetReaderConfig config;
		String[] splitLine;
        String[] headNames;
        int expectedColumns;
        BufferedReader reader;
        int lineNumber;
        boolean closed;
        /** Save some memory when reading datasets with nominal values. */
        final StringPool stringPool;
        /** The parsers to use; they are auto-detected from the first line, in case not explicitly specified. */
        ValueParser[] parsers;

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
            if (splitLine != null) {
                return true;
            }
            return read();
        }

        private boolean read() {
            try {
                final String line = reader.readLine();
                if (line == null) {
                	splitLine = null;
                    close();
                    LOGGER.debug("Finished reading {} lines", config.readHeader() ? lineNumber - 1 : lineNumber);
                    return false;
                }
                if (line.isEmpty()) { // skip empty lines
                	lineNumber++;
                	splitLine = null;
                	return hasNext();
                }
                splitLine = line.split(config.fieldSeparator(), -1);
                if (splitLine.length < 2) {
                    throw new IllegalStateException("Separator '" + config.fieldSeparator()
                            + "' was not found, lines cannot be split ('" + line + "').");
                }
                if (lineNumber == 0) {
                    expectedColumns = splitLine.length;
                    if (config.readHeader()) {
                        headNames = splitLine;
                        lineNumber++;
                        splitLine = null;
                        return hasNext();
                    } else { // generate default header names
                    	headNames = new String[expectedColumns];
                    	for (int c = 0; c < expectedColumns; c++) {
                    		headNames[c] = String.valueOf(c);
                    	}
                    }
                } else {
                    if (expectedColumns != splitLine.length) {
                        throw new IllegalStateException("Unexpected number of entries in line " + lineNumber + "("
                                + splitLine.length + ", but should be " + expectedColumns + ")");
                    }
                }
                if (parsers == null) {
                	detectParsers(splitLine);
                }
                lineNumber++;
                return true;
            } catch (IOException e) {
                throw new IllegalStateException("I/O exception while trying to read from file", e);
            }
        }

		/**
		 * Initialize appropriate parsers for the data; either by consider the
		 * parsers provided via configuration, or by trying to parse the value
		 * as different types (see {@link CsvDatasetReader#DEFAULT_PARSERS}).
		 * 
		 * @param parts
		 *            The split line.
		 */
		private void detectParsers(String[] parts) {
			parsers = new ValueParser[parts.length];
			for (int i = 0; i < parts.length; i++) {
				// (1) try to use parser defined via configuration
				ValueParser parser = config.getParser(headNames[i]);
				// (2) if not, auto-detect applicable parser
				if (parser == null) {
					String input = parts[i];
					for (ValueParser currentParser : DEFAULT_PARSERS) {
						if (currentParser.canParse(input)) {
							parser = currentParser;
							break;
						}
					}
				}
				LOGGER.debug("Parser for {}: {}", headNames[i], parser.getClass().getName());
				parsers[i] = parser;
			}
		}

		@Override
        public Instance next() {
            if (closed) {
                throw new IllegalStateException("Already closed.");
            }
            if (splitLine == null) {
                read();
            }
            InstanceBuilder builder = new InstanceBuilder();
			for (int f = 0; f < splitLine.length - (config.readClassFromLastColumn() ? 1 : 0); f++) {
				String name = headNames[f];
				String value = splitLine[f];
				Value parsedValue;
				if (value.equals(config.nullValue())) {
					parsedValue = NullValue.NULL;
				} else {
					parsedValue = parsers[f].parse(value);
				}
				builder.set(name, parsedValue);
			}
            String targetClass = config.readClassFromLastColumn() ? stringPool.get(splitLine[splitLine.length - 1]) : "dummy";
            if (lineNumber % 100000 == 0) {
                LOGGER.debug("Read {} lines", lineNumber);
            }
            splitLine = null;
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
    
	private static final ValueParser[] DEFAULT_PARSERS = new ValueParser[] { ImmutableDoubleValue.PARSER,
			ImmutableStringValue.PARSER };

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
    	config = configBuilder.createConfig();
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
