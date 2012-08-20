/**
 * Created on: 16.06.2012 19:27:56
 */
package ws.palladian.extraction.feature;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.patterns.SequentialPattern;
import ws.palladian.extraction.patterns.SequentialPatternsFeature;
import ws.palladian.processing.AbstractPipelineProcessor;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.Port;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;
import ws.palladian.processing.features.BooleanFeature;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

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
public final class SparseArffWriter extends AbstractPipelineProcessor<Object> {

    /**
     * <p>
     * The logger for objects of this class.
     * </p>
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SparseArffWriter.class);
    /**
     * <p>
     * Used for serializing objects of this class. Should only change if the attribute set of this class changes.
     * </p>
     */
    private static final long serialVersionUID = -8674006178227544037L;
    /**
     * <p>
     * The target ARFF file this writer saves data to.
     * </p>
     */
    private final File targetFile;
    /**
     * <p>
     * The {@link FeatureDescriptor}s of the {@link Feature} the writer should consider when saving.
     * </p>
     */
    private final List<FeatureDescriptor<? extends Feature<?>>> featureDescriptors;

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
    private Integer featuresAdded;

    /**
     * <p>
     * Creates a new {@code SparseArffWriter} saving all data identified by the provided {@link FeatureDescriptor}s to
     * the file specified by {@code fileName}, creating that file if it does not exist and overwriting it if it already
     * exists.
     * </p>
     * 
     * @param fileName The name of the target ARFF file this writer should write to.
     * @param featureDescriptors The {@link FeatureDescriptor}s of the {@link Feature} the writer should consider when
     *            saving.
     * @throws IOException If the target file could not be initialized successfully.
     */
    public SparseArffWriter(final String fileName, final FeatureDescriptor<? extends Feature<?>>... featureDescriptors)
            throws IOException {
        this(fileName, true, 1, featureDescriptors);
    }

    /**
     * <p>
     * Creates a new {@code SparseArffWriter} saving all data identified by the provided {@link FeatureDescriptor}s to
     * the file specified by {@code modelArffFile}, creating that file if it does not exist. If it exists it is either
     * overwriten or any new data is appended. This depends on the value of {@code overwrite}.
     * </p>
     * 
     * @param fileName The name of the target ARFF file this writer should write to.
     * @param overwrite If {@code true} overwrites the ARFF file created by this ARFF writer. If {@code false} appends
     *            new data to the created ARFF file if it already exists.
     * @param featureDescriptors The {@link FeatureDescriptor}s of the {@link Feature} the writer should consider when
     *            saving.
     * @throws IOException If the target file could not be initialized successfully.
     */
    public SparseArffWriter(final String fileName, final Boolean overwrite,
            final FeatureDescriptor<? extends Feature<?>>... featureDescriptors) throws IOException {
        this(fileName, overwrite, 1, featureDescriptors);
    }

    /**
     * <p>
     * Creates a new {@code SparseArffWriter} saving all data identified by the provided {@link FeatureDescriptor}s to
     * the file specified by {@code fileName}, creating that file if it does not exist and overwriting it if it already
     * exists.
     * </p>
     * <p>
     * To speed up processing or save memory you can experiment with the value of {@code batchSize}.
     * </p>
     * 
     * @param fileName The name of the target ARFF file this writer should write to.
     * @param batchSize The number of documents this ARFF writer should remember when used inside a continuous
     *            processing run, until data is written to the ARFF file. A larger {@code batchSize} reduces disk access
     *            and thus improves performance but requires more memory.
     * @param featureDescriptors The {@link FeatureDescriptor}s of the {@link Feature} the writer should consider when
     *            saving.
     * @throws IOException If the target file could not be initialized successfully.
     */
    public SparseArffWriter(final String fileName, final Integer batchSize,
            final FeatureDescriptor<? extends Feature<?>>... featureDescriptors) throws IOException {
        this(fileName, true, batchSize, featureDescriptors);
    }

