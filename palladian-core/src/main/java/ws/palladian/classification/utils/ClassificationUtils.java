package ws.palladian.classification.utils;

import java.io.*;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.*;
import ws.palladian.core.dataset.csv.CsvDatasetWriter;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * A utility class providing convenience methods for working with classifiers and their results.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class ClassificationUtils {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassificationUtils.class);

    /** The default separator which is assumed for separating instance attributes when reading/writing to/from files. */
    public static final String DEFAULT_SEPARATOR = ";";

    private ClassificationUtils() {
        // Should not be instantiated.
    }

    /**
     * <p>
     * Create instances from a file. The instances must be given in a CSV file in the following format:
     * <code>feature1 .. featureN NominalClass</code>. Each line is one training instance.
     * </p>
     * <p>
     * Each field must be separated by {@code setFieldSeparator} and each line must end with a line break.
     * </p>
     * 
     * @param filePath The path to the CSV file to load either specified as path on the file system or as Java resource
     *            path.
     * @param readHeader <code>true</code> to treat the first line as column headers, <code>false</code> otherwise
     *            (column names are generated automatically).
     * @param fieldSeparator The separator {@code String} for individual fields.
     * @deprecated Use dedicated {@link CsvDatasetReader}.
     */
    @Deprecated
    public static List<Instance> readCsv(String filePath, final boolean readHeader, final String fieldSeparator) {
        return new CsvDatasetReader(new File(filePath), readHeader, fieldSeparator).readAll();
    }

    /**
     * <p>
     * Write {@link Classifiable} instances to a CSV file. If the instances implement {@link Trainable} (i.e. they
     * provide a target class), the target class is appended as last column after the features in the CSV.
     * </p>
     * 
     * @param data The instances to write, not <code>null</code>.
     * @param filePath The path specifying the CSV file, not <code>null</code>.
     * @deprecated Use the {@link CsvDatasetWriter} instead.
     */
    @Deprecated
    public static void writeCsv(Iterable<? extends Instance> data, File outputFile) {
        Validate.notNull(data, "data must not be null");
        Validate.notNull(outputFile, "outputFile must not be null");

        Writer writer = null;
        try {
            writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(outputFile), FileHelper.DEFAULT_ENCODING));

            boolean writeHeader = true;
            int count = 0;
            int featureCount = 0;
            for (Instance instance : data) {
                featureCount = writeLine(instance, writer, writeHeader);
                writeHeader = false;
                count++;
            }
            LOGGER.info("Wrote {} train instances with {} features to {}.", count, featureCount, outputFile);
        } catch (IOException e) {
            throw new IllegalStateException("Encountered " + e + " while writing to '" + outputFile + "'", e);
        } finally {
            FileHelper.close(writer);
        }
    }

    private static int writeLine(Instance instance, Writer writer, boolean writeHeader) throws IOException {
        if (writeHeader) {
            for (VectorEntry<String, Value> feature : instance.getVector()) {
                writer.write(feature.key());
                writer.write(DEFAULT_SEPARATOR);
            }
            writer.write("targetClass");
            writer.write(FileHelper.NEWLINE_CHARACTER);
        }
        int featureCount = 0;
        for (VectorEntry<String, Value> feature : instance.getVector()) {
            Value value = feature.value();
            if (!(value instanceof NullValue)) {
                writer.write(value.toString());
            }
            writer.write(DEFAULT_SEPARATOR);
            featureCount++;
        }
        writer.write(instance.getCategory());
        writer.write(FileHelper.NEWLINE_CHARACTER);
        return featureCount;
    }

    /**
     * <p>
     * Append a {@link Classifiable} to a CSV file. In case, the file did not exist already, the header is written. In
     * case the file already exists, only the data is written. Does <b>not</b> check, whether the given data conforms to
     * existing CSV structure (number of columns, types, ...).
     * </p>
     * 
     * @param data The data to append to the file, not <code>null</code>.
     * @param outputFile The output file to which to append, or which to create in case it does not exist. Not
     *            <code>null</code>.
     */
    public static void appendCsv(Instance instance, File outputFile) {
        Validate.notNull(instance, "instance must not be null");
        Validate.notNull(outputFile, "outputFile must not be null");

        Writer writer = null;
        boolean writeHeader = !outputFile.exists();
        try {
            writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(outputFile, true), FileHelper.DEFAULT_ENCODING));
            writeLine(instance, writer, writeHeader);
        } catch (IOException e) {
            throw new IllegalStateException("Encountered " + e + " while writing to '" + outputFile + "'", e);
        } finally {
            FileHelper.close(writer);
        }
    }

    // XXX nice would be to have this code as Classifier taking multiple models
    public static <M extends Model, T extends FeatureVector> CategoryEntries classifyWithMultipleModels(
            Classifier<M> classifier, T classifiable, M... models) {

        // merge the results
        CategoryEntriesBuilder mergedCategoryEntries = new CategoryEntriesBuilder();
        for (M model : models) {
            CategoryEntries categoryEntries = classifier.classify(classifiable, model);
            mergedCategoryEntries.add(categoryEntries);
        }

        return mergedCategoryEntries.create();
    }

}
