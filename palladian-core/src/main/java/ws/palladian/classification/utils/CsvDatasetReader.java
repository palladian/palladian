package ws.palladian.classification.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.ImmutableInstance;
import ws.palladian.core.Instance;
import ws.palladian.core.dataset.AbstractDataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.featurevector.FlyweightVectorBuilder;
import ws.palladian.core.featurevector.FlyweightVectorSchema;
import ws.palladian.core.value.ImmutableBooleanValue;
import ws.palladian.core.value.ImmutableDoubleValue;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.core.value.io.ValueParser;
import ws.palladian.core.value.io.ValueParserException;
import ws.palladian.helper.StopWatch;
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
public class CsvDatasetReader extends AbstractDataset {

    private final class CsvDatasetIterator implements CloseableIterator<Instance> {
		String[] splitLine;
        BufferedReader reader;
        int lineNumber;
        boolean closed;
        final StopWatch stopWatch = new StopWatch();

        CsvDatasetIterator() {
            try {
            	InputStream inputStream = config.openInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(config.filePath() + " not found.");
            } catch (IOException e) {
            	throw new IllegalStateException("IOException for" + config.filePath());
			}
            this.closed = false;
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
                if (lineNumber == 0 && config.readHeader()) {
                	lineNumber++;
                	return hasNext();
                }
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
                if (expectedColumns != splitLine.length) {
                    throw new IllegalStateException("Unexpected number of entries in line " + lineNumber + "("
                            + splitLine.length + ", but should be " + expectedColumns + ")");
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
            if (splitLine == null) {
                read();
            }
            FlyweightVectorBuilder builder = vectorSchema.builder();
			for (int f = 0; f < splitLine.length - (config.readClassFromLastColumn() ? 1 : 0); f++) {
				String name = headNames[f];
				String value = splitLine[f];
				Value parsedValue;
				if (value.equals(config.nullValue())) {
					parsedValue = NullValue.NULL;
				} else {
					try {
						parsedValue = parsers[f].parse(value);
					} catch (ValueParserException e) {
						throw new IllegalStateException("Could not parse value \"" + value + "\" in column \"" + name
								+ "\", row " + lineNumber + " using " + parsers[f].getClass().getName() + ".", e);
					}
				}
				builder.set(name, parsedValue);
			}
            String targetClass = config.readClassFromLastColumn() ? stringPool.get(splitLine[splitLine.length - 1]) : "dummy";
            if (lineNumber % 1000 == 0) {
                LOGGER.debug("Read {} lines in {}", lineNumber, stopWatch);
            }
            splitLine = null;
            return new ImmutableInstance(builder.create(), targetClass);
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
    
	private static final ValueParser[] DEFAULT_PARSERS = new ValueParser[] { ImmutableBooleanValue.PARSER,
			ImmutableDoubleValue.PARSER, ImmutableStringValue.PARSER };

	private final CsvDatasetReaderConfig config;
	
    private final String[] headNames;
    
    private final int expectedColumns;

    /** Save some memory when reading datasets with nominal values. */
    private final StringPool stringPool = new StringPool();
    
    /** The parsers to use; they are auto-detected from the first line, in case not explicitly specified. */
    private final ValueParser[] parsers;
	
    private final FlyweightVectorSchema vectorSchema;
    
    /** The number of items in this dataset; cached once it is requested. */
    private long size = -1;

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
    	this(CsvDatasetReaderConfig.filePath(filePath).readHeader(readHeader).setFieldSeparator(fieldSeparator).createConfig());
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
		try (InputStream inputStream = config.openInputStream()) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String line = reader.readLine();
			for (;;) {
				if (line == null) {
					throw new IllegalStateException("No lines in file.");
				}
				if (line.isEmpty()) {
					continue;
				}
				String[] splitLine = line.split(config.fieldSeparator(), -1);
				expectedColumns = splitLine.length;
				int numValues = config.readClassFromLastColumn() ? splitLine.length - 1 : splitLine.length;
				if (config.readHeader()) {
					headNames = splitLine;
					vectorSchema = new FlyweightVectorSchema(Arrays.copyOf(headNames, numValues));
					// XXX consider case, that multiple blank lines might follow
					line = reader.readLine();
					splitLine = line.split(config.fieldSeparator(), -1);
				} else { // generate default header names
					headNames = new String[numValues];
					for (int c = 0; c < numValues; c++) {
						headNames[c] = String.valueOf(c);
					}
					vectorSchema = new FlyweightVectorSchema(headNames);
				}
				parsers = detectParsers(splitLine);
				break;
			}
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(config.filePath() + " not found.");
		} catch (IOException e) {
			throw new IllegalStateException("IOException for" + config.filePath());
		}
	}

    /**
     * <p>
     * Read all instances into a {@link List}. (in case the dataset is big, you should consider using an iterator
     * instead, see {@link #iterator()}).
     * </p>
     * 
     * @return List with instances from the dataset.
     * @deprecated Use {@link #buffer()} instead.
     */
    @Deprecated
    public List<Instance> readAll() {
        CloseableIterator<Instance> iterator = iterator();
        try {
            return CollectionHelper.newArrayList(iterator);
        } finally {
            FileHelper.close(iterator);
        }
    }
    
	/**
	 * Initialize appropriate parsers for the data; either by consider the
	 * parsers provided via configuration, or by trying to parse the value as
	 * different types (see {@link CsvDatasetReader#DEFAULT_PARSERS}).
	 * 
	 * @param parts
	 *            The split line.
	 * @return The parsers.
	 */
	private final ValueParser[] detectParsers(String[] parts) {
		int numValues = config.readClassFromLastColumn() ? parts.length - 1 : parts.length;
		ValueParser[] parsers = new ValueParser[numValues];
		for (int i = 0; i < numValues; i++) {
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
		return parsers;
	}
	
	// ws.palladian.core.dataset.Dataset
	
	@Override
    public CloseableIterator<Instance> iterator() {
        return new CsvDatasetIterator();
    }
	
	@Override
	public FeatureInformation getFeatureInformation() {
		FeatureInformationBuilder builder = new FeatureInformationBuilder();
		for (int i = 0; i < headNames.length; i++) {
			builder.set(headNames[i], parsers[i].getType());
		}
		return builder.create();
	}

	@Override
	public long size() {
		if (size == -1) {
			try (InputStream inputStream = config.openInputStream()) {
				int lineNumber = FileHelper.getNumberOfLines(inputStream);
				size = config.readHeader() ? lineNumber - 1 : lineNumber;
			} catch (IOException e) {
				throw new IllegalStateException("IOException for" + config.filePath());
			}
		}
		return size;
	}

}
