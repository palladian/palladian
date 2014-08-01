package ws.palladian.classification.utils;

import static ws.palladian.helper.math.MathHelper.log2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntriesBuilder;
import ws.palladian.classification.text.CountingCategoryEntriesBuilder;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.Model;
import ws.palladian.core.NominalValue;
import ws.palladian.core.NumericValue;
import ws.palladian.core.Value;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Function;
import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ImmutableNumericVector;
import ws.palladian.helper.math.NumericVector;

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
     * <code>feature1;..;featureN;NominalClass</code>. Each line is one training instance.
     * </p>
     * 
     * @param filePath The path to the CSV file to load either specified as path on the file system or as Java resource
     *            path.
     * @deprecated Use dedicated {@link CsvDatasetReader}.
     */
    @Deprecated
    public static List<Instance> readCsv(String filePath) {
        return readCsv(filePath, true, DEFAULT_SEPARATOR);
    }

    /**
     * <p>
     * Create instances from a file. The instances must be given in a CSV file in the following format:
     * <code>feature1;..;featureN;NominalClass</code>. Each line is one training instance.
     * </p>
     * 
     * @param filePath The path to the CSV file to load either specified as path on the file system or as Java resource
     *            path.
     * @param readHeader <code>true</code> to treat the first line as column headers, <code>false</code> otherwise
     *            (column names are generated automatically).
     * @deprecated Use dedicated {@link CsvDatasetReader}.
     */
    @Deprecated
    public static List<Instance> readCsv(String filePath, boolean readHeader) {
        return readCsv(filePath, readHeader, DEFAULT_SEPARATOR);
    }

    /**
     * <p>
     * Create instances from a file. The instances must be given in a CSV file in the following format:
     * <code>feature1 .. featureN NominalClass</code>. Each line is one training instance.
     * </p>
     * <p>
     * Each field must be separated by {@code fieldSeparator} and each line must end with a line break.
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
//        if (!new File(filePath).canRead()) {
//            throw new IllegalArgumentException("Cannot find or read file \"" + filePath + "\"");
//        }
//
//        final StopWatch stopWatch = new StopWatch();
//        final List<Trainable> instances = CollectionHelper.newArrayList();
//
//        FileHelper.performActionOnEveryLine(filePath, new LineAction() {
//
//            String[] headNames;
//            int expectedColumns;
//
//            @Override
//            public void performAction(String line, int lineNumber) {
//                String[] parts = line.split(fieldSeparator);
//
//                if (parts.length < 2) {
//                    throw new IllegalStateException("Separator '" + fieldSeparator
//                            + "'was not found, lines cannot be split ('" + line + "').");
//                }
//
//                if (lineNumber == 0) {
//                    expectedColumns = parts.length;
//                    if (readHeader) {
//                        headNames = parts;
//                        return;
//                    }
//                } else {
//                    if (expectedColumns != parts.length) {
//                        throw new IllegalStateException("Unexpected number of entries in line " + lineNumber + "("
//                                + parts.length + ", but should be " + expectedColumns + ")");
//                    }
//                }
//
//                FeatureVector featureVector = new BasicFeatureVector();
//
//                for (int f = 0; f < parts.length - 1; f++) {
//                    String name = headNames == null ? String.valueOf(f) : headNames[f];
//                    String value = parts[f];
//                    // FIXME make better.
//                    if (value.equals("?")) {
//                        // missing value, TODO maybe rethink what to do here and how
//                        // to handle missing values in general.
//                        continue;
//                    }
//                    try {
//                        Double doubleValue = Double.valueOf(value);
//                        featureVector.add(new NumericFeature(name, doubleValue));
//                    } catch (NumberFormatException e) {
//                        featureVector.add(new NominalFeature(name, value));
//                    }
//                }
//                String targetClass = parts[parts.length - 1];
//                instances.add(new Instance(targetClass, featureVector));
//
//                if (lineNumber % 10000 == 0) {
//                    LOGGER.debug("Read {} lines", lineNumber);
//                }
//            }
//        });
//        LOGGER.info("Read {} instances from {} in {}", instances.size(), filePath, stopWatch);
//        return instances;
        
        return new CsvDatasetReader(new File(filePath),readHeader,fieldSeparator).readAll();
    }

    /**
     * <p>
     * Write {@link Classifiable} instances to a CSV file. If the instances implement {@link Trainable} (i.e. they
     * provide a target class), the target class is appended as last column after the features in the CSV.
     * </p>
     * 
     * @param data The instances to write, not <code>null</code>.
     * @param filePath The path specifying the CSV file, not <code>null</code>.
     */
    public static void writeCsv(Iterable<? extends Instance> data, File outputFile) {
        Validate.notNull(data, "data must not be null");
        Validate.notNull(outputFile, "outputFile must not be null");

        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile),
                    FileHelper.DEFAULT_ENCODING));

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
            if (value instanceof NominalValue) {
                writer.write(((NominalValue)value).getString());
            } else if (value instanceof NumericValue) {
                writer.write(String.valueOf(((NumericValue)value).getDouble()));
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
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, true),
                    FileHelper.DEFAULT_ENCODING));
            writeLine(instance, writer, writeHeader);
        } catch (IOException e) {
            throw new IllegalStateException("Encountered " + e + " while writing to '" + outputFile + "'", e);
        } finally {
            FileHelper.close(writer);
        }
    }

    /**
     * <p>
     * Draws a fraction of the provided list by random.
     * </p>
     * 
     * @param list The {@code List} to draw from.
     * @param fraction The fraction to draw from the list.
     * @return The random subset from {@code list}.
     */
    public static <T> List<T> drawRandomSubset(final List<T> list, final int fraction) {
        Random rnd = new Random(System.currentTimeMillis());
//        int m = (fraction * list.size()) / 100;
//        for (int i = 0; i < list.size(); i++) {
//            int pos = i + rnd.nextInt(list.size() - i);
//            T tmp = list.get(pos);
//            list.set(pos, list.get(i));
//            list.set(i, tmp);
//        }
//        return list.subList(0, m);
        
        // http://stackoverflow.com/questions/136474/best-way-to-pick-a-random-subset-from-a-collection
        
        List<T> result = new ArrayList<T>(list);
        int count = (fraction * list.size()) / 100;
        
        for (int n = 0; n < count; n++) {
            int k = rnd.nextInt(result.size() - n) + n;
            T tmp = result.get(n);
            result.set(n, result.get(k));
            result.set(k, tmp);
        }
        return new ArrayList<T>(result.subList(0, count));
    }

    /**
     * <p>
     * Filter features by names, as specified by the filter. A new {@link FeatureVector} containing the accepted
     * features is returned.
     * </p>
     * 
     * @param classifiable The {@link Classifiable} to filter, not <code>null</code>.
     * @param nameFilter The filter specifying which features to remove, not <code>null</code>.
     * @return The FeatureVector without the features filtered out by the nameFilter.
     */
    public static FeatureVector filterFeatures(FeatureVector featureVector, Filter<? super String> nameFilter) {
        Validate.notNull(featureVector, "featureVector must not be null");
        Validate.notNull(nameFilter, "nameFilter must not be null");
        InstanceBuilder builder = new InstanceBuilder();
        for (VectorEntry<String, Value> entry : featureVector) {
            if (nameFilter.accept(entry.key())) {
                builder.set(entry.key(), entry.value());
            }
        }
        FeatureVector newFeatureVector = builder.create();
        LOGGER.trace("Reduced from {} to {}", featureVector.size(), newFeatureVector.size());
        return newFeatureVector;
    }

    /**
     * <p>
     * Filter features for a list of instances by names. See {@link #filterFeatures(Classifiable, Filter)}.
     * </p>
     * 
     * @param instances The instances to process, not <code>null</code>.
     * @param nameFilter The filter specifying which features to remove, not <code>null</code>.
     * @return A {@link List} with new {@link Trainable} instances containing the filtered {@link FeatureVector}.
     * @see #filterFeaturesIterable(Iterable, Filter) which does the same on an iterable, without loading the whole
     *      dataset in memory.
     */
    public static List<Instance> filterFeatures(Iterable<? extends Instance> instances,
            Filter<? super String> nameFilter) {
        List<Instance> result = CollectionHelper.newArrayList();
        for (Instance instance : instances) {
            FeatureVector featureVector = ClassificationUtils.filterFeatures(instance.getVector(), nameFilter);
            result.add(new InstanceBuilder().add(featureVector).create(instance.getCategory()));
        }
        return result;
    }

    /**
     * <p>
     * Apply a filter on the features in a dataset.
     * </p>
     * 
     * @param dataset The dataset to filter, not <code>null</code>.
     * @param nameFilter The filter specifying which features to ignore, not <code>null</code>.
     * @return A new {@link Iterable} providing the filtered feature set.
     */
    public static Iterable<Instance> filterFeaturesIterable(Iterable<? extends Instance> dataset,
            Filter<? super String> nameFilter) {
        return new DatasetFeatureFilter(dataset, nameFilter);
    }

    /**
     * <p>
     * Get the names of all features.
     * </p>
     * 
     * @param dataset
     * @return
     */
    // XXX currently, only get from first item in the dataset
    public static Set<String> getFeatureNames(Iterable<? extends FeatureVector> dataset) {
        Validate.notNull(dataset, "dataset must not be null");
        Set<String> featureNames = CollectionHelper.newTreeSet();
        FeatureVector featureVector = CollectionHelper.getFirst(dataset);
        for (VectorEntry<String, Value> entry : featureVector) {
            featureNames.add(entry.key());
        }
        return featureNames;
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

    /**
     * <p>
     * Get a {@link NumericFeature} with all numeric values from the given {@link Classifiable}. Note: This is just a
     * crutch and should be better integrated with the existing {@link FeatureVector}.
     * </p>
     * 
     * @param classifiable The classifiable, not <code>null</code>.
     * @return A {@link NumericVector} with all numeric features from the {@link Classifiable}'s {@link FeatureVector}.
     */
    public static NumericVector<String> getNumericVector(FeatureVector featureVector) {
        Validate.notNull(featureVector, "featureVector must not be null");
        Map<String, Double> values = CollectionHelper.newHashMap();
        for (VectorEntry<String, Value> entry : featureVector) {
            Value value = entry.value();
            if (value instanceof NumericValue) {
                values.put(entry.key(), ((NumericValue)value).getDouble());
            }
        }
        return new ImmutableNumericVector<String>(values);
    }

    public static Iterable<FeatureVector> unwrapInstances(Iterable<? extends Instance> instances) {
        Validate.notNull(instances, "learnables must not be null");
        return CollectionHelper.convert(instances, new Function<Instance, FeatureVector>() {
            @Override
            public FeatureVector compute(Instance input) {
                return input.getVector();
            }
        });
    }
    
    public static CategoryEntries getCategoryCounts(Iterable<? extends Instance> instances) {
        CountingCategoryEntriesBuilder builder = new CountingCategoryEntriesBuilder();
        for (Instance instance : instances) {
            builder.add(instance.getCategory(), 1);
        }
        return builder.create();
    }

    public static double entropy(CategoryEntries categoryEntries) {
        double entropy = 0;
        for (Category category : categoryEntries) {
            entropy -= category.getProbability() * log2(category.getProbability());
        }
        return entropy;
    }

}
