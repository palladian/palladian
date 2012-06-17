/**
 * Created on: 16.06.2012 19:27:56
 */
package ws.palladian.extraction.feature;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ArffLoader.ArffReader;
import weka.core.converters.ArffSaver;
import ws.palladian.extraction.AbstractPipelineProcessor;
import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.Port;
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
    private final FeatureDescriptor<? extends Feature<?>>[] featureDescriptors;

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
        super(Arrays.asList(new Port<?>[] {new Port<Object>(DEFAULT_INPUT_PORT_IDENTIFIER)}), new ArrayList<Port<?>>());

        Validate.notNull(fileName, "fileName must not be null");
        Validate.notEmpty(featureDescriptors, "featureDescriptors must not be empty");

        this.targetFile = new File(fileName);
        this.featureDescriptors = featureDescriptors;
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        PipelineDocument<Object> document = getDefaultInput();

        FastVector schema = new FastVector();
        Instances model = new Instances("model", schema, 1);
        Instance newInstance = new SparseInstance(0);
        newInstance.setDataset(model);
        for (FeatureDescriptor<? extends Feature<?>> featureDescriptor : featureDescriptors) {
            Feature<?> feature = document.getFeature(featureDescriptor);
            if (feature instanceof NumericFeature) {
                handleNumericFeature((NumericFeature)feature, newInstance, schema);
            } else if (feature instanceof AnnotationFeature) {
                handleAnnotationFeature((AnnotationFeature)feature, newInstance, schema);
            } else if (feature instanceof BooleanFeature) {
                handleBooleanFeature((BooleanFeature)feature, newInstance, schema);
            } else if (feature instanceof NominalFeature) {
                handleNominalFeature((NominalFeature)feature, newInstance, schema);
            } else {
                LOGGER.warn("Unsupported feature type. Ignoring feature: " + featureDescriptor.getIdentifier());
            }
        }
        model.add(newInstance);
        model.compactify();
        try {
            if (targetFile.exists()) {
                FileReader in = new FileReader(targetFile);
                try {
                    BufferedReader reader = new BufferedReader(in);
                    ArffReader arff = new ArffReader(reader);
                    Instances existingData = arff.getData();
                    model = Instances.mergeInstances(existingData, model);
                } finally {
                    IOUtils.closeQuietly(in);
                }
            }
            ArffSaver saver = new ArffSaver();
            saver.setInstances(model);
            saver.setFile(targetFile);
            LOGGER.info("Saving dataset to: " + targetFile.getAbsoluteFile());
            saver.writeBatch();
        } catch (IOException e) {
            throw new DocumentUnprocessableException(e);
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
    private void handleNominalFeature(NominalFeature feature, Instance newInstance, FastVector schema) {
        String annotationValue = feature.getValue();
        Attribute attribute = new Attribute(annotationValue);
        if (schema.indexOf(attribute) == -1) {
            schema.addElement(attribute);
        }
        Integer indexOfAttribute = schema.indexOf(attribute);

        newInstance.setValue(indexOfAttribute, 1.0);
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
    private void handleBooleanFeature(BooleanFeature feature, Instance newInstance, FastVector schema) {
        FastVector booleanValue = new FastVector(2);
        booleanValue.addElement(new Attribute("true"));
        booleanValue.addElement(new Attribute("false"));

        Attribute attribute = new Attribute(feature.getName(), booleanValue);
        schema.addElement(attribute);

        Integer indexOfAttribute = schema.indexOf(attribute);
        newInstance.setValue(indexOfAttribute, feature.getValue().toString());
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
    private void handleAnnotationFeature(AnnotationFeature feature, Instance newInstance, FastVector schema) {
        for (Annotation annotation : feature.getValue()) {
            String annotationValue = annotation.getValue();
            Attribute attribute = new Attribute(annotationValue);
            if (schema.indexOf(attribute) == -1) {
                schema.addElement(attribute);
            }
            Integer indexOfAttribute = schema.indexOf(attribute);

            newInstance.setValue(indexOfAttribute, 1.0);
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
    private void handleNumericFeature(NumericFeature feature, Instance newInstance, FastVector schema) {
        Attribute attribute = new Attribute(feature.getName());
        schema.addElement(attribute);

        Integer indexOfAttribute = schema.indexOf(attribute);
        newInstance.setValue(indexOfAttribute, feature.getValue());
    }
}
// Collection<Item> dataset = persistenceLayer.loadItemsLabeledBy(labeler)
//
// def pipeline = new ProcessingPipeline()
// pipeline.add(new HtmlCleaner())
// pipeline.add(new LowerCaser())
// pipeline.add(new LingPipeTokenizer())
// pipeline.add(new StemmerAnnotator())
//
// def schema = new FastVector()
// def classes = new FastVector()
// classes.addElement("dummy") // Dummy class to make up for wekas inability to store values with index zero in sparse
// vectors. see: http://weka.wikispaces.com/ARFF+%28stable+version%29#Sparse%20ARFF%20files
// classes.addElement("ANSWER")
// classes.addElement("BUMP")
// classes.addElement("DESCRIPTION")
// classes.addElement("ELABORATION")
// classes.addElement("OTHER")
// classes.addElement("QUESTION")
// classes.addElement("REQUEST")
// classes.addElement("AFFIRMATION")
// classes.addElement("THX")
//
// def booleanFeature = new FastVector()
// booleanFeature.addElement("false")
// booleanFeature.addElement("true")
//
// schema.addElement(new Attribute("targetClass",classes))
// schema.addElement(new Attribute("activityFeature"))
// schema.addElement(new Attribute("byThreadStarterFeature",booleanFeature))
// schema.addElement(new Attribute("imperativeRatioFeature"))
// schema.addElement(new Attribute("nounRatioFeature"))
// schema.addElement(new Attribute("onThreadTopicFeature"))
// schema.addElement(new Attribute("postLengthFeature"))
// schema.addElement(new Attribute("questionCountFeature"))
// schema.addElement(new Attribute("ratioOfConsecutiveCapitalLettersFeature"))
// schema.addElement(new Attribute("ratioOfConsecutivePunctuationFeature"))
// schema.addElement(new Attribute("threadPositionFeature"))
// schema.addElement(new Attribute("timelinessFeature"))
// schema.addElement(new Attribute("webLinkingFeature"))
//
// Collection<String> uniqueWords = new HashSet<String>()
// def model = new Instances("languageModel",schema,dataset.size())
//
// File staticFeaturesFile = new File("forumEntryFeatures.csv")
// Map<String,List<Object>> staticFeatures = [:]
// staticFeaturesFile.eachLine { line ->
// def entries = line.split(",")
// staticFeatures["${entries[0]}"] = entries[1..12]
// }
//
//
// def importantWords = []
// sql = Sql.newInstance( "jdbc:mysql://localhost:3306/iirmodel",
// "effingo", "effingo", "com.mysql.jdbc.Driver" )
// sql.eachRow( "CALL `calculateImportantWords`;" ) { importantWords << it.word }
//
//
// dataset.eachWithIndex { item,index ->
// PipelineDocument<String> document = new PipelineDocument<String>(item.getText())
// pipeline.process(document)
//
// Feature<Annotation> token = document.featureVector.get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR)
//
// def newInstance = new SparseInstance(uniqueWords.size()+13)
// newInstance.dataset = model
//
// def staticFeatureVector = staticFeatures["${item.identifier}"]
// if(staticFeatureVector==null) {
// throw new IllegalStateException("No static features for item: "+item.identifier)
// }
//
// // def label = persistenceLayer.loadLabelsForItem(item, labeler).iterator().next()
// // newInstance.setValue(model.attribute(0),label.getType().getName())
// newInstance.setValue(model.attribute(0), staticFeatureVector[0])
// newInstance.setValue(model.attribute(1), Double.valueOf(staticFeatureVector[1]))
// newInstance.setValue(model.attribute(2), staticFeatureVector[2])
// newInstance.setValue(model.attribute(3), Double.valueOf(staticFeatureVector[3]))
// newInstance.setValue(model.attribute(4), Double.valueOf(staticFeatureVector[4]))
// newInstance.setValue(model.attribute(5), Double.valueOf(staticFeatureVector[5]))
// newInstance.setValue(model.attribute(6), Double.valueOf(staticFeatureVector[6]))
// newInstance.setValue(model.attribute(7), Double.valueOf(staticFeatureVector[7]))
// newInstance.setValue(model.attribute(8), Double.valueOf(staticFeatureVector[8]))
// newInstance.setValue(model.attribute(9), Double.valueOf(staticFeatureVector[9]))
// newInstance.setValue(model.attribute(10), Double.valueOf(staticFeatureVector[10]))
// newInstance.setValue(model.attribute(11), Double.valueOf(staticFeatureVector[11]))
// // newInstance.setValue(model.attribute(12), staticFeatureVector[11])
//
// for(Annotation stemmedToken:token.getValue()) {
// String stem = stemmedToken.featureVector.get(StemmerAnnotator.STEM).value
// if(importantWords.contains(stem) && StringUtils.isAlphanumeric(stem)) {
// def stemAttribute = new Attribute(stem)
// if(uniqueWords.add(stem)) {
// schema.addElement(stemAttribute)
// }
// Integer indexOfAttribute = schema.indexOf(stemAttribute)
//
// newInstance.setValue(indexOfAttribute, 1.0);
// }
// }
//
// model.add(newInstance)
// }
