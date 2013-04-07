package ws.palladian.extraction.entity;

import java.io.File;
import java.util.Collections;
import java.util.List;
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
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.Annotated;
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

    public abstract List<? extends Annotated> getAnnotations(String inputText);

    public String tag(String inputText) {
        StopWatch stopWatch = new StopWatch();

        List<? extends Annotated> annotations = getAnnotations(inputText);
        String taggedText = tagText(inputText, annotations);

        LOGGER.debug("tagged text in {}", stopWatch.getElapsedTimeString(false));

        return taggedText;
    }

    protected String tagText(String inputText, List<? extends Annotated> annotations) {

        return tag(inputText, annotations, taggingFormat);

    }

    public static String tag(String inputText, List<? extends Annotated> annotations, TaggingFormat taggingFormat) {
        StringBuilder taggedText = new StringBuilder();

        int lastEndIndex = 0;

        // we need to sort in ascending order first
        Collections.sort(annotations);

        Annotated lastAnnotation = null;
        for (Annotated annotation : annotations) {

            // ignore nested annotations
            if (annotation.getStartPosition() < lastEndIndex) {
                continue;
            }

            String tagName = annotation.getTag();

            taggedText.append(inputText.substring(lastEndIndex, annotation.getStartPosition()));

            String correctText = inputText.substring(annotation.getStartPosition(), annotation.getEndPosition());

            if (!correctText.equalsIgnoreCase(annotation.getValue()) && correctText.indexOf("\n") == -1) {
                StringBuilder errorString = new StringBuilder();
                errorString.append("alignment error, the annotation candidates don't match the text:\n");
                errorString.append("found: " + correctText + "\n");
                errorString.append("instead of: " + annotation.getValue() + "(" + annotation + ")\n");
                errorString.append("last annotation: " + lastAnnotation);
                throw new IllegalStateException(errorString.toString());
            }

            if (taggingFormat == TaggingFormat.XML) {

                taggedText.append("<").append(tagName).append(">");
                taggedText.append(annotation.getValue());
                taggedText.append("</").append(tagName).append(">");

            } else if (taggingFormat == TaggingFormat.BRACKETS) {

                taggedText.append("[").append(tagName).append(" ");
                taggedText.append(annotation.getValue());
                taggedText.append(" ]");

            } else if (taggingFormat == TaggingFormat.SLASHES) {

                List<String> tokens = Tokenizer.tokenize(annotation.getValue());
                for (String token : tokens) {
                    taggedText.append(token).append("/").append(tagName).append(" ");
                }

            }

            lastEndIndex = annotation.getEndPosition();
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

        String tempFilePath = "data/temp/nerConcatenatedEvaluation.xml";

        // delete temp file that might have been created
        FileHelper.delete(tempFilePath);

        // concatenate all xml files from the training index to one large file
        List<String> lines = FileHelper.readFileToArray(dataset.getPath());
        for (String line : lines) {

            String[] parts = line.split(" ");

            FileHelper.concatenateFiles(new File(tempFilePath), new File(dataset.getRootPath() + parts[0]));
        }

        return evaluate(tempFilePath, TaggingFormat.XML);
    }

    public EvaluationResult evaluate(String testingFilePath, TaggingFormat format) {
        return evaluate(testingFilePath, format, Collections.<String> emptySet());
    }

    public EvaluationResult evaluate(String testingFilePath, TaggingFormat format, Set<String> ignore) {

        // get the correct annotations from the testing file
        List<Annotation> goldStandard = FileFormatParser.getAnnotations(testingFilePath, format);
        Collections.sort(goldStandard);
        // goldStandard.save(FileHelper.getFilePath(testingFilePath) + "goldStandard.txt");

        // get the annotations of the NER
        List<? extends Annotated> nerAnnotations = getAnnotations(FileFormatParser.getText(testingFilePath, format));
        // nerAnnotations.removeNestedAnnotations();
        Annotations.removeNestedAnnotations(nerAnnotations);
        Collections.sort(nerAnnotations);
        // String inputFile = FileHelper.getFileName(testingFilePath);
        // nerAnnotations.save(FileHelper.getFilePath(testingFilePath) + "nerResult_" + inputFile + "_"
        // + getName().replace(" ", "") + DateHelper.getCurrentDatetime() + ".txt");

        return evaluate(goldStandard, nerAnnotations, ignore);
    }

    public static EvaluationResult evaluate(List<Annotation> goldStandard, List<? extends Annotated> nerResult,
            Set<String> ignore) {

        EvaluationResult evaluationResult = new EvaluationResult(goldStandard);
        Set<Annotation> taggedAnnotations = CollectionHelper.newHashSet();

        // check each NER annotation against the gold standard and add it to the assignment map depending on its error
        // type, we allow only one overlap for each gold standard annotation => real(<Person>Homer J. Simpson</Person>),
        // tagged(<Person>Homer</Person> J. <Person>Simpson</Person>) => tagged(<Person>Homer</Person> J. Simpson)
        // otherwise we get problems with calculating MUC precision and recall scores
        for (Annotated nerAnnotation : nerResult) {

            // skip "O" tags, XXX should this really be done here in this method?
            if (nerAnnotation.getTag().equalsIgnoreCase("o")) {
                continue;
            }

            boolean taggedOverlap = false;
            int counter = 0;

            for (Annotation goldStandardAnnotation : goldStandard) {

                counter++;

                // skip ignored annotations for error cases 2,3,4, and 5, however, leave the possibility for error 1
                // (tagged something that should not have been tagged)
                if (ignore.contains(goldStandardAnnotation.getValue())
                        && !(nerAnnotation.getStartPosition() < goldStandardAnnotation.getEndPosition() && !taggedOverlap)) {
                    continue;
                }

                boolean samePositionAndLength = nerAnnotation.getStartPosition() == goldStandardAnnotation
                        .getStartPosition()
                        && nerAnnotation.getValue().length() == goldStandardAnnotation.getValue().length();
                boolean overlaps = nerAnnotation.getStartPosition() <= goldStandardAnnotation.getStartPosition()
                        && nerAnnotation.getEndPosition() >= goldStandardAnnotation.getStartPosition()
                        || nerAnnotation.getStartPosition() <= goldStandardAnnotation.getEndPosition()
                        && nerAnnotation.getEndPosition() >= goldStandardAnnotation.getStartPosition();
                boolean sameTag = nerAnnotation.getTag().equalsIgnoreCase(goldStandardAnnotation.getTag());

                if (samePositionAndLength) {
                    // exact match
                    taggedAnnotations.add(goldStandardAnnotation);
                    if (sameTag) {
                        // correct tag (no error)
                        evaluationResult.add(ResultType.CORRECT, goldStandardAnnotation, nerAnnotation);
                    } else {
                        // wrong tag (error3)
                        evaluationResult.add(ResultType.ERROR3, goldStandardAnnotation, nerAnnotation);
                    }
                    break;
                } else if (overlaps) {
                    // overlaps
                    taggedAnnotations.add(goldStandardAnnotation);
                    if (sameTag) {
                        // correct tag (error4)
                        evaluationResult.add(ResultType.ERROR4, goldStandardAnnotation, nerAnnotation);
                    } else {
                        // wrong tag (error5)
                        evaluationResult.add(ResultType.ERROR5, goldStandardAnnotation, nerAnnotation);
                    }
                    taggedOverlap = true;
                } else if (nerAnnotation.getStartPosition() < goldStandardAnnotation.getEndPosition()
                        || counter == goldStandard.size()) {
                    if (!taggedOverlap) {
                        // tagged something that should not have been tagged (error1)
                        evaluationResult.add(ResultType.ERROR1, null, nerAnnotation);
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
                evaluationResult.add(ResultType.ERROR2, goldStandardAnnotation, null);
            }
        }

        // String evaluationDetails = evaluationResult.getEvaluationDetails();
        // String evaluationFile = FileHelper.getFilePath(testingFilePath) + DateHelper.getCurrentDatetime() +
        // "_results_"
        // + inputFile + "_" + getName().replace(" ", "") + ".csv";

        // FileHelper.writeToFile(evaluationFile, evaluationDetails);
        return evaluationResult;
    }
    
    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        String content = document.getContent();
        // TODO merge annotation classes
        List<? extends Annotated> annotations = getAnnotations(content);

        FeatureVector featureVector = document.getFeatureVector();

        PositionAnnotationFactory annotationFactory = new PositionAnnotationFactory(PROVIDED_FEATURE, document);
        for (Annotated nerAnnotation : annotations) {
            PositionAnnotation procAnnotation = annotationFactory.create(nerAnnotation.getStartPosition(),
                    nerAnnotation.getEndPosition());
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