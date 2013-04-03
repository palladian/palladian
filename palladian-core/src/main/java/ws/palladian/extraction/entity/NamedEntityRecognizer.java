package ws.palladian.extraction.entity;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType;
import ws.palladian.extraction.feature.TextDocumentPipelineProcessor;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.PositionAnnotationFactory;

/**
 * <p>
 * The abstract Named Entity Recognizer (NER). Every NER should provide functionality for tagging an input text.
 * </p>
 * 
 * @author David Urbansky
 */
public abstract class NamedEntityRecognizer extends TextDocumentPipelineProcessor {

    /** The logger for named entity recognizer classes. */
    protected static final Logger LOGGER = LoggerFactory.getLogger(NamedEntityRecognizer.class);

    public static final String PROVIDED_FEATURE = "ws.palladian.processing.entity.ner";

    /** The format in which the text should be tagged. */
    private TaggingFormat taggingFormat = TaggingFormat.XML;

    public abstract Annotations getAnnotations(String inputText);

    public String tag(String inputText) {
        StopWatch stopWatch = new StopWatch();

        Annotations annotations = getAnnotations(inputText);
        String taggedText = tagText(inputText, annotations);

        LOGGER.debug("tagged text in {}", stopWatch.getElapsedTimeString(false));

        return taggedText;
    }

    protected String tagText(String inputText, Annotations annotations) {

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
                throw new IllegalStateException(errorString.toString());
            }

