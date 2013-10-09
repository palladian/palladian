/**
 * Created on: 16.06.2012 19:27:56
 */
package ws.palladian.extraction.feature;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.BidiMap;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.AbstractPipelineProcessor;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.InputPort;
import ws.palladian.processing.OutputPort;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.BooleanFeature;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.SequentialPattern;
import ws.palladian.processing.features.SparseFeature;

/**
 * <p>
 * Saves the {@link Feature}s from the {@link PipelineDocument} provided to this component as a sparse ARFF file. The
 * ARFF file format is used as input for Weka classifiers. For further information see <a
 * href="http://www.cs.waikato.ac.nz/ml/weka/">the Weka Website</a>.
 * </p>
 * <p>
 * This writer is especially used for the sparse ARFF format which is a little different then the normal ARFF format.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class SparseArffWriter extends AbstractPipelineProcessor {

    /**
     * <p>
     * The logger for objects of this class.
     * </p>
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SparseArffWriter.class);
    /**
     * <p>
     * The target ARFF file this writer saves data to.
     * </p>
     */
    private final File targetFile;

    /**
     * <p>
     * The current schema area of the ARFF file. This is a bidirectional mapping. The left side is the ARFF schema
     * string such as '"www" numeric', that follows the '@attribute' part of each line in an ARFF schema. The right side
     * is the index of that attribute so that the writer always knows on which line it has to print the attribute.
     * </p>
     */
    private final BidiMap<String, Integer> featureTypes;

    /**
     * <p>
     * A collection of all values possible for each {@link NominalFeature} in the dataset.
     * </p>
     */
    private final Map<Integer, Set<String>> nominalPossibleValues;
    /**
     * <p>
     * The currently available instances in the data part of the ARFF file. This is a list of lists. Each sublist is one
     * instance consisting of pairs of {@code Integer} and {@code String}. The {@code Integer} is the index of the
     * attribute and the {@code String} is the value for the attribute for that instance.
     * </p>
     */
    private final List<List<Pair<Integer, String>>> instances;
    /**
     * <p>
     * A counter of how many features were already added used to assign a correct index to new {@link #featureTypes}.
     * </p>
     */
    private int featuresAdded;

    /**
     * <p>
     * Creates a new {@code SparseArffWriter} saving all data identified by the provided feature identifiers to the file
     * specified by {@code fileName}, creating that file if it does not exist and overwriting it if it already exists.
     * </p>
     * 
     * @param fileName The name of the target ARFF file this writer should write to.
     * @throws IOException If the target file could not be initialized successfully.
     */
    public SparseArffWriter(final String fileName) throws IOException {
        this(fileName, true, 1);
    }

    /**
     * <p>
     * Creates a new {@code SparseArffWriter} saving all data identified by the provided feature identifiers to the file
     * specified by {@code modelArffFile}, creating that file if it does not exist. If it exists it is either overwriten
     * or any new data is appended. This depends on the value of {@code overwrite}.
     * </p>
     * 
     * @param fileName The name of the target ARFF file this writer should write to.
     * @param overwrite If {@code true} overwrites the ARFF file created by this ARFF writer. If {@code false} appends
     *            new data to the created ARFF file if it already exists.
     * @throws IOException If the target file could not be initialized successfully.
     */
    public SparseArffWriter(final String fileName, final boolean overwrite) throws IOException {
        this(fileName, overwrite, 1);
    }

    /**
     * <p>
     * Creates a new {@code SparseArffWriter} saving all data identified by the provided feature identifiers to the file
     * specified by {@code fileName}, creating that file if it does not exist and overwriting it if it already exists.
     * </p>
     * <p>
     * To speed up processing or save memory you can experiment with the value of {@code batchSize}.
     * </p>
     * 
     * @param fileName The name of the target ARFF file this writer should write to.
     * @param batchSize The number of documents this ARFF writer should remember when used inside a continuous
     *            processing run, until data is written to the ARFF file. A larger {@code batchSize} reduces disk access
     *            and thus improves performance but requires more memory.
     * @throws IOException If the target file could not be initialized successfully.
     */
    public SparseArffWriter(final String fileName, final int batchSize) throws IOException {
        this(fileName, true, batchSize);
    }

    /**
     * <p>
     * Creates a new {@code SparseArffWriter} saving all data identified by the provided feature identifiers to the file
     * specified by {@code modelArffFile}, creating that file if it does not exist. If it exists it is either overwriten
     * or any new data is appended. This depends on the value of {@code overwrite}.
     * </p>
     * <p>
     * To speed up processing or save memory you can experiment with the value of {@code batchSize}.
     * </p>
     * 
     * @param fileName The name of the target ARFF file this writer should write to.
     * @param overwrite If {@code true} overwrites the ARFF file created by this ARFF writer. If {@code false} appends
     *            new data to the created ARFF file if it already exists.
     * @param batchSize The number of documents this ARFF writer should remember when used inside a continuous
     *            processing run, until data is written to the ARFF file. A larger {@code batchSize} reduces disk access
     *            and thus improves performance but requires more memory.
     * @throws IOException If the target file could not be initialized successfully.
     */
    public SparseArffWriter(final String fileName, final boolean overwrite, final int batchSize) throws IOException {
        this(new File(fileName), overwrite, batchSize);
    }

    /**
     * <p>
     * Creates a new {@code SparseArffWriter} saving all data identified by the provided feature identifiers to the file
     * specified by {@code modelArffFile}, creating that file if it does not exist. If it exists it is either overwriten
     * or any new data is appended. This depends on the value of {@code overwrite}.
     * </p>
     * <p>
     * To speed up processing or save memory you can experiment with the value of {@code batchSize}.
     * </p>
     * 
     * @param modelArffFile The name of the target ARFF file this writer should write to.
     * @param overwrite If {@code true} overwrites the ARFF file created by this ARFF writer. If {@code false} appends
     *            new data to the created ARFF file if it already exists.
     * @param batchSize The number of documents this ARFF writer should remember when used inside a continuous
     *            processing run, until data is written to the ARFF file. A larger {@code batchSize} reduces disk access
     *            and thus improves performance but requires more memory.
     * @throws IOException If the target file could not be initialized successfully.
     */
    public SparseArffWriter(final File modelArffFile, final boolean overwrite, final int batchSize) throws IOException {
        super(new InputPort[] {new InputPort(DEFAULT_INPUT_PORT_IDENTIFIER)}, new OutputPort[0]);
        Validate.notNull(modelArffFile, "fileName must not be null");

        featureTypes = new BidiMap<String, Integer>();
        instances = new LinkedList<List<Pair<Integer, String>>>();
        nominalPossibleValues = new HashMap<Integer, Set<String>>();
        featuresAdded = 0;

        this.targetFile = modelArffFile;
        if (targetFile.exists()) {
            if (overwrite) {
                targetFile.delete();
            } else {
                readExistingArffFile();
            }
        }
    }

    /**
     * <p>
     * Creates a new {@code SparseArffWriter} saving all data identified by the provided feature identifiers to the file
     * specified by {@code modelArffFile}, creating that file if it does not exist. If it exists it is either overwriten
     * or any new data is appended. This depends on the value of {@code overwrite}.
     * </p>
     * 
     * @param modelArffFile The name of the target ARFF file this writer should write to.
     * @param overwrite If {@code true} overwrites the ARFF file created by this ARFF writer. If {@code false} appends
     *            new data to the created ARFF file if it already exists.
     * @throws IOException If the target file could not be initialized successfully.
     */
    public SparseArffWriter(final File modelArffFile, final Boolean overwrite) throws IOException {
        this(modelArffFile, overwrite, 1);
    }

    /**
     * <p>
     * Reads an existing ARFF file populating the in memory model of this writer, so that it can extent on that model.
     * </p>
     * 
     * @throws IOException If the ARFF file is not accessible.
     * 
     */
    private void readExistingArffFile() throws IOException {
        List<String> lines = FileHelper.readFileToArray(targetFile);

        int currentLineIndex = 0;
        String currentLine = lines.get(0).trim();
        // read the header
        while (!currentLine.startsWith("@attribute")) {
            currentLineIndex++;
            currentLine = lines.get(currentLineIndex).trim();
        }

        int attributeIndex = 0;
        // read the schema
        while (currentLine.startsWith("@attribute")) {
            String featureType = currentLine.replaceFirst("@attribute ", "");
            featureType = featureType.replace("\n", "");

            // Read possible values for nominal values
            Pattern pattern = Pattern.compile("\".*?\"\\s{dummy,(.*?)}");
            java.util.regex.Matcher featureMatcher = pattern.matcher(featureType);
            if (featureMatcher.matches()) {
                String[] possibleValues = featureMatcher.group().split(",");
                nominalPossibleValues.put(attributeIndex, new HashSet<String>(Arrays.asList(possibleValues)));
            }

            featureTypes.put(featureType, attributeIndex);
            currentLineIndex++;
            currentLine = lines.get(currentLineIndex).trim();
        }

        // jump to data section
        while (!currentLine.startsWith("@data")) {
            currentLineIndex++;
            currentLine = lines.get(currentLineIndex).trim();
        }

        // read the data
        while (currentLineIndex < lines.size() - 1 && !currentLine.isEmpty()) {
            // this needs to happen at the beginning to jump over the @data line
            currentLineIndex++;
            currentLine = lines.get(currentLineIndex).trim();

            // remove curly braces at the beginning and the end
            currentLine.substring(1, currentLine.length() - 1);
            String[] dataEntries = currentLine.split(",");
            List<Pair<Integer, String>> instance = new ArrayList<Pair<Integer, String>>();
            for (String dataEntry : dataEntries) {
                String[] entry = dataEntry.split(" ");
                instance.add(new ImmutablePair<Integer, String>(attributeIndex, entry[1]));
            }
            instances.add(instance);
        }
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        PipelineDocument<?> document = getInputPort(DEFAULT_INPUT_PORT_IDENTIFIER).poll();
        addFeatureVectorToOutput(document.getFeatureVector());
    }

    /**
     * <p>
     * Adds the provided {@link FeatureVector} to the schema this ARFF writer is currently creating. After you have
     * added als {@link FeatureVector}s you wish please call {@link #saveModel()} to write the created schema to an
     * actual ARFF file.
     * </p>
     * 
     * @param vector The {@link FeatureVector} to add to the ARFF File.
     */
    public void addFeatureVectorToOutput(final FeatureVector vector) {
        Validate.notNull(vector);
        
        SortedSet<String> sortedFeatureNames = CollectionHelper.newTreeSet();
        for (Feature<?> feature : vector) {
            sortedFeatureNames.add(feature.getName());
        }

        List<Pair<Integer, String>> newInstance = new LinkedList<Pair<Integer, String>>();
        for (String featureName : sortedFeatureNames) {
            handleFeature(vector.get(featureName), newInstance);
        }
        instances.add(newInstance);
    }

    /**
     * <p>
     * Saves the model this writer created up to now to the ARFF file.
     * </p>
     * 
     * @throws IOException If the ARFF file is not accessible.
     */
    public void saveModel() throws IOException {
        LOGGER.trace("Saving attributes");
        FileOutputStream arffFileStream = new FileOutputStream(targetFile);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(arffFileStream));
        writer.write("@relation model\n\n");
        try {
            for (Integer i = 0; i < featuresAdded; i++) {
                String featureType = featureTypes.getKey(i);
                if (featureType == null) {
                    throw new IllegalStateException("No feature type at index: " + i + " expected to write "
                            + (featuresAdded - 1) + " feature types.");
                }
                StringBuilder featureTypeBuilder = new StringBuilder(featureType);

                // write possible values for nominal attributes
                Set<String> possibleValues = nominalPossibleValues.get(i);
                if (possibleValues != null) {
                    featureTypeBuilder.append(" {wekadummy");
                    for (String possibleValue : possibleValues) {
                        featureTypeBuilder.append("," + possibleValue);
                    }
                    featureTypeBuilder.append("}");
                }

                writer.write("@attribute " + featureTypeBuilder.toString() + "\n");

                LOGGER.debug("Saved {}% of schema to ARFF file.", i.doubleValue() * 100.0d / (double)featuresAdded);
            }

            writer.write("\n@data\n");

            LOGGER.trace("Saving instances");
            Integer instanceCounter = 0;
            for (List<Pair<Integer, String>> instance : instances) {
                StringBuilder instanceBuilder = new StringBuilder("{");
                Collections.sort(instance);
                boolean isStart = true;
                for (Pair<Integer, String> feature : instance) {
                    // prepend a comma only if this is not the first feature.
                    if (!isStart) {
                        instanceBuilder.append(",");
                    }
                    isStart = false;
                    instanceBuilder.append(feature.getLeft());
                    instanceBuilder.append(" ");
                    instanceBuilder.append(feature.getRight());
                }
                instanceBuilder.append("}\n");
                writer.write(instanceBuilder.toString());

                LOGGER.debug("Saved {}% of all instances to ARFF file.", instanceCounter.doubleValue() * 100.0d
                        / instances.size());
                instanceCounter++;
            }
        } finally {
            FileHelper.close(writer, arffFileStream);
        }
    }

    /**
     * <p>
     * Handles one {@link Feature} from the {@link FeatureVector} of the current instance.
     * </p>
     * 
     * @param feature The {@code Feature} to handle.
     * @param newInstance The instance the feature should be handled for. This is basically a {@code List} of already
     *            handled {@code Feature}s to which the current {@code Feature} is added. The mapping is from the
     *            attributes index in the ARFF file to the feature's name. An attribute is a synonym for a feature in
     *            Weka.
     */
    private void handleFeature(final Feature<?> feature, final List<Pair<Integer, String>> newInstance) {
        if (feature instanceof NumericFeature) {
            handleNumericFeature((NumericFeature)feature, newInstance);
            // } else if (feature instanceof AnnotationFeature) {
            // AnnotationFeature<?> annotationFeature = (AnnotationFeature<?>)feature;
            // handleAnnotationFeature(annotationFeature, newInstance);
        } else if (feature instanceof BooleanFeature) {
            handleBooleanFeature((BooleanFeature)feature, newInstance);
        } else if (feature instanceof NominalFeature) {
            handleNominalFeature((NominalFeature)feature, newInstance);
        } else if (feature instanceof SequentialPattern) {
            handleSequentialPattern((SequentialPattern)feature, newInstance);
        } else if (feature instanceof ListFeature) {
            handleSparseFeature((ListFeature)feature, newInstance);
        }
    }

    /**
     * <p>
     * Adds all sequential patterns from a {@code SequentialPatternsFeature} to the created ARFF file.
     * </p>
     * 
     * @param feature
     *            {@see #handleFeature(Feature, List)}
     * @param newInstance
     *            {@see #handleFeature(Feature, List)}
     */
    private void handleSequentialPattern(final SequentialPattern feature, final List<Pair<Integer, String>> newInstance) {
        String featureType = "\"" + mask(SequentialPattern.getStringValue(feature.getValue())) + "\" numeric";

        Integer featureTypeIndex = featureTypes.get(featureType);
        if (featureTypeIndex == null) {
            featureTypes.put(featureType, featuresAdded);
            featureTypeIndex = featuresAdded;
            featuresAdded++;
        }

        ImmutablePair<Integer, String> featureValue = new ImmutablePair<Integer, String>(featureTypeIndex, "1.0");
        if (!newInstance.contains(featureValue)) {
            newInstance.add(featureValue);
        }
    }

    /**
     * <p>
     * Handles addition of {@link NominalFeature}s.
     * </p>
     * 
     * @param feature {@see #handleFeature(Feature, List)}
     * @param newInstance {@see #handleFeature(Feature, List)}
     */
    private void handleNominalFeature(final NominalFeature feature, final List<Pair<Integer, String>> newInstance) {
        String featureType = "\"" + mask(feature.getName()) + "\"";

        Integer featureTypeIndex = featureTypes.get(featureType);
        if (featureTypeIndex == null) {
            featureTypeIndex = featuresAdded;
            featureTypes.put(featureType, featureTypeIndex);
            featuresAdded++;
        }

        Set<String> possibleValues = nominalPossibleValues.get(featureTypeIndex);
        if (possibleValues == null) {
            possibleValues = new HashSet<String>();
        }
        possibleValues.add(feature.getValue());
        nominalPossibleValues.put(featureTypeIndex, possibleValues);

        ImmutablePair<Integer, String> featureValue = new ImmutablePair<Integer, String>(featureTypeIndex,
                feature.getValue());
        if (!newInstance.contains(featureValue)) {
            newInstance.add(featureValue);
        }
    }

    /**
     * <p>
     * Write the provided {@link BooleanFeature} to the ARFF file. This method adds the {@link NumericFeature} to the
     * ARFF files schema if it was not added yet and also to the {@code newInstance}
     * </p>
     * 
     * @param feature the feature to add to the instance and the ARFF file if necessary.
     * @param newInstance Saves the information to write the currently handled instance to the ARFF file. The mapping is
     *            from the index of the attribute in the ARFF file schema to the attributes name. An attribute is a
     *            synonym for a feature in Weka.
     */
    private void handleBooleanFeature(BooleanFeature feature, List<Pair<Integer, String>> newInstance) {
        String featureType = "\"" + mask(feature.getName()) + "\" {dummy,true,false}";

        Integer featureTypeIndex = featureTypes.get(featureType);
        if (featureTypeIndex == null) {
            featureTypes.put(featureType, featuresAdded);
            featureTypeIndex = featuresAdded;
            featuresAdded++;
        }

        ImmutablePair<Integer, String> featureValue = new ImmutablePair<Integer, String>(featureTypeIndex, feature
                .getValue().toString());
        if (!newInstance.contains(featureValue)) {
            newInstance.add(featureValue);
        }
    }

    /**
     * <p>
     * Write the provided {@link ListFeature} to the ARFF file. This method adds the {@link NumericFeature} to the ARFF
     * files schema if it was not added yet and also to the {@code newInstance}
     * </p>
     * 
     * @param feature the feature to add to the instance and the ARFF file if necessary.
     * @param newInstance Saves the information to write the currently handled instance to the ARFF file. The mapping is
     *            from the index of the attribute in the ARFF file schema to the attributes name. An attribute is a
     *            synonym for a feature in Weka.
     */
    private void handleSparseFeature(ListFeature<Feature<?>> feature, List<Pair<Integer, String>> newInstance) {
        for (Feature<?> value : feature.getValue()) {
            if (value instanceof SparseFeature) {
                String featureType = "\"" + mask(value.getName()) + "\" numeric";
                
                Integer featureTypeIndex = featureTypes.get(featureType);
                if (featureTypeIndex == null) {
                    featureTypes.put(featureType, featuresAdded);
                    featureTypeIndex = featuresAdded;
                    featuresAdded++;
                }
                
                ImmutablePair<Integer, String> featureValue = new ImmutablePair<Integer, String>(featureTypeIndex,
                        "1.0");
                if (!newInstance.contains(featureValue)) {
                    newInstance.add(featureValue);
                }
            } else {
                handleFeature((Feature<?>)value, newInstance);
            }
        }
    }

    /**
     * <p>
     * Masks special characters in an attributes name, that are required by Weka.
     * </p>
     * 
     * @param string The {@link String} to mask.
     * @return The new masked {@link String}.
     */
    private String mask(String string) {
        String maskedString = string.replace("\\", "\\\\");
        maskedString = maskedString.replace("\"", "\\\"");
        return maskedString;
    }

    /**
     * <p>
     * Write the provided {@link NumericFeature} to the ARFF file. This method adds the {@link NumericFeature} to the
     * ARFF files schema if it was not added yet and also to the {@code newInstance}
     * </p>
     * 
     * @param feature the feature to add to the instance and the ARFF file if necessary.
     * @param newInstance Saves the information to write the currently handled instance to the ARFF file. The mapping is
     *            from the index of the attribute in the ARFF file schema to the attributes name. An attribute is a
     *            synonym for a feature in Weka.
     */
    private void handleNumericFeature(NumericFeature feature, List<Pair<Integer, String>> newInstance) {
        String featureType = "\"" + feature.getName() + "\" numeric";

        Integer featureTypeIndex = featureTypes.get(featureType);
        if (featureTypeIndex == null) {
            featureTypes.put(featureType, featuresAdded);
            featureTypeIndex = featuresAdded;
            featuresAdded++;
        }

        ImmutablePair<Integer, String> featureValue = new ImmutablePair<Integer, String>(featureTypeIndex, feature
                .getValue().toString());
        if (!newInstance.contains(featureValue)) {
            newInstance.add(featureValue);
        }
    }

    @Override
    public void processingFinished() {
        // try {
        // TODO does currently not work since we have no continuous processing pipeline
        // saveModel();
        // } catch (IOException e) {
        // throw new IllegalStateException(e);
        // }
    }
}