    /**
     * <p>
     * Creates a new {@code SparseArffWriter} saving all data identified by the provided {@link FeatureDescriptor}s to
     * the file specified by {@code modelArffFile}, creating that file if it does not exist. If it exists it is either
     * overwriten or any new data is appended. This depends on the value of {@code overwrite}.
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
     * @param featureDescriptors The {@link FeatureDescriptor}s of the {@link Feature} the writer should consider when
     *            saving.
     * @throws IOException If the target file could not be initialized successfully.
     */
    public SparseArffWriter(final String fileName, final Boolean overwrite, final Integer batchSize,
            final FeatureDescriptor<? extends Feature<?>>... featureDescriptors) throws IOException {
        this(new File(fileName), overwrite, batchSize, featureDescriptors);
    }

    /**
     * <p>
     * Creates a new {@code SparseArffWriter} saving all data identified by the provided {@link FeatureDescriptor}s to
     * the file specified by {@code modelArffFile}, creating that file if it does not exist. If it exists it is either
     * overwriten or any new data is appended. This depends on the value of {@code overwrite}.
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
     * @param featureDescriptors The {@link FeatureDescriptor}s of the {@link Feature} the writer should consider when
     *            saving.
     * @throws IOException If the target file could not be initialized successfully.
     */
    public SparseArffWriter(final File modelArffFile, final Boolean overwrite, final Integer batchSize,
            FeatureDescriptor<? extends Feature<?>>[] featureDescriptors) throws IOException {
        super(Arrays.asList(new Port<?>[] {new Port<Object>(DEFAULT_INPUT_PORT_IDENTIFIER)}), new ArrayList<Port<?>>());

        Validate.notNull(modelArffFile, "fileName must not be null");
        Validate.notEmpty(featureDescriptors, "featureDescriptors must not be empty");

        featureTypes = new DualHashBidiMap<String, Integer>();
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
        this.featureDescriptors = Arrays.asList(featureDescriptors);
    }

    /**
     * <p>
     * Creates a new {@code SparseArffWriter} saving all data identified by the provided {@link FeatureDescriptor}s to
     * the file specified by {@code modelArffFile}, creating that file if it does not exist. If it exists it is either
     * overwriten or any new data is appended. This depends on the value of {@code overwrite}.
     * </p>
     * 
     * @param modelArffFile The name of the target ARFF file this writer should write to.
     * @param overwrite If {@code true} overwrites the ARFF file created by this ARFF writer. If {@code false} appends
     *            new data to the created ARFF file if it already exists.
     * @param featureDescriptors The {@link FeatureDescriptor}s of the {@link Feature} the writer should consider when
     *            saving.
     * @throws IOException If the target file could not be initialized successfully.
     */
    public SparseArffWriter(final File modelArffFile, final Boolean overwrite,
            final FeatureDescriptor<? extends Feature<?>>... featureDescriptors) throws IOException {
        this(modelArffFile, overwrite, 1, featureDescriptors);
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
        List<String> lines = FileUtils.readLines(targetFile);

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
                Integer featureTypeIndex = featureTypes.get(entry[0]);
                instance.add(new ImmutablePair(attributeIndex, entry[1]));
            }
            instances.add(instance);
        }
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        PipelineDocument<Object> document = getDefaultInput();
        List<Pair<Integer, String>> newInstance = new LinkedList<Pair<Integer, String>>();
        for (Feature<?> feature : document.getFeatureVector()) {
            handleFeature(feature, newInstance);
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
    private void saveModel() throws IOException {
        LOGGER.info("Saving attributes:");
        FileOutputStream arffFileStream = new FileOutputStream(targetFile);
        IOUtils.write("@relation model\n\n ", arffFileStream);
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

                IOUtils.write("@attribute " + featureTypeBuilder.toString() + "\n", arffFileStream);

                LOGGER.debug("Saved {}% of schema to ARFF file.",
                        i.doubleValue() * 100.0d / featuresAdded.doubleValue());
            }

            IOUtils.write("\n@data\n", arffFileStream);

            LOGGER.info("Saving instances:");
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
                IOUtils.write(instanceBuilder.toString(), arffFileStream);

                LOGGER.debug("Saved {}% of all instances to ARFF file.", instanceCounter.doubleValue() * 100.0d
                        / instances.size());
                instanceCounter++;
            }
        } finally {
            IOUtils.closeQuietly(arffFileStream);
        }
    }

    /**
     * <p>
     * Handles one {@link Feature} from the {@link FeatureVector} of the current instance.
     * </p>
     * 
     * @param feature The {@code Feature} to handle.
     * @param newInstance The instance the feature should be handled for. This is basically a {@code List} of already
     *            handled {@code Feature}s to which the current {@code Feature} is added.
     */
    private void handleFeature(final Feature<?> feature, final List<Pair<Integer, String>> newInstance) {
        FeatureDescriptor<?> descriptor = feature.getDescriptor();
        if (feature instanceof AnnotationFeature) {
            AnnotationFeature<?> annotationFeature = (AnnotationFeature<?>)feature;
            for (Annotation<?> annotation : annotationFeature.getValue()) {
                for (Feature<?> subFeature : annotation.getFeatureVector()) {
                    handleFeature(subFeature, newInstance);
                }
            }
        }

        if (!featureDescriptors.contains(descriptor)) {
            return;
        }

        if (feature instanceof NumericFeature) {
            handleNumericFeature((NumericFeature)feature, newInstance);
        } else if (feature instanceof AnnotationFeature) {
            AnnotationFeature<?> annotationFeature = (AnnotationFeature<?>)feature;
            handleAnnotationFeature(annotationFeature, newInstance);
        } else if (feature instanceof BooleanFeature) {
            handleBooleanFeature((BooleanFeature)feature, newInstance);
        } else if (feature instanceof NominalFeature) {
            handleNominalFeature((NominalFeature)feature, newInstance);
        } else if (feature instanceof SequentialPatternsFeature) {
            handleSequentialPatterns((SequentialPatternsFeature)feature, newInstance);
        } else if (feature instanceof ListFeature) {
            handleListFeature((ListFeature)feature, newInstance);
        }
    }

    /**
     * <p>
     * A handle method for {@link ListFeature}s.
     * </p>
     * 
     * @param feature {@see #handleFeature(Feature, List)}
     * @param newInstance {@see #handleFeature(Feature, List)}
     */
    private void handleListFeature(ListFeature feature, List<Pair<Integer, String>> newInstance) {
        List<Object> elements = feature.getValue();
        for (Object element : elements) {
            String featureType = "\"" + element.toString() + "\" numeric";

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
    }

    /**
     * <p>
     * Adds all sequential patterns from a {@code SequentialPatternsFeature} to the created Arff file.
     * </p>
     * 
     * @param feature
     *            {@see #handleFeature(Feature, List)}
     * @param newInstance
     *            {@see #handleFeature(Feature, List)}
     */
    private void handleSequentialPatterns(final SequentialPatternsFeature feature,
            final List<Pair<Integer, String>> newInstance) {
        List<SequentialPattern> sequentialPatterns = feature.getValue();
        for (SequentialPattern pattern : sequentialPatterns) {
            String featureType = "\"" + pattern.getStringValue() + "\" numeric";

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
        // StringBuilder featureTypeBuilder = new StringBuilder("\"" + feature.getName() + "\" {dummy");

        // for (String value : feature.getPossibleValues()) {
        // featureTypeBuilder.append(",");
        // featureTypeBuilder.append(value);
        // }
        // featureTypeBuilder.append("}");
        // String featureType = featureTypeBuilder.toString();
        String featureType = "\"" + feature.getName() + "\"";

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
     * 
     * </p>
     * 
     * @param feature
     * @param newInstance
     * @param schema
     */
    private void handleBooleanFeature(BooleanFeature feature, List<Pair<Integer, String>> newInstance) {
        String featureType = "\"" + feature.getName() + "\" {dummy,true,false}";

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
     * 
     * </p>
     * 
     * @param feature
     * @param model
     * @param schema
     */
    private void handleAnnotationFeature(AnnotationFeature<?> feature, List<Pair<Integer, String>> newInstance) {
        for (Annotation<?> annotation : feature.getValue()) {
            String featureType = "\"" + annotation.getValue() + "\" numeric";

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
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param feature
     * @param model
     * @param schema
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
        try {
            saveModel();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