            if (taggingFormat == TaggingFormat.XML) {

                taggedText.append("<").append(tagName).append(">");
                taggedText.append(annotation.getEntity());
                taggedText.append("</").append(tagName).append(">");

            } else if (taggingFormat == TaggingFormat.BRACKETS) {

                taggedText.append("[").append(tagName).append(" ");
                taggedText.append(annotation.getEntity());
                taggedText.append(" ]");

            } else if (taggingFormat == TaggingFormat.SLASHES) {

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
        goldStandard.sort();
        goldStandard.save(FileHelper.getFilePath(testingFilePath) + "goldStandard.txt");

        // get the annotations of the NER
        Annotations nerAnnotations = null;
        if (configModelFilePath.length() > 0 && this instanceof TrainableNamedEntityRecognizer) {
            ((TrainableNamedEntityRecognizer)this).loadModel(configModelFilePath);
        }
        nerAnnotations = getAnnotations(FileFormatParser.getText(testingFilePath, format));

        nerAnnotations.removeNestedAnnotations();
        nerAnnotations.sort();
        String inputFile = FileHelper.getFileName(testingFilePath);
        nerAnnotations.save(FileHelper.getFilePath(testingFilePath) + "nerResult_" + inputFile + "_"
                + getName().replace(" ", "") + DateHelper.getCurrentDatetime() + ".txt");

        // see EvaluationResult for explanation of that field
        Map<String, CountMap<ResultType>> assignments = new HashMap<String, CountMap<ResultType>>();

        ConfusionMatrix confusionMatrix = new ConfusionMatrix();

        // create count maps for each possible tag (for gold standard and annotation because both could have different
        // tags)
        for (Annotation goldStandardAnnotation : goldStandard) {
            String tagName = goldStandardAnnotation.getTargetClass();
            if (assignments.get(tagName) == null) {
                assignments.put(tagName, CountMap.<ResultType> create());
            }
            assignments.get(tagName).add(ResultType.POSSIBLE);
        }
        for (Annotation nerAnnotation : nerAnnotations) {
            String tagName = nerAnnotation.getMostLikelyTagName();
            if (assignments.get(tagName) == null) {
                assignments.put(tagName, CountMap.<ResultType> create());
            }
        }

        // error map of annotations to precisely show which errors were made
        Map<ResultType, Annotations> annotationsErrors = new HashMap<ResultType, Annotations>();
        annotationsErrors.put(ResultType.CORRECT, new Annotations());
        annotationsErrors.put(ResultType.ERROR1, new Annotations());
        annotationsErrors.put(ResultType.ERROR2, new Annotations());
        annotationsErrors.put(ResultType.ERROR3, new Annotations());
        annotationsErrors.put(ResultType.ERROR4, new Annotations());
        annotationsErrors.put(ResultType.ERROR5, new Annotations());

        Set<Integer> ignoreAnnotationSet = new HashSet<Integer>();
        for (Annotation annotation : ignoreAnnotations) {
            ignoreAnnotationSet.add(annotation.getEntity().hashCode());
        }

        Set<Annotation> taggedAnnotations = CollectionHelper.newHashSet();

        // check each NER annotation against the gold standard and add it to the assignment map depending on its error
        // type, we allow only one overlap for each gold standard annotation => real(<Person>Homer J. Simpson</Person>),
        // tagged(<Person>Homer</Person> J. <Person>Simpson</Person>) => tagged(<Person>Homer</Person> J. Simpson)
        // otherwise we get problems with calculating MUC precision and recall scores
        for (Annotation nerAnnotation : nerAnnotations) {

            // skip "O" tags, XXX should this really be done here in this method?
            String assignedClass = nerAnnotation.getMostLikelyTagName();
            if (assignedClass.equalsIgnoreCase("o")) {
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

                String realClass = goldStandardAnnotation.getTargetClass();

                if (nerAnnotation.matches(goldStandardAnnotation)) {

                    // exact match
                    if (nerAnnotation.sameTag(goldStandardAnnotation)) {

                        // correct tag (no error)
                        assignments.get(assignedClass).add(ResultType.CORRECT);
                        annotationsErrors.get(ResultType.CORRECT).add(nerAnnotation);

                        // in confusion matrix real = tagged
                        confusionMatrix.add(realClass, assignedClass);

                        taggedAnnotations.add(goldStandardAnnotation);

                        break;

                    } else {

                        // wrong tag (error3)
                        assignments.get(realClass).add(ResultType.ERROR3);
                        annotationsErrors.get(ResultType.ERROR3).add(nerAnnotation);

                        // in confusion matrix real != tagged
                        confusionMatrix.add(realClass, assignedClass);

                        taggedAnnotations.add(goldStandardAnnotation);

                        break;

                    }

                } else if (nerAnnotation.overlaps(goldStandardAnnotation)) {

                    // overlaps
                    if (nerAnnotation.sameTag(goldStandardAnnotation)) {

                        // correct tag (error4)
                        assignments.get(assignedClass).add(ResultType.ERROR4);
                        annotationsErrors.get(ResultType.ERROR4).add(nerAnnotation);

                        // in confusion matrix real = tagged
                        confusionMatrix.add(realClass, assignedClass);

                        taggedAnnotations.add(goldStandardAnnotation);

                        // break;

                    } else {

                        // wrong tag (error5)
                        assignments.get(assignedClass).add(ResultType.ERROR5);
                        annotationsErrors.get(ResultType.ERROR5).add(nerAnnotation);

                        // in confusion matrix real != tagged
                        confusionMatrix.add(realClass, assignedClass);

                        taggedAnnotations.add(goldStandardAnnotation);

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
                        assignments.get(assignedClass).add(ResultType.ERROR1);
                        annotationsErrors.get(ResultType.ERROR1).add(nerAnnotation);

                        // in confusion matrix add count to "other" since NER tagged something that should not have been
                        // tagged
                        confusionMatrix.add(
                                EvaluationResult.SPECIAL_MARKER + "OTHER" + EvaluationResult.SPECIAL_MARKER,
                                assignedClass);
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
            if (!taggedAnnotations.contains(goldStandardAnnotation)) {
                assignments.get(goldStandardAnnotation.getTargetClass()).add(ResultType.ERROR2);
                annotationsErrors.get(ResultType.ERROR2).add(goldStandardAnnotation);
            }
        }

        EvaluationResult evaluationResult = new EvaluationResult(assignments, goldStandard, annotationsErrors,
                confusionMatrix);

        String evaluationDetails = evaluationResult.getEvaluationDetails();
        String evaluationFile = FileHelper.getFilePath(testingFilePath) + DateHelper.getCurrentDatetime() + "_results_"
                + inputFile + "_" + getName().replace(" ", "") + ".csv";

        FileHelper.writeToFile(evaluationFile, evaluationDetails);
        return evaluationResult;
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
                if (alignIndex + 1 >= alignedContent.length()) {
                    LOGGER.warn("Length error when aligning; aligned content is shorter than expected.");
                    break;
                }
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
            } else if (Character.isWhitespace(alignedCharacter)) {

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

            // FileHelper.writeToFile(alignFilePath, alignedContent);
        }

        FileHelper.writeToFile(alignFilePath, alignedContent);
    }

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        String content = document.getContent();
        // TODO merge annotation classes
        Annotations annotations = getAnnotations(content);

        FeatureVector featureVector = document.getFeatureVector();

        PositionAnnotationFactory annotationFactory = new PositionAnnotationFactory(PROVIDED_FEATURE, document);
        for (Annotation nerAnnotation : annotations) {
            PositionAnnotation procAnnotation = annotationFactory.create(nerAnnotation.getOffset(),
                    nerAnnotation.getEndIndex());
            featureVector.add(procAnnotation);

        }
    }

    public abstract String getName();

    public void setTaggingFormat(TaggingFormat taggingFormat) {
        this.taggingFormat = taggingFormat;
    }

    public TaggingFormat getTaggingFormat() {
        return taggingFormat;
    }

}