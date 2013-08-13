package ws.palladian.classification.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.Instance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

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
     * @param readHeader <code>true</code> to treat the first line as column headers, <code>false</code> otherwise
     *            (column names are generated automatically).
     */
    public static List<Trainable> readCsv(String filePath, boolean readHeader) {
        return readCsv(filePath, readHeader, DEFAULT_SEPARATOR);
    }

    /**
     * <p>
     * Create instances from a file. The instances must be given in a CSV file in the following format: feature1 ..
     * featureN NominalClass. Each line is one training instance.
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
     */
    public static List<Trainable> readCsv(String filePath, final boolean readHeader, final String fieldSeparator) {
        if (!new File(filePath).canRead()) {
            throw new IllegalArgumentException("Cannot find or read file \"" + filePath + "\"");
        }

        final List<Trainable> instances = CollectionHelper.newArrayList();

        FileHelper.performActionOnEveryLine(filePath, new LineAction() {

            String[] headNames;
            int expectedColumns;

            @Override
            public void performAction(String line, int lineNumber) {
                String[] parts = line.split(fieldSeparator);

                if (parts.length < 2) {
                    throw new IllegalStateException("Separator '" + fieldSeparator
                            + "'was not found, lines cannot be split ('" + line + "').");
                }

                if (lineNumber == 0) {
                    expectedColumns = parts.length;
                    if (readHeader) {
                        headNames = parts;
                        return;
                    }
                } else {
                    if (expectedColumns != parts.length) {
                        throw new IllegalStateException("Unexpected number of entries in line " + lineNumber + "("
                                + parts.length + ", but should be " + expectedColumns + ")");
                    }
                }

                FeatureVector featureVector = new FeatureVector();

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
                        featureVector.add(new NominalFeature(name, value));
                    }
                }
                String targetClass = parts[parts.length - 1];
                instances.add(new Instance(targetClass, featureVector));
            }
        });

        return instances;
    }

    /**
     * <p>
     * Write {@link Classifiable} instances to a CSV file. If the instances implement {@link Trainable} (i.e. they
     * provide a target class), the target class is appended as last column after the features in the CSV.
     * </p>
     * 
     * @param trainData The instances to write, not <code>null</code>.
     * @param filePath The path specifying the CSV file, not <code>null</code>.
     */
    public static void writeCsv(Iterable<? extends Classifiable> trainData, File outputFile) {
        Validate.notNull(trainData, "trainData must not be null");
        Validate.notNull(outputFile, "outputFile must not be null");

        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile),
                    FileHelper.DEFAULT_ENCODING));

            boolean writeHeader = true;
            int count = 0;
            int featureCount = 0;
            for (Classifiable trainable : trainData) {
                if (writeHeader) {
                    for (Feature<?> feature : trainable.getFeatureVector()) {
                        writer.write(feature.getName());
                        writer.write(DEFAULT_SEPARATOR);
                        featureCount++;
                    }
                    if (trainable instanceof Trainable) {
                        writer.write("targetClass");
                    }
                    writer.write(FileHelper.NEWLINE_CHARACTER);
                    writeHeader = false;
                }
                for (Feature<?> feature : trainable.getFeatureVector()) {
                    writer.write(feature.getValue().toString());
                    writer.write(DEFAULT_SEPARATOR);
                }
                if (trainable instanceof Trainable) {
                    writer.write(((Trainable)trainable).getTargetClass());
                }
                writer.write(FileHelper.NEWLINE_CHARACTER);
                count++;
            }
            LOGGER.info("Wrote {} train instances with {} features.", count, featureCount);
        } catch (IOException e) {
            throw new IllegalStateException("Encountered " + e + " while writing to '" + outputFile + "'", e);
        } finally {
            FileHelper.close(writer);
        }
    }

    /**
     * <p>
     * Calculate Min-Max normalization information over the numeric values of the given features (i.e. calculate the
     * minimum and maximum values for each feature). The {@link MinMaxNormalization} instance can then be used to
     * normalize numeric instances to an interval of [0,1].
     * </p>
     * 
     * @param instances The {@code List} of {@link Instance}s to normalize, not <code>null</code>.
     * @return A {@link MinMaxNormalization} instance carrying information to normalize {@link Instance}s based on the
     *         calculated normalization information.
     */
    public static MinMaxNormalization calculateMinMaxNormalization(List<? extends Classifiable> instances) {
        Validate.notNull(instances, "instances must not be null");

        // hold the min value of each feature <featureName, minValue>
        Map<String, Double> minValues = CollectionHelper.newHashMap();

        // hold the max value of each feature <featureIndex, maxValue>
        Map<String, Double> maxValues = CollectionHelper.newHashMap();

        // find the min and max values
        for (Classifiable instance : instances) {

            List<NumericFeature> numericFeatures = instance.getFeatureVector().getAll(NumericFeature.class);

            for (Feature<Double> feature : numericFeatures) {

                String featureName = feature.getName();
                double featureValue = feature.getValue();

                // check min value
                if (minValues.get(featureName) != null) {
                    double currentMin = minValues.get(featureName);
                    if (currentMin > featureValue) {
                        minValues.put(featureName, featureValue);
                    }
                } else {
                    minValues.put(featureName, featureValue);
                }

                // check max value
                if (maxValues.get(featureName) != null) {
                    double currentMax = maxValues.get(featureName);
                    if (currentMax < featureValue) {
                        maxValues.put(featureName, featureValue);
                    }
                } else {
                    maxValues.put(featureName, featureValue);
                }

            }
        }

        return new MinMaxNormalization(maxValues, minValues);
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
     * Filter features by names, as specified by the filter. A new {@link FeatureVector} containing the accpted features
     * is returned.
     * </p>
     * 
     * @param classifiable The {@link Classifiable} to filter, not <code>null</code>.
     * @param nameFilter The filter specifying which features to remove, not <code>null</code>.
     * @return The FeatureVector without the features filtered out by the nameFilter.
     */
    public static FeatureVector filterFeatures(Classifiable classifiable, Filter<String> nameFilter) {
        Validate.notNull(classifiable, "classifiable must not be null");
        Validate.notNull(nameFilter, "nameFilter must not be null");
        FeatureVector newFeatureVector = new FeatureVector();
        for (Feature<?> feature : classifiable.getFeatureVector()) {
            if (nameFilter.accept(feature.getName())) {
                newFeatureVector.add(feature);
            }
        }
        LOGGER.trace("Reduced from {} to {}", classifiable.getFeatureVector().size(), newFeatureVector.size());
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
     */
    public static List<Trainable> filterFeatures(Iterable<? extends Trainable> instances, Filter<String> nameFilter) {
        List<Trainable> result = CollectionHelper.newArrayList();
        for (Trainable instance : instances) {
            FeatureVector featureVector = ClassificationUtils.filterFeatures(instance, nameFilter);
            result.add(new Instance(instance.getTargetClass(), featureVector));
        }
        return result;
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
    public static Set<String> getFeatureNames(Collection<? extends Trainable> dataset) {
        Validate.notNull(dataset, "dataset must not be null");
        Set<String> featureNames = CollectionHelper.newTreeSet();
        Trainable instance = CollectionHelper.getFirst(dataset);
        for (Feature<?> feature : instance.getFeatureVector()) {
            featureNames.add(feature.getName());
        }
        return featureNames;
    }

}
