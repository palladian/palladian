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
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import weka.core.FastVector;
import weka.core.Instances;
import ws.palladian.extraction.AbstractPipelineProcessor;
import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.Port;
import ws.palladian.extraction.patterns.SequentialPattern;
import ws.palladian.extraction.patterns.SequentialPatternsFeature;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.BooleanFeature;
import ws.palladian.model.features.Feature;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.NominalFeature;
import ws.palladian.model.features.NumericFeature;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class SparseArffWriter extends AbstractPipelineProcessor<Object> {

    private static final Logger LOGGER = Logger.getLogger(SparseArffWriter.class);
    /**
     * <p>
     * Used for serializing objects of this class. Should only change if the attribute set of this class changes.
     * </p>
     */
    private static final long serialVersionUID = -8674006178227544037L;
    /**
     * <p>
     * 
     * </p>
     */
    private final File targetFile;
    private final List<FeatureDescriptor<? extends Feature<?>>> featureDescriptors;
    // private final Integer batchSize;
    private Instances model;

    // private Integer processedDocuments;
    private final BidiMap<String, Integer> featureTypes;
    private final List<List<Pair<Integer, String>>> instances;
    private Integer featuresAdded;

    /**
     * <p>
     * Creates a new {@code SparseArffWriter} saving all data identified by the provided {@link FeatureDescriptor}s to
     * the file specified by {@code fileName}, creating that file if it does not exist and overwriting it if it already
     * exists.
     * </p>
     * 
     * @param fileName
     * @param featureDescriptors
     */
    public SparseArffWriter(final String fileName, final FeatureDescriptor<? extends Feature<?>>... featureDescriptors) {
        this(fileName, 1, featureDescriptors);
    }

    /**
     * <p>
     * Creates a new {@code SparseArffWriter} saving all data identified by the provided {@link FeatureDescriptor}s to
     * the file specified by {@code fileName}, creating that file if it does not exist and overwriting it if it already
     * exists.
     * </p>
     * 
     * @param fileName
     * @param batchSize
     * @param featureDescriptors
     */
    public SparseArffWriter(final String fileName, final Integer batchSize,
            final FeatureDescriptor<? extends Feature<?>>... featureDescriptors) {
        super(Arrays.asList(new Port<?>[] {new Port<Object>(DEFAULT_INPUT_PORT_IDENTIFIER)}), new ArrayList<Port<?>>());

        Validate.notNull(fileName, "fileName must not be null");
        Validate.notEmpty(featureDescriptors, "featureDescriptors must not be empty");

        this.targetFile = new File(fileName);
        if (targetFile.exists()) {
            targetFile.delete();
        }
        this.featureDescriptors = Arrays.asList(featureDescriptors);
        // this.batchSize = batchSize;
        FastVector schema = new FastVector();
        this.model = new Instances("model", schema, batchSize);
        // this.processedDocuments = 0;
        featureTypes = new DualHashBidiMap<String, Integer>();
        instances = new LinkedList<List<Pair<Integer, String>>>();
        featuresAdded = 0;
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        PipelineDocument<Object> document = getDefaultInput();
        // try {
        // checkAndUpdateBatch();

        // Instance newInstance = new SparseInstance(0);
        // newInstance.setDataset(model);
        List<Pair<Integer, String>> newInstance = new LinkedList<Pair<Integer, String>>();
        for (Feature<?> feature : document.getFeatureVector()) {
            handleFeature(feature, newInstance);
        }
        instances.add(newInstance);
        // model.add(newInstance);

        // } catch (IOException e) {
        // throw new DocumentUnprocessableException(e);
        // } catch (ArrayIndexOutOfBoundsException e) {
        // throw new DocumentUnprocessableException(e);
        // }
    }

    // /**
    // * <p>
    // *
    // * </p>
    // *
    // * @throws IOException
    // *
    // */
    // private void checkAndUpdateBatch() throws IOException {
    // // processedDocuments++;
    // // if (processedDocuments == batchSize) {
    //
    // if (targetFile.exists()) {
    // FileReader in = new FileReader(targetFile);
    // try {
    // BufferedReader reader = new BufferedReader(in);
    // ArffReader arff = new ArffReader(reader);
    // Instances oldModel = arff.getData();
    // Instances mergedModel = mergeModels(oldModel, model);
    // saveModel(mergedModel);
    // } finally {
    // IOUtils.closeQuietly(in);
    // }
    // } else {
    // saveModel(model);
    // }
    // // FastVector schema = new FastVector();
    // // model = new Instances("model", schema, batchSize);
    // // processedDocuments = 0;
    // // }
    // }

    private void saveModel() throws IOException {
        LOGGER.info("Saving attributes:");
        FileOutputStream arffFileStream = new FileOutputStream(targetFile);
        try {
            for (Integer i = 0; i < featuresAdded; i++) {
                String featureType = featureTypes.getKey(i);
                if (featureType == null) {
                    throw new IllegalStateException("No feature type at index: " + i + " expected to write "
                            + (featuresAdded - 1) + " feature types.");
                }
                IOUtils.write("@attribute " + featureType + "\n", arffFileStream);

                ProgressHelper.showProgress(i, featuresAdded, 5, LOGGER);
            }

            IOUtils.write("\n@data\n", arffFileStream);

            LOGGER.info("Saving instances:");
            int instanceCounter = 0;
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

                ProgressHelper.showProgress(instanceCounter, instances.size(), 5, LOGGER);
                instanceCounter++;
            }
        } finally {
            IOUtils.closeQuietly(arffFileStream);
        }
        // model.compactify();
        // ArffSaver saver = new ArffSaver();
        // saver.setInstances(model);
        // saver.setFile(targetFile);
        // LOGGER.debug("Saving dataset to: " + targetFile.getAbsoluteFile());
        // saver.writeBatch();
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param feature
     */
    private void handleFeature(final Feature<?> feature, final List<Pair<Integer, String>> newInstance) {
        FeatureDescriptor descriptor = feature.getDescriptor();
        if (feature instanceof AnnotationFeature) {
            AnnotationFeature annotationFeature = (AnnotationFeature)feature;
            for (Annotation annotation : annotationFeature.getValue()) {
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
            AnnotationFeature annotationFeature = (AnnotationFeature)feature;
            handleAnnotationFeature(annotationFeature, newInstance);
        } else if (feature instanceof BooleanFeature) {
            handleBooleanFeature((BooleanFeature)feature, newInstance);
        } else if (feature instanceof NominalFeature) {
            handleNominalFeature((NominalFeature)feature, newInstance);
        } else if (feature instanceof SequentialPatternsFeature) {
            handleSequentialPatterns((SequentialPatternsFeature)feature, newInstance);
        }
    }

    /**
     * <p>
     * Adds all sequential patterns from a {@code SequentialPatternsFeature} to the created Arff file.
     * </p>
     * 
     * @param feature The {@code Feature} to add.
     * @param newInstance The Weka {@code Instance} to add the {@code Feature} to
     * @param model
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

            newInstance.add(new ImmutablePair<Integer, String>(featureTypeIndex, "1.0"));

            // Attribute attribute = model.attribute(featureName);
            // if (attribute == null) {
            // attribute = new Attribute(featureName);
            // model.insertAttributeAt(attribute, model.numAttributes());
            // attribute = model.attribute(featureName);
            // }
            // newInstance.setValue(attribute, 1.0);
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
    private void handleNominalFeature(final NominalFeature feature, final List<Pair<Integer, String>> newInstance) {
        StringBuilder featureTypeBuilder = new StringBuilder("\"" + feature.getName() + "\" {dummy");

        for (String value : feature.getPossibleValues()) {
            featureTypeBuilder.append(",");
            featureTypeBuilder.append(value);
        }
        featureTypeBuilder.append("}");
        String featureType = featureTypeBuilder.toString();

        Integer featureTypeIndex = featureTypes.get(featureType);
        if (featureTypeIndex == null) {
            featureTypeIndex = featuresAdded;
            featureTypes.put(featureType, featureTypeIndex);
            featuresAdded++;
        }

        newInstance.add(new ImmutablePair<Integer, String>(featureTypeIndex, feature.getValue()));
        // Attribute attribute = model.attribute(featureName);
        // if (attribute == null) {
        // FastVector possibleValues = new FastVector(feature.getPossibleValues().length);
        // possibleValues.addElement("dummy");
        // for (String value : feature.getPossibleValues()) {
        // possibleValues.addElement(value);
        // }
        //
        // attribute = new Attribute(featureName, possibleValues);
        // model.insertAttributeAt(attribute, model.numAttributes());
        // attribute = model.attribute(featureName);
        // }
        // newInstance.setValue(attribute, feature.getValue());
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

        newInstance.add(new ImmutablePair<Integer, String>(featureTypeIndex, feature.getValue().toString()));
        // Attribute attribute = model.attribute(feature.getName());
        // if (attribute == null) {
        // FastVector booleanValue = new FastVector(2);
        // booleanValue.addElement("dummy");
        // booleanValue.addElement("true");
        // booleanValue.addElement("false");
        //
        // attribute = new Attribute(feature.getName(), booleanValue);
        //
        // model.insertAttributeAt(attribute, model.numAttributes());
        // attribute = model.attribute(feature.getName());
        // }
        //
        // newInstance.setValue(attribute, feature.getValue().toString());
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
    private void handleAnnotationFeature(AnnotationFeature feature, List<Pair<Integer, String>> newInstance) {
        for (Annotation annotation : feature.getValue()) {
            String featureType = "\"" + annotation.getValue() + "\" numeric";

            Integer featureTypeIndex = featureTypes.get(featureType);
            if (featureTypeIndex == null) {
                featureTypes.put(featureType, featuresAdded);
                featureTypeIndex = featuresAdded;
                featuresAdded++;
            }

            newInstance.add(new ImmutablePair<Integer, String>(featureTypeIndex, "1.0"));

            // Attribute attribute = model.attribute(annotationValue);
            // if (attribute == null) {
            // attribute = new Attribute(annotationValue);
            // model.insertAttributeAt(attribute, model.numAttributes());
            // attribute = model.attribute(annotationValue);
            // }
            // newInstance.setValue(attribute, 1.0);
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

        newInstance.add(new ImmutablePair<Integer, String>(featureTypeIndex, feature.getValue().toString()));

        // Attribute attribute = model.attribute(feature.getName());
        // if (attribute == null) {
        // attribute = new Attribute(feature.getName());
        // model.insertAttributeAt(attribute, model.numAttributes());
        // attribute = model.attribute(feature.getName());
        // }
        // newInstance.setValue(attribute, feature.getValue());
    }

    // private Instances mergeModels(Instances oldModel, Instances newModel) {
    //
    // for (int i = 0; i < newModel.numAttributes(); i++) {
    // Attribute attribute = newModel.attribute(i);
    // Attribute oldModelAttribute = oldModel.attribute(attribute.name());
    // if (oldModelAttribute == null) {
    // oldModel.insertAttributeAt(attribute, oldModel.numAttributes());
    // }
    // }
    // for (int i = 0; i < newModel.numInstances(); i++) {
    // Instance instance = newModel.instance(i);
    // int numAttributesInInstance = instance.numAttributes();
    // Instance mergedInstance = new Instance(numAttributesInInstance);
    // for (int j = 0; j < numAttributesInInstance; j++) {
    // Attribute attribute = instance.attributeSparse(j);
    // Attribute oldModelAttribute = oldModel.attribute(attribute.name());
    // Integer attributeIndex = oldModelAttribute.index();
    // double value = instance.value(attribute);
    // try {
    // mergedInstance.setValueSparse(attributeIndex, value);
    // } catch (ArrayIndexOutOfBoundsException e) {
    // System.out.println();
    // }
    // }
    // mergedInstance.setDataset(oldModel);
    // oldModel.add(mergedInstance);
    // }
    // return oldModel;
    // }

    @Override
    public void processingFinished() {
        // processedDocuments = batchSize;
        try {
            // // checkAndUpdateBatch();
            // saveModel(model);
            saveModel();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
