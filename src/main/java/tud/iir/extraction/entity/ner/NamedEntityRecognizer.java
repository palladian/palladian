package tud.iir.extraction.entity.ner;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


import org.apache.log4j.Logger;


import tud.iir.extraction.entity.ner.evaluation.EvaluationAnnotation;
import tud.iir.extraction.entity.ner.evaluation.EvaluationResult;
import tud.iir.helper.CountMap;
import tud.iir.helper.FileHelper;
import tud.iir.helper.MathHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.Tokenizer;

/**
 * <p>
 * The abstract Named Entity Recognizer (NER). Every NER should provide functionality for tagging an input text. Some
 * might also be able to be trained on input data.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public abstract class NamedEntityRecognizer {

    /** The logger for named entity recognizer classes. */
    protected static final Logger LOGGER = Logger.getLogger(NamedEntityRecognizer.class);

    /** The format in which the text should be tagged. */
    private TaggingFormat taggingFormat = TaggingFormat.XML;

    /** Name of the named entity recognizer. */
    private String name = "unknown";

    public abstract Annotations getAnnotations(String inputText, String configModelFilePath);

    public Annotations getAnnotations(File inputTextFile, String configModelFilePath) {
        String inputText = FileHelper.readFileToString(inputTextFile.getPath());
        return getAnnotations(inputText, configModelFilePath);
    }

    /**
     * <p>
     * Train the named entity recognizer using the data from the training file and save it to the model file path.
     * </p>
     * <p>
     * The training file must be given in tab separated column format where the first column is the term and the second
     * column is the concept.
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

    /**
     * Tag the input text using the given model or model configuration.
     * 
     * @param inputText The text to be tagged.
     * @param configModelFilePath The file to the model or the configuration depending on the NER. Every NER has it's
     *            own model or configuration file.
     * @return The tagged string in the specified {@link TaggingFormat}.
     */
    public String tag(String inputText, String configModelFilePath) {
        StopWatch stopWatch = new StopWatch();

        Annotations annotations = getAnnotations(inputText, configModelFilePath);
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

        for (Annotation annotation : annotations) {

            // ignore nested annotations
            if (annotation.getOffset() < lastEndIndex) {
                continue;
            }

            String tagName = annotation.getMostLikelyTag().getCategory().getName();

            taggedText.append(inputText.substring(lastEndIndex, annotation.getOffset()));

            if (format == TaggingFormat.XML) {

                taggedText.append("<").append(tagName).append(">");
                taggedText.append(annotation.getEntity().getName());
                taggedText.append("</").append(tagName).append(">");

            } else if (format == TaggingFormat.BRACKETS) {

                taggedText.append("[").append(tagName).append(" ");
                taggedText.append(annotation.getEntity().getName());
                taggedText.append(" ]");

            } else if (format == TaggingFormat.SLASHES) {

                List<String> tokens = Tokenizer.tokenize(annotation.getEntity().getName());
                for (String token : tokens) {
                    taggedText.append(token).append("/").append(tagName).append(" ");
                }

            }

            lastEndIndex = annotation.getEndIndex();
        }

        taggedText.append(inputText.substring(lastEndIndex));

        return taggedText.toString();

    }

    public EvaluationResult evaluate(String testingFilePath, String configModelFilePath, TaggingFormat format) {

        // get the correct annotations from the testing file
        Annotations goldStandard = FileFormatParser.getAnnotations(testingFilePath, format);
        goldStandard.transformToEvaluationAnnotations();
        goldStandard.sort();
        goldStandard.save(FileHelper.getFilePath(testingFilePath) + "goldStandard.txt");

        // get the annotations of the NER
        Annotations nerAnnotations = getAnnotations(FileFormatParser.getText(testingFilePath, format),
                configModelFilePath);
        nerAnnotations.sort();
        nerAnnotations.save(FileHelper.getFilePath(testingFilePath) + "nerResult.txt");

        // see EvaluationResult for explanation of that field
        Map<String, CountMap> assignments = new HashMap<String, CountMap>();

        // create count maps for each possible tag (for gold standard and annotation because both could have different
        // tags)
        for (Annotation goldStandardAnnotation : goldStandard) {
            String tagName = goldStandardAnnotation.getMostLikelyTagName();
            if (assignments.get(tagName) == null) {
                CountMap cm = new CountMap();
                assignments.put(tagName, cm);
            }
            assignments.get(tagName).increment(EvaluationResult.POSSIBLE);
        }
        for (Annotation nerAnnotation : nerAnnotations) {
            String tagName = nerAnnotation.getMostLikelyTagName();
            if (assignments.get(tagName) == null) {
                CountMap cm = new CountMap();
                assignments.put(tagName, cm);
            }
        }

        // check each NER annotation against the gold standard and add it to the assignment map depending on its error
        // type, we allow only one overlap for each gold standard annotation => real(<Person>Homer J. Simpson</Person>),
        // tagged(<Person>Homer</Person> J. <Person>Simpson</Person>) => tagged(<Person>Homer</Person> J. Simpson)
        // otherwise we get problems with calculating MUC precision and recall scores
        for (Annotation nerAnnotation : nerAnnotations) {

            for (Annotation goldStandardAnnotation : goldStandard) {

                if (nerAnnotation.matches(goldStandardAnnotation)) {

                    // exact match
                    if (nerAnnotation.sameTag(goldStandardAnnotation)) {

                        // check whether annotation has been tagged already, if so, just skip the ner annotation
                        if (!((EvaluationAnnotation) goldStandardAnnotation).isTagged()) {

                            // correct tag (no error)
                            assignments.get(nerAnnotation.getMostLikelyTagName()).increment(EvaluationResult.CORRECT);

                            // in confusion matrix real = tagged
                            assignments.get(nerAnnotation.getMostLikelyTagName()).increment(
                                    goldStandardAnnotation.getMostLikelyTagName());

                            ((EvaluationAnnotation) goldStandardAnnotation).setTagged(true);

                        }
                        break;

                    } else {

                        // check whether annotation has been tagged already, if so, just skip the ner annotation
                        if (!((EvaluationAnnotation) goldStandardAnnotation).isTagged()) {

                            // wrong tag (error3)
                            assignments.get(goldStandardAnnotation.getMostLikelyTagName()).increment(EvaluationResult.ERROR3);

                            // ner tagged something that belongs to other tag
                            assignments.get(nerAnnotation.getMostLikelyTagName()).increment(EvaluationResult.ERROR1);

                            // in confusion matrix real != tagged
                            assignments.get(nerAnnotation.getMostLikelyTagName()).increment(
                                    goldStandardAnnotation.getMostLikelyTagName());

                            ((EvaluationAnnotation) goldStandardAnnotation).setTagged(true);

                        }

                        break;

                    }

                } else if (nerAnnotation.overlaps(goldStandardAnnotation)) {

                    // overlaps
                    if (nerAnnotation.sameTag(goldStandardAnnotation)) {

                        // check whether annotation has been tagged already, if so, just skip the ner annotation
                        if (!((EvaluationAnnotation) goldStandardAnnotation).isTagged()) {

                            // correct tag (error4)
                            assignments.get(nerAnnotation.getMostLikelyTagName()).increment(EvaluationResult.ERROR4);

                            // in confusion matrix real = tagged
                            assignments.get(nerAnnotation.getMostLikelyTagName()).increment(
                                    goldStandardAnnotation.getMostLikelyTagName());

                            ((EvaluationAnnotation) goldStandardAnnotation).setTagged(true);

                        }

                        break;

                    } else {

                        // check whether annotation has been tagged already, if so, just skip the ner annotation
                        if (!((EvaluationAnnotation) goldStandardAnnotation).isTagged()) {

                            // wrong tag (error5)
                            assignments.get(nerAnnotation.getMostLikelyTagName()).increment(EvaluationResult.ERROR5);

                            // in confusion matrix real != tagged
                            assignments.get(nerAnnotation.getMostLikelyTagName()).increment(
                                    goldStandardAnnotation.getMostLikelyTagName());

                            ((EvaluationAnnotation) goldStandardAnnotation).setTagged(true);

                        }

                        break;

                    }

                } else if (nerAnnotation.getOffset() < goldStandardAnnotation.getEndIndex()) {

                    // tagged something that should not have been tagged (error1)
                    assignments.get(nerAnnotation.getMostLikelyTagName()).increment(EvaluationResult.ERROR1);

                    // in confusion matrix add count to "other" since NER tagged something that should not have been
                    // tagged
                    assignments.get(nerAnnotation.getMostLikelyTagName()).increment(
                            EvaluationResult.SPECIAL_MARKER + "OTHER" + EvaluationResult.SPECIAL_MARKER);

                    break;
                }

            }

        }

        // check which gold standard annotations have not been found by the NER (error2)
        for (Annotation goldStandardAnnotation : goldStandard) {
            if (!((EvaluationAnnotation) goldStandardAnnotation).isTagged()) {
                assignments.get(goldStandardAnnotation.getMostLikelyTagName()).increment(EvaluationResult.ERROR2);
            }
        }

        EvaluationResult evaluationResult = new EvaluationResult(assignments);

        printEvaluationDetails(evaluationResult, FileHelper.getFilePath(testingFilePath) + "results.csv");

        return evaluationResult;
    }

    private void printEvaluationDetails(EvaluationResult evaluationResult, String targetPath) {

        // write evaluation results to file
        StringBuilder results = new StringBuilder();

        results.append("Number of tags: ").append(evaluationResult.getAssignments().size()).append("\n");
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
        for (Entry<String, CountMap> tagEntry : evaluationResult.getAssignments().entrySet()) {

            CountMap cm = tagEntry.getValue();

            int totalNumber = 0;

            results.append(tagEntry.getKey()).append(";");

            // write frequencies of confusion matrix
            for (String tagName : tagOrder) {
                results.append(cm.get(tagName)).append(";");
                totalNumber += cm.get(tagName);
            }

            // total number of real tags in test set
            results.append(totalNumber).append(";");
            totalTagAssignments += totalNumber;

            // precision, recall, and F1 for exact match
            results.append(evaluationResult.getPrecisionFor(tagEntry.getKey(), EvaluationResult.EXACT_MATCH)).append(
                    ";");
            results.append(evaluationResult.getRecallFor(tagEntry.getKey(), EvaluationResult.EXACT_MATCH)).append(";");
            results.append(evaluationResult.getF1For(tagEntry.getKey(), EvaluationResult.EXACT_MATCH)).append(";");

            // precision, recall, and F1 for MUC score
            results.append(evaluationResult.getPrecisionFor(tagEntry.getKey(), EvaluationResult.MUC)).append(";");
            results.append(evaluationResult.getRecallFor(tagEntry.getKey(), EvaluationResult.MUC)).append(";");
            results.append(evaluationResult.getF1For(tagEntry.getKey(), EvaluationResult.MUC)).append("\n");

        }

        // write last line with averages over all tags
        results.append("ALL TAGS;");
        for (String tagName : tagOrder) {
            int totalAssignments = 0;
            for (CountMap countMap : evaluationResult.getAssignments().values()) {
                totalAssignments += countMap.get(tagName);
            }
            results.append(totalAssignments).append(";");
        }

        // total assignments
        results.append(totalTagAssignments).append(";");

        // precision, recall, and F1 for exact match
        results.append("tag averaged:")
                .append(MathHelper.round(evaluationResult.getTagAveragedPrecision(EvaluationResult.EXACT_MATCH), 4))
                .append(", overall:");
        results.append(MathHelper.round(evaluationResult.getPrecision(EvaluationResult.EXACT_MATCH), 4)).append(";");
        results.append("tag averaged:")
                .append(MathHelper.round(evaluationResult.getTagAveragedRecall(EvaluationResult.EXACT_MATCH), 4))
                .append(", overall:");
        results.append(MathHelper.round(evaluationResult.getRecall(EvaluationResult.EXACT_MATCH), 4)).append(";");
        results.append("tag averaged:")
                .append(MathHelper.round(evaluationResult.getTagAveragedF1(EvaluationResult.EXACT_MATCH), 4))
                .append(", overall:");
        results.append(MathHelper.round(evaluationResult.getF1(EvaluationResult.EXACT_MATCH), 4)).append(";");

        // precision, recall, and F1 for MUC score
        results.append("tag averaged:")
                .append(MathHelper.round(evaluationResult.getTagAveragedPrecision(EvaluationResult.MUC), 4))
                .append(", overall:");
        results.append(MathHelper.round(evaluationResult.getPrecision(EvaluationResult.MUC), 4)).append(";");
        results.append("tag averaged:")
                .append(MathHelper.round(evaluationResult.getTagAveragedRecall(EvaluationResult.MUC), 4))
                .append(", overall:");
        results.append(MathHelper.round(evaluationResult.getRecall(EvaluationResult.MUC), 4)).append(";");
        results.append("tag averaged:")
                .append(MathHelper.round(evaluationResult.getTagAveragedF1(EvaluationResult.MUC), 4))
                .append(", overall:");
        results.append(MathHelper.round(evaluationResult.getF1(EvaluationResult.MUC), 4)).append("\n");

        FileHelper.writeToFile(targetPath, results);
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

}