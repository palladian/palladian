package ws.palladian.classification.utils;

import static ws.palladian.helper.io.DelimitedStringHelper.splitLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
		List<String> splitLine;
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
            closed = false;
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
                if (lineNumber == config.getLimit() + 1) {
                	LOGGER.debug("Limit of {} reached, stopping", config.getLimit());
                	return false;
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
                splitLine = splitLine(line, config.fieldSeparator(), config.quoteCharacter());
                if (splitLine.size() < 2) {
                    throw new IllegalStateException("Separator '" + config.fieldSeparator()
                            + "' was not found, lines cannot be split ('" + line + "').");
                }
                if (expectedColumns != splitLine.size()) {
                    throw new IllegalStateException("Unexpected number of entries in line " + lineNumber + " ("
                            + splitLine.size() + ", but should be " + expectedColumns + ")");
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
			for (int f = 0; f < splitLine.size() - (config.readClassFromLastColumn() ? 1 : 0); f++) {
				String name = headNames[f];
				if (name == null) {
					continue;
				}
				String value = splitLine.get(f);
				if (config.isTrim()) {
					value = value.trim();
				}
				Value parsedValue;
				if (config.isNullValue(value)) {
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
            String targetClass;
            if (config.readClassFromLastColumn()) {
            	String value = splitLine.get(splitLine.size() - 1);
            	if (config.isTrim()) {
            		value = value.trim();
            	}
				targetClass = stringPool.get(value);
            } else {
            	targetClass = NO_CATEGORY_DUMMY;
            }
            if (lineNumber % LOG_EVERY_N_LINES == 0) {
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
	
	/** Interval for the debug logging output when reading lines. */
	private static final int LOG_EVERY_N_LINES = 100000;
	
	/** Dummy string to use for {@link Instance#getCategory()} when not reading category columns. */
	private static final String NO_CATEGORY_DUMMY = "";

	private final CsvDatasetReaderConfig config;
	
	/** Names of the headers, in case a value is <code>null</code> it will be skipped during parsing. */
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
    @Deprecated
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
    	this(CsvDatasetReaderConfig.filePath(filePath).readHeader(readHeader).fieldSeparator(fieldSeparator).createConfig());
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
				List<String> splitLine = splitLine(line, config.fieldSeparator(), config.quoteCharacter());
				expectedColumns = splitLine.size();
				int numValues = config.readClassFromLastColumn() ? splitLine.size() - 1 : splitLine.size();
				headNames = new String[numValues];
				if (config.readHeader()) {
					List<String> usedHeadNames = new ArrayList<>();
					for (int c = 0; c < numValues; c++) {
						String columnName = splitLine.get(c);
						if (config.isSkippedColumn(columnName)) {
							LOGGER.debug("Skipping column {}", columnName);
							continue;
						}
						if (config.isTrim()) {
							columnName = columnName.trim();
						}
						headNames[c] = columnName;
						usedHeadNames.add(columnName);
					}
					vectorSchema = new FlyweightVectorSchema(usedHeadNames.toArray(new String[0]));
					// XXX consider case, that multiple blank lines might follow
					line = reader.readLine();
					splitLine = splitLine(line, config.fieldSeparator(), config.quoteCharacter());
				} else { // generate default header names
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
	private final ValueParser[] detectParsers(List<String> parts) {
		int numValues = config.readClassFromLastColumn() ? parts.size() - 1 : parts.size();
		ValueParser[] parsers = new ValueParser[numValues];
		for (int i = 0; i < numValues; i++) {
			// (1) try to use parser defined via configuration
			String columnName = headNames[i];
			if (columnName == null) {
				continue;
			}
			ValueParser parser = config.getParser(columnName);
			// (2) if not, auto-detect applicable parser
			if (parser == null) {
				String input = parts.get(i);
				for (ValueParser currentParser : config.getDefaultParsers()) {
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
			String columnName = headNames[i];
			if (columnName == null) {
				continue;
			}
			builder.set(columnName, parsers[i].getType());
		}
		return builder.create();
	}

	@Override
	public long size() {
		if (size == -1) {
			try (InputStream inputStream = config.openInputStream()) {
				int lineNumber = FileHelper.getNumberOfLines(inputStream);
				size = Math.min(config.readHeader() ? lineNumber - 1 : lineNumber, config.getLimit());
			} catch (IOException e) {
				throw new IllegalStateException("IOException for" + config.filePath());
			}
		}
		return size;
	}

}
