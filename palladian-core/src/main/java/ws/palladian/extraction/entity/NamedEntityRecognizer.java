package ws.palladian.extraction.entity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.extraction.entity.evaluation.EvaluationAnnotation;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode;
import ws.palladian.extraction.feature.TextDocumentPipelineProcessor;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.PositionAnnotationFactory;

/**
 * <p>
 * The abstract Named Entity Recognizer (NER). Every NER should provide functionality for tagging an input text. Some
 * might also be able to be trained on input data.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public abstract class NamedEntityRecognizer extends TextDocumentPipelineProcessor {

    /** The logger for named entity recognizer classes. */
    protected static final Logger LOGGER = LoggerFactory.getLogger(NamedEntityRecognizer.class);

//    public static final FeatureDescriptor<TextAnnotationFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder
//            .build("ws.palladian.processing.entity.ner", TextAnnotationFeature.class);
    
    public static final String PROVIDED_FEATURE = "ws.palladian.processing.entity.ner";

    /** The format in which the text should be tagged. */
    private TaggingFormat taggingFormat = TaggingFormat.XML;

    /** Name of the named entity recognizer. */
    private String name = "unknown";

    /** The loaded model. */
    private Object model;

    /**
     * The file ending of the model file.
     * 
     * @return The file ending of the model/config file.
     */
    public abstract String getModelFileEnding();

    public String getModelFileEndingIfNotSetAutomatically() {
        if (setsModelFileEndingAutomatically()) {
            return "";
        }

        return getModelFileEnding();
    }

    /**
     * Whether or not the NER sets the model file ending itself after specifying the model name.
     * 
     * @return True, if it does, false otherwise.
     */
    public abstract boolean setsModelFileEndingAutomatically();

    /**
     * Whether the NER needs one model file per concept. Usually you can train and recognize several entities using only
     * one model.
     * 
     * @return True, if you need to train each concept separately, false otherwise.
     */
    public boolean oneModelPerConcept() {
        return false;
    }

    public abstract boolean loadModel(String configModelFilePath);
    public abstract Annotations getAnnotations(String inputText, String configModelFilePath);

    public abstract Annotations getAnnotations(String inputText);

    public Annotations getAnnotations(File inputTextFile, String configModelFilePath) {
        String inputText = FileHelper.readFileToString(inputTextFile.getPath());
        return getAnnotations(inputText, configModelFilePath);
    }

    /**
     * <p>
     * Train the named entity recognizer using the data from the training file and save it to the model file path.
     * </p>
     * <p>
     * The training file must be given in tab (\t) separated column format where the first column is the term and the
     * second column is the concept.
     * </p>
     * 
     * @param trainingFilePath The path where the training data can be found.
     * @param modelFilePath The path where the trained model should be saved to.
     * @return True, if the training succeeded, false otherwise.
     */
    public abstract boolean train(String trainingFilePath, String modelFilePath);

    public boolean train(File trainingFile, File modelFile) {
        return train(trainingFile.getPath(), modelFile.getPath());
    }

    public boolean train(Dataset dataset, String modelFilePath) {

        if (dataset.isColumnNER()) {
            return train(dataset.getPath(), modelFilePath);
        }

        String tempFilePath = "data/temp/nerConcatenated.xml";
        String tempColumnFilePath = FileHelper.appendToFileName(tempFilePath, "_tsv");

        // delete temp file that might have been created
        FileHelper.delete(tempFilePath);
        FileHelper.delete(tempColumnFilePath);

        if (!oneModelPerConcept()) {

            // concatenate all xml files from the training index to one large file
            List<String> lines = FileHelper.readFileToArray(dataset.getPath());
            for (String line : lines) {

                String[] parts = line.split(" ");

                FileHelper.concatenateFiles(new File(tempFilePath), new File(dataset.getRootPath() + parts[0]));
            }

            // transform file to tsv format
            FileFormatParser.xmlToColumn(tempFilePath, tempColumnFilePath, "\t");

            return train(tempColumnFilePath, modelFilePath);

        } else {

            boolean trainingComplete = false;

            // map containing the parts with the file links
            Map<String, Set<String>> conceptMap = new HashMap<String, Set<String>>();

            List<String> lines = FileHelper.readFileToArray(dataset.getPath());
            for (String line : lines) {

                if (line.length() == 0) {
                    continue;
                }

                String[] lineParts = line.split(" ");
                String part = lineParts[1].replaceAll("_part(\\d+)", "");

                Set<String> links = conceptMap.get(part);
                if (links == null) {
                    links = new HashSet<String>();
                    links.add(lineParts[0]);
                    conceptMap.put(part, links);
                } else {
                    links.add(lineParts[0]);
                }
            }

            // train x files where x is the number of concepts
            for (Entry<String, Set<String>> partEntry : conceptMap.entrySet()) {

                // concatenate all files for this current concept
                for (String link : partEntry.getValue()) {
                    String[] parts = link.split(" ");

                    FileHelper.concatenateFiles(new File(tempFilePath), new File(dataset.getRootPath() + parts[0]));
                }

                // transform file to tsv format
                FileFormatParser.xmlToColumn(tempFilePath, tempColumnFilePath, "\t");

                trainingComplete = train(tempColumnFilePath,
                        FileHelper.appendToFileName(modelFilePath, "_" + partEntry.getKey().toUpperCase()));

                if (!trainingComplete) {
                    return false;
                }

            }

            return trainingComplete;
        }
    }

    /**
     * Get the tags that were trained for this model.
     * 
     * @param modelPath The path to the model
     * @return A list of tags that the model can apply to text.
     */
    public List<String> getModelTags(String modelPath) {
        String filename = FileHelper.getFileName(modelPath);
        String path = FileHelper.getFilePath(modelPath);
        filename += "_meta.txt";
        return FileHelper.readFileToArray(path + filename);
    }

    /**
     * Tag the input text using the given model or model configuration.
     * 
     * @param inputText The text to be tagged.
     * @param configModelFilePath The file to the model or the configuration depending on the NER. Every NER has it's
     *            own model or configuration file.
     * @return The tagged string in the specified {@link TaggingFormat}.
     */
    public String tag(String inputText, String configModelFilePath) {
        if (configModelFilePath.length() > 0) {
            loadModel(configModelFilePath);
        }
        return tag(inputText);
    }

    public String tag(String inputText) {
        StopWatch stopWatch = new StopWatch();

        Annotations annotations = getAnnotations(inputText);
        String taggedText = tagText(inputText, annotations);

        LOGGER.debug("tagged text in " + stopWatch.getElapsedTimeString(false));

        return taggedText;
    }

    public String tag(File inputFile, File configModelFile) {
        String inputText = FileHelper.readFileToString(inputFile.getPath());
        return tag(inputText, configModelFile.getPath());
    }

    public void tag(String inputText, String outputFilePath, String configModelFilePath) {
        String taggedText = tag(inputText, configModelFilePath);
        FileHelper.writeToFile(outputFilePath, taggedText);
    }

    public void tag(File inputFile, File outputFile, File configModelFile) {
        String inputText = FileHelper.readFileToString(inputFile.getPath());
        tag(inputText, outputFile.getPath(), configModelFile.getPath());
    }

    protected String tagText(String inputText, Annotations annotations) {
        return tagText(inputText, annotations, getTaggingFormat());
    }

    protected String tagText(String inputText, Annotations annotations, TaggingFormat format) {

        StringBuilder taggedText = new StringBuilder();

        int lastEndIndex = 0;

        // we need to sort in ascending order first
        annotations.sort();

        Annotation lastAnnotation = null;
        for (Annotation annotation : annotations) {

            // ignore nested annotations
            if (annotation.getOffset() < lastEndIndex) {
                continue;
            }

            String tagName = annotation.getMostLikelyTagName();

            taggedText.append(inputText.substring(lastEndIndex, annotation.getOffset()));

            String correctText = inputText.substring(annotation.getOffset(), annotation.getEndIndex());

            if (!correctText.equalsIgnoreCase(annotation.getEntity()) && correctText.indexOf("\n") == -1) {
                StringBuilder errorString = new StringBuilder();
                errorString.append("alignment error, the annotation candidates don't match the text:\n");
                errorString.append("found: " + correctText + "\n");
                errorString.append("instead of: " + annotation.getEntity() + "(" + annotation + ")\n");
                errorString.append("last annotation: " + lastAnnotation);
                //System.exit(1);
                throw new IllegalStateException(errorString.toString());
            }

            if (format == TaggingFormat.XML) {

                taggedText.append("<").append(tagName).append(">");
                taggedText.append(annotation.getEntity());
                taggedText.append("</").append(tagName).append(">");

            } else if (format == TaggingFormat.BRACKETS) {

                taggedText.append("[").append(tagName).append(" ");
                taggedText.append(annotation.getEntity());
                taggedText.append(" ]");

            } else if (format == TaggingFormat.SLASHES) {

                List<String> tokens = Tokenizer.tokenize(annotation.getEntity());
                for (String token : tokens) {
                    taggedText.append(token).append("/").append(tagName).append(" ");
                }

            }

            lastEndIndex = annotation.getEndIndex();
            lastAnnotation = annotation;
        }

        taggedText.append(inputText.substring(lastEndIndex));

        return taggedText.toString();

    }

    /**
     * Evaluate the NER, the model must have been loaded before.
     * 
     * @param dataset The dataset to use for evaluation.
     * @return The evaluation results.
     */
    public EvaluationResult evaluate(Dataset dataset) {
        return evaluate(dataset, "");
    }
    public EvaluationResult evaluate(Dataset dataset, String configModelFilePath) {

        String tempFilePath = "data/temp/nerConcatenatedEvaluation.xml";

        // delete temp file that might have been created
        FileHelper.delete(tempFilePath);

        // concatenate all xml files from the training index to one large file
        List<String> lines = FileHelper.readFileToArray(dataset.getPath());
        for (String line : lines) {

            String[] parts = line.split(" ");

            FileHelper.concatenateFiles(new File(tempFilePath), new File(dataset.getRootPath() + parts[0]));
        }

        return evaluate(tempFilePath, configModelFilePath, TaggingFormat.XML);
    }

    public EvaluationResult evaluate(String testingFilePath, String configModelFilePath, TaggingFormat format) {
        return evaluate(testingFilePath, configModelFilePath, format, new Annotations());
    }
    public EvaluationResult evaluate(String testingFilePath, TaggingFormat format) {
        return evaluate(testingFilePath, "", format, new Annotations());
    }
    public EvaluationResult evaluate(String testingFilePath, String configModelFilePath, TaggingFormat format,
            Annotations ignoreAnnotations) {

        // get the correct annotations from the testing file
        Annotations goldStandard = FileFormatParser.getAnnotations(testingFilePath, format);
        goldStandard.transformToEvaluationAnnotations();
        goldStandard.sort();
        goldStandard.save(FileHelper.getFilePath(testingFilePath) + "goldStandard.txt");

        // get the annotations of the NER
        Annotations nerAnnotations = null;
        if (configModelFilePath.length() > 0) {
            nerAnnotations = getAnnotations(FileFormatParser.getText(testingFilePath, format), configModelFilePath);
        } else {
            nerAnnotations = getAnnotations(FileFormatParser.getText(testingFilePath, format));
        }

        nerAnnotations.removeNestedAnnotations();
        nerAnnotations.sort();
        String inputFile = FileHelper.getFileName(testingFilePath);
        nerAnnotations.save(FileHelper.getFilePath(testingFilePath) + "nerResult_" + inputFile + "_"
                + getName().replace(" ", "")
                + DateHelper.getCurrentDatetime() + ".txt");

        // see EvaluationResult for explanation of that field
        Map<String, CountMap<String>> assignments = new HashMap<String, CountMap<String>>();

        // create count maps for each possible tag (for gold standard and annotation because both could have different
        // tags)
        for (Annotation goldStandardAnnotation : goldStandard) {
            String tagName = goldStandardAnnotation.getTargetClass();
            if (assignments.get(tagName) == null) {
                assignments.put(tagName, CountMap.<String>create());
            }
            assignments.get(tagName).add(EvaluationResult.POSSIBLE);
        }
        for (Annotation nerAnnotation : nerAnnotations) {
            String tagName = nerAnnotation.getMostLikelyTagName();
            if (assignments.get(tagName) == null) {
                assignments.put(tagName, CountMap.<String>create());
            }
        }

        // error map of annotations to precisely show which errors were made
        Map<String, Annotations> annotationsErrors = new HashMap<String, Annotations>();
        annotationsErrors.put(EvaluationResult.CORRECT, new Annotations());
        annotationsErrors.put(EvaluationResult.ERROR1, new Annotations());
        annotationsErrors.put(EvaluationResult.ERROR2, new Annotations());
        annotationsErrors.put(EvaluationResult.ERROR3, new Annotations());
        annotationsErrors.put(EvaluationResult.ERROR4, new Annotations());
        annotationsErrors.put(EvaluationResult.ERROR5, new Annotations());

        Set<Integer> ignoreAnnotationSet = new HashSet<Integer>();
        for (Annotation annotation : ignoreAnnotations) {
            ignoreAnnotationSet.add(annotation.getEntity().hashCode());
        }

        // check each NER annotation against the gold standard and add it to the assignment map depending on its error
        // type, we allow only one overlap for each gold standard annotation => real(<Person>Homer J. Simpson</Person>),
        // tagged(<Person>Homer</Person> J. <Person>Simpson</Person>) => tagged(<Person>Homer</Person> J. Simpson)
        // otherwise we get problems with calculating MUC precision and recall scores
        for (Annotation nerAnnotation : nerAnnotations) {

            // skip "O" tags, XXX should this really be done here in this method?
            if (nerAnnotation.getMostLikelyTagName().equalsIgnoreCase("o")) {
                continue;
            }

            boolean taggedOverlap = false;

            int counter = 0;
            for (Annotation goldStandardAnnotation : goldStandard) {

                counter++;

                // skip ignored annotations for error cases 2,3,4, and 5, however, leave the possibility for error 1
                // (tagged something that should not have been tagged)
                if (ignoreAnnotationSet.contains(goldStandardAnnotation.getEntity().hashCode())
                        && !(nerAnnotation.getOffset() < goldStandardAnnotation.getEndIndex() && !taggedOverlap)) {
                    continue;
                }
                // check whether annotation has been tagged already, if so, just skip the ner annotation
                // if (((EvaluationAnnotation) goldStandardAnnotation).isTagged()) {
                // continue;
                // }

                if (nerAnnotation.matches(goldStandardAnnotation)) {

                    // exact match
                    if (nerAnnotation.sameTag((EvaluationAnnotation) goldStandardAnnotation)) {

                        // correct tag (no error)
                        assignments.get(nerAnnotation.getMostLikelyTagName()).add(EvaluationResult.CORRECT);
                        annotationsErrors.get(EvaluationResult.CORRECT).add(nerAnnotation);

                        // in confusion matrix real = tagged
                        assignments.get(nerAnnotation.getMostLikelyTagName()).add(
                                goldStandardAnnotation.getTargetClass());

                        ((EvaluationAnnotation) goldStandardAnnotation).setTagged(true);

                        break;

                    } else {

                        // wrong tag (error3)
                        assignments.get(goldStandardAnnotation.getTargetClass()).add(
                                EvaluationResult.ERROR3);
                        annotationsErrors.get(EvaluationResult.ERROR3).add(nerAnnotation);

                        // in confusion matrix real != tagged
                        assignments.get(nerAnnotation.getMostLikelyTagName()).add(
                                goldStandardAnnotation.getTargetClass());

                        ((EvaluationAnnotation) goldStandardAnnotation).setTagged(true);

                        break;

                    }

                } else if (nerAnnotation.overlaps(goldStandardAnnotation)) {

                    // overlaps
                    if (nerAnnotation.sameTag((EvaluationAnnotation) goldStandardAnnotation)) {

                        // correct tag (error4)
                        assignments.get(nerAnnotation.getMostLikelyTagName()).add(EvaluationResult.ERROR4);
                        annotationsErrors.get(EvaluationResult.ERROR4).add(nerAnnotation);

                        // in confusion matrix real = tagged
                        assignments.get(nerAnnotation.getMostLikelyTagName()).add(
                                goldStandardAnnotation.getTargetClass());

                        ((EvaluationAnnotation) goldStandardAnnotation).setTagged(true);

                        // break;

                    } else {

                        // wrong tag (error5)
                        assignments.get(nerAnnotation.getMostLikelyTagName()).add(EvaluationResult.ERROR5);
                        annotationsErrors.get(EvaluationResult.ERROR5).add(nerAnnotation);

                        // in confusion matrix real != tagged
                        assignments.get(nerAnnotation.getMostLikelyTagName()).add(
                                goldStandardAnnotation.getTargetClass());

                        ((EvaluationAnnotation) goldStandardAnnotation).setTagged(true);

                        // break;

                    }

                    taggedOverlap = true;

                } else if (nerAnnotation.getOffset() < goldStandardAnnotation.getEndIndex()
                        || counter == goldStandard.size()) {

                    if (!taggedOverlap) {

                        // if (ignoreAnnotations.containsAnnotationWithEntity(goldStandardAnnotation)) {
                        // System.out.println("here");
                        // }

                        // tagged something that should not have been tagged (error1)
                        assignments.get(nerAnnotation.getMostLikelyTagName()).add(EvaluationResult.ERROR1);
                        annotationsErrors.get(EvaluationResult.ERROR1).add(nerAnnotation);

                        // in confusion matrix add count to "other" since NER tagged something that should not have been
                        // tagged
                        assignments.get(nerAnnotation.getMostLikelyTagName()).add(
                                EvaluationResult.SPECIAL_MARKER + "OTHER" + EvaluationResult.SPECIAL_MARKER);
                    }

                    break;
                }

                // break if there is no chance that any upcoming gold standard annotation might match this ner
                // annotation
                // if (nerAnnotation.getEndIndex() < goldStandardAnnotation.getOffset()) {
                // break;
                // }

            }

        }

        // check which gold standard annotations have not been found by the NER (error2)
        for (Annotation goldStandardAnnotation : goldStandard) {
            if (!((EvaluationAnnotation) goldStandardAnnotation).isTagged()) {
                assignments.get(goldStandardAnnotation.getTargetClass()).add(EvaluationResult.ERROR2);
                annotationsErrors.get(EvaluationResult.ERROR2).add(goldStandardAnnotation);
            }
        }

        EvaluationResult evaluationResult = new EvaluationResult(assignments, goldStandard, annotationsErrors);

        printEvaluationDetails(evaluationResult, annotationsErrors, FileHelper.getFilePath(testingFilePath)
                + DateHelper.getCurrentDatetime() + "_results_" + inputFile + "_" + getName().replace(" ", "") + ".csv");

        return evaluationResult;
    }


    public static StringBuilder printEvaluationDetails(EvaluationResult evaluationResult) {
        return printEvaluationDetails(evaluationResult, new HashMap<String, Annotations>(), null);
    }
    public static StringBuilder printEvaluationDetails(EvaluationResult evaluationResult,
            Map<String, Annotations> annotationErrors, String targetPath) {

        // write evaluation results to file
        StringBuilder results = new StringBuilder();

        results.append("Number of distinct tags:; ").append(evaluationResult.getAssignments().size()).append("\n");
        results.append("Total annotations in test set:; ").append(evaluationResult.getGoldStandardAnnotations().size())
        .append("\n");
        results.append("Confusion Matrix:\n");

        results.append("predicted\\real;");

        // order of tag names for matrix
        List<String> tagOrder = new ArrayList<String>();
        for (String tagName : evaluationResult.getAssignments().keySet()) {
            if (!(tagName.startsWith(EvaluationResult.SPECIAL_MARKER) && tagName
                    .endsWith(EvaluationResult.SPECIAL_MARKER))) {
                tagOrder.add(tagName);
                results.append(tagName).append(";");
            }
        }
        // add "OTHER" in case of ERROR1
        tagOrder.add(EvaluationResult.SPECIAL_MARKER + "OTHER" + EvaluationResult.SPECIAL_MARKER);
        results.append(EvaluationResult.SPECIAL_MARKER + "OTHER" + EvaluationResult.SPECIAL_MARKER).append(";");

        results.append("#total number;Exact Match Precision;Exact Match Recall;Exact Match F1;MUC Precision;MUC Recall;MUC F1\n");

        int totalTagAssignments = 0;
        for (Entry<String, CountMap<String>> tagEntry : evaluationResult.getAssignments().entrySet()) {

            CountMap<String> cm = tagEntry.getValue();

            int totalNumber = 0;

            results.append(tagEntry.getKey()).append(";");

            // write frequencies of confusion matrix
            for (String tagName : tagOrder) {
                results.append(cm.getCount(tagName)).append(";");
                totalNumber += cm.getCount(tagName);
            }

            // total number of real tags in test set
            results.append(totalNumber).append(";");
            totalTagAssignments += totalNumber;

            // precision, recall, and F1 for exact match
            results.append(evaluationResult.getPrecisionFor(tagEntry.getKey(), EvaluationMode.EXACT_MATCH)).append(
            ";");
            results.append(evaluationResult.getRecallFor(tagEntry.getKey(), EvaluationMode.EXACT_MATCH)).append(";");
            results.append(evaluationResult.getF1For(tagEntry.getKey(), EvaluationMode.EXACT_MATCH)).append(";");

            // precision, recall, and F1 for MUC score
            results.append(evaluationResult.getPrecisionFor(tagEntry.getKey(), EvaluationMode.MUC)).append(";");
            results.append(evaluationResult.getRecallFor(tagEntry.getKey(), EvaluationMode.MUC)).append(";");
            results.append(evaluationResult.getF1For(tagEntry.getKey(), EvaluationMode.MUC)).append("\n");

        }

        // write last line with averages over all tags
        results.append("ALL TAGS;");
        for (String tagName : tagOrder) {
            int totalAssignments = 0;
            for (CountMap<String> countMap : evaluationResult.getAssignments().values()) {
                totalAssignments += countMap.getCount(tagName);
            }
            results.append(totalAssignments).append(";");
        }

        // total assignments
        results.append(totalTagAssignments).append(";");

        // precision, recall, and F1 for exact match
        results.append("tag averaged:")
                .append(MathHelper.round(evaluationResult.getTagAveragedPrecision(EvaluationMode.EXACT_MATCH), 4))
        .append(", overall:");
        results.append(MathHelper.round(evaluationResult.getPrecision(EvaluationMode.EXACT_MATCH), 4)).append(";");
        results.append("tag averaged:")
                .append(MathHelper.round(evaluationResult.getTagAveragedRecall(EvaluationMode.EXACT_MATCH), 4))
        .append(", overall:");
        results.append(MathHelper.round(evaluationResult.getRecall(EvaluationMode.EXACT_MATCH), 4)).append(";");
        results.append("tag averaged:")
                .append(MathHelper.round(evaluationResult.getTagAveragedF1(EvaluationMode.EXACT_MATCH), 4))
        .append(", overall:");
        results.append(MathHelper.round(evaluationResult.getF1(EvaluationMode.EXACT_MATCH), 4)).append(";");

        // precision, recall, and F1 for MUC score
        results.append("tag averaged:")
                .append(MathHelper.round(evaluationResult.getTagAveragedPrecision(EvaluationMode.MUC), 4))
        .append(", overall:");
        results.append(MathHelper.round(evaluationResult.getPrecision(EvaluationMode.MUC), 4)).append(";");
        results.append("tag averaged:")
                .append(MathHelper.round(evaluationResult.getTagAveragedRecall(EvaluationMode.MUC), 4))
        .append(", overall:");
        results.append(MathHelper.round(evaluationResult.getRecall(EvaluationMode.MUC), 4)).append(";");
        results.append("tag averaged:")
                .append(MathHelper.round(evaluationResult.getTagAveragedF1(EvaluationMode.MUC), 4))
        .append(", overall:");
        results.append(MathHelper.round(evaluationResult.getF1(EvaluationMode.MUC), 4)).append("\n");

        Map<String, String> errorTypes = new TreeMap<String, String>();
        errorTypes.put(EvaluationResult.ERROR1, "ERROR 1: Completely Incorrect Annotations");
        errorTypes.put(EvaluationResult.ERROR2, "ERROR 2: Missed Annotations");
        errorTypes.put(EvaluationResult.ERROR3, "ERROR 3: Correct Boundaries, Wrong Tag");
        errorTypes.put(EvaluationResult.ERROR4, "ERROR 4: Wrong Boundaries, Correct Tag");
        errorTypes.put(EvaluationResult.ERROR5, "ERROR 5: Wrong Boundaries, Wrong Tag");

        results.append("\n\n");
        results.append("CORRECT:");
        results.append(" : ").append(annotationErrors.get(EvaluationResult.CORRECT).size()).append("\n");
        for (Entry<String, String> errorTypeEntry : errorTypes.entrySet()) {
            results.append(errorTypeEntry.getValue());
            results.append(" : ").append(annotationErrors.get(errorTypeEntry.getKey()).size()).append("\n");
        }

        for (Entry<String, String> errorTypeEntry : errorTypes.entrySet()) {
            results.append("\n\n");
            results.append(errorTypeEntry.getValue());
            results.append(" (total: ").append(annotationErrors.get(errorTypeEntry.getKey()).size()).append("):\n\n");

            CountMap<String> cm = getAnnotationCountForTag(annotationErrors.get(errorTypeEntry.getKey()));
            for (String item : cm) {
                results.append(item).append(":; ").append(cm.getCount(item)).append("\n");
            }
            results.append("\n");
            for (Annotation annotation : annotationErrors.get(errorTypeEntry.getKey())) {
                results.append("  ").append(annotation).append("\n");
            }
        }

        if (targetPath != null) {
            FileHelper.writeToFile(targetPath, results);
        }

        return results;
    }

    private static CountMap<String> getAnnotationCountForTag(Annotations annotations) {
        CountMap<String> cm = CountMap.create();
        for (Annotation annotation : annotations) {
            if (annotation instanceof EvaluationAnnotation) {
                cm.add(annotation.getTargetClass());
            } else {
                cm.add(annotation.getMostLikelyTagName());
            }
        }
        return cm;
    }

    /**
     * The output of the named entity recognition is not well formatted and we need to align it with the input data.
     * 
     * @param file The file where the prediction output is written in BIO format. This file will be overwritten.
     */
    protected void alignContent(File alignFile, String correctContent) {
        alignContent(alignFile.getPath(), correctContent);
    }

    protected void alignContent(String alignFilePath, String correctContent) {
        // transform to XML
        FileFormatParser.columnToXml(alignFilePath, alignFilePath, "\t");

        String alignedContent = FileHelper.readFileToString(alignFilePath);

        // compare contents, ignore tags and align content with inputText (correctContent)
        // the index for the aligned context is different because of the tags
        int alignIndex = 0;
        boolean jumpOne = false;
        for (int i = 0; i < correctContent.length(); i++, alignIndex++) {
            Character correctCharacter = correctContent.charAt(i);
            Character alignedCharacter = alignedContent.charAt(alignIndex);
            Character nextAlignedCharacter = 0;
            if (i < correctContent.length() - 1) {
                nextAlignedCharacter = alignedContent.charAt(alignIndex + 1);
            }

            // if same, continue
            if (correctCharacter.equals(alignedCharacter)) {
                continue;
            }

            // don't distinguish between " and '
            if ((correctCharacter.charValue() == 34 || correctCharacter.charValue() == 39)
                    && (alignedCharacter.charValue() == 34 || alignedCharacter.charValue() == 39)) {
                continue;
            }

            // characters are different

            // if tag "<" skip it
            if (alignedCharacter.charValue() == 60
                    && (!Character.isWhitespace(correctCharacter) || nextAlignedCharacter.charValue() == 47 || jumpOne)) {
                do {
                    alignIndex++;
                    alignedCharacter = alignedContent.charAt(alignIndex);
                } while (alignedCharacter.charValue() != 62);

                if (jumpOne) {
                    alignIndex++;
                    jumpOne = false;
                }
                alignedCharacter = alignedContent.charAt(++alignIndex);

                if (alignedCharacter.charValue() == 60) {
                    do {
                        alignIndex++;
                        alignedCharacter = alignedContent.charAt(alignIndex);
                    } while (alignedCharacter.charValue() != 62);
                    alignedCharacter = alignedContent.charAt(++alignIndex);
                }

                nextAlignedCharacter = alignedContent.charAt(alignIndex + 1);

                // check again if the characters are the same
                if (correctCharacter.equals(alignedCharacter)) {
                    continue;
                }
            }

            if (correctCharacter.charValue() == 10) {
                alignedContent = alignedContent.substring(0, alignIndex) + "\n"
                + alignedContent.substring(alignIndex, alignedContent.length());
                // alignIndex--;
            } else
                if (Character.isWhitespace(alignedCharacter)) {

                    alignedContent = alignedContent.substring(0, alignIndex)
                    + alignedContent.substring(alignIndex + 1, alignedContent.length());
                    if (nextAlignedCharacter.charValue() == 60) {
                        alignIndex--;
                        jumpOne = true;
                    } else {
                        jumpOne = false;
                    }

                } else {
                    alignedContent = alignedContent.substring(0, alignIndex) + " "
                    + alignedContent.substring(alignIndex, alignedContent.length());
                }

            FileHelper.writeToFile(alignFilePath, alignedContent);
        }

        FileHelper.writeToFile(alignFilePath, alignedContent);
    }
    
    @Override
    public void processDocument(TextDocument document)
    		throws DocumentUnprocessableException {
    	String content = document.getContent();
    	// TODO merge annotation classes
    	Annotations annotations = getAnnotations(content);
    	
    	FeatureVector featureVector = document.getFeatureVector();
    	
    	PositionAnnotationFactory annotationFactory = new PositionAnnotationFactory(PROVIDED_FEATURE, document);
    	for(Annotation nerAnnotation:annotations) {
    		PositionAnnotation procAnnotation = annotationFactory.create(nerAnnotation.getOffset(), nerAnnotation.getEndIndex());
    		featureVector.add(procAnnotation);
    		
    	}
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setTaggingFormat(TaggingFormat taggingFormat) {
        this.taggingFormat = taggingFormat;
    }

    public TaggingFormat getTaggingFormat() {
        return taggingFormat;
    }

    public void setModel(Object model) {
        this.model = model;
    }

    public Object getModel() {
        return model;
    }

}