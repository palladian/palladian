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

import ws.palladian.classification.Instance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringPool;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.BasicFeatureVector;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Read for CSV files containing datasets in form of instances. The instances must be given in the CSV file in the
 * following format: <code>feature1;..;featureN;NominalClass</code>. Each line is one training instance.
 * <b>Important</b>: If you use the {@link #iterator()} and do not fully iterate over the dataset, you <b>must</b>
 * {@link CloseableIterator#close()} the iterator!
 * </p>
 * 
 * @author pk
 */
public class CsvDatasetReader implements Iterable<Trainable> {

    private static final class CsvDatasetIterator implements CloseableIterator<Trainable> {
        final String fieldSeparator;
        final boolean readHeader;
        String line;
        String[] headNames;
        int expectedColumns;
        BufferedReader reader;
        int lineNumber;
        boolean closed;
        /** Save some memory when reading datasets with nominal values. */
        final StringPool stringPool;

        public CsvDatasetIterator(File filePath, boolean readHeader, String fieldSeparator) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(filePath);
                reader = new BufferedReader(new InputStreamReader(inputStream));
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(filePath + " not found.");
            }
            this.fieldSeparator = fieldSeparator;
            this.readHeader = readHeader;
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
                    LOGGER.debug("Finished reading {} lines", readHeader ? lineNumber - 1 : lineNumber);
                    return false;
                }
                String[] parts = line.split(fieldSeparator);
                if (parts.length < 2) {
                    throw new IllegalStateException("Separator '" + fieldSeparator
                            + "'was not found, lines cannot be split ('" + line + "').");
                }
                if (lineNumber == 0) {
                    expectedColumns = parts.length;
                    if (readHeader) {
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
        public Trainable next() {
            if (closed) {
                throw new IllegalStateException("Already closed.");
            }
            if (line == null) {
                read();
            }
            String[] parts = line.split(fieldSeparator);
            line = null;
            FeatureVector featureVector = new BasicFeatureVector();
            for (int f = 0; f < parts.length - 1; f++) {
                String name = headNames == null ? String.valueOf(f) : headNames[f];
                String value = parts[f];
                // FIXME make better.
                if (value.equals("?")) {
                    // missing value, TODO maybe rethink what to do here and how
                    // to handle missing values in general.
                    continue;
                }
                try {
                    Double doubleValue = Double.valueOf(value);
                    featureVector.add(new NumericFeature(name, doubleValue));
                } catch (NumberFormatException e) {
                    String stringValue = stringPool.get(value);
                    featureVector.add(new NominalFeature(name, stringValue));
                }
            }
            String targetClass = stringPool.get(parts[parts.length - 1]);
            Trainable instance = new Instance(targetClass, featureVector);
            if (lineNumber % 100000 == 0) {
                LOGGER.debug("Read {} lines", lineNumber);
            }
            return instance;
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

    private final File filePath;
    private final boolean readHeader;
    private final String fieldSeparator;

    /**
     * <p>
     * Create a {@link CsvDatasetReader} for a CSV file with a header line and a default separator character
     * {@value ClassificationUtils#DEFAULT_SEPARATOR}.
     * </p>
     * 
     * @param filePath Path to the CSV file, not <code>null</code>.
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
     */
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
     */
    public CsvDatasetReader(File filePath, boolean readHeader, String fieldSeparator) {
        Validate.notNull(filePath, "filePath must not be null");
        if (!filePath.canRead()) {
            throw new IllegalArgumentException("Cannot find or read file \"" + filePath + "\"");
        }
        Validate.notEmpty(fieldSeparator, "fieldSeparator must not be empty");

        this.filePath = filePath;
        this.readHeader = readHeader;
        this.fieldSeparator = fieldSeparator;
    }

    @Override
    public CloseableIterator<Trainable> iterator() {
        return new CsvDatasetIterator(filePath, readHeader, fieldSeparator);
    }

    /**
     * <p>
     * Read all instances into a {@link List}. (in case the dataset is big, you should consider using an iterator
     * instead, see {@link #iterator()}).
     * </p>
     * 
     * @return List with instances from the dataset.
     */
    public List<Trainable> readAll() {
        CloseableIterator<Trainable> iterator = iterator();
        try {
            return CollectionHelper.newArrayList(iterator);
        } finally {
            FileHelper.close(iterator);
        }
    }

}
