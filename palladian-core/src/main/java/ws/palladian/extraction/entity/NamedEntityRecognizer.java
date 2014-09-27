package ws.palladian.extraction.entity;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.core.Annotation;
import ws.palladian.core.Tagger;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.ResultType;
import ws.palladian.extraction.entity.tagger.NerHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * The abstract Named Entity Recognizer (NER). Every NER should provide functionality for tagging an input text.
 * </p>
 * 
 * @author David Urbansky
 */
public abstract class NamedEntityRecognizer implements Tagger {

    /** The logger for named entity recognizer classes. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NamedEntityRecognizer.class);

    /** The format in which the text should be tagged. */
    private TaggingFormat taggingFormat = TaggingFormat.XML;

    @Override
    public abstract List<? extends Annotation> getAnnotations(String inputText);

    public String tag(String inputText) {
        StopWatch stopWatch = new StopWatch();

        List<? extends Annotation> annotations = getAnnotations(inputText);
        String taggedText = NerHelper.tag(inputText, annotations, taggingFormat);

        LOGGER.debug("tagged text in {}", stopWatch.getElapsedTimeString(false));

        return taggedText;
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
        Annotations<Annotation> goldStandard = FileFormatParser.getAnnotations(testingFilePath, format);
        goldStandard.sort();
        // goldStandard.save(FileHelper.getFilePath(testingFilePath) + "goldStandard.txt");

        // get the annotations of the NER
        List<? extends Annotation> nerAnnotations = getAnnotations(FileFormatParser.getText(testingFilePath, format));
        Annotations<Annotation> annotations = new Annotations<Annotation>(nerAnnotations);
        annotations.removeNested();
        annotations.sort();
        // String inputFile = FileHelper.getFileName(testingFilePath);
        // nerAnnotations.save(FileHelper.getFilePath(testingFilePath) + "nerResult_" + inputFile + "_"
        // + getName().replace(" ", "") + DateHelper.getCurrentDatetime() + ".txt");

        return evaluate(goldStandard, annotations, ignore);
    }

    public static EvaluationResult evaluate(List<? extends Annotation> goldStandard,
            List<? extends Annotation> nerResult,
            Set<String> ignore) {

        EvaluationResult evaluationResult = new EvaluationResult(goldStandard);
        Set<Annotation> taggedAnnotations = CollectionHelper.newHashSet();

        // check each NER annotation against the gold standard and add it to the assignment map depending on its error
        // type, we allow only one overlap for each gold standard annotation => real(<Person>Homer J. Simpson</Person>),
        // tagged(<Person>Homer</Person> J. <Person>Simpson</Person>) => tagged(<Person>Homer</Person> J. Simpson)
        // otherwise we get problems with calculating MUC precision and recall scores
        for (Annotation nerAnnotation : nerResult) {

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

                if (nerAnnotation.congruent(goldStandardAnnotation)) {
                    // exact match
                    taggedAnnotations.add(goldStandardAnnotation);
                    if (nerAnnotation.sameTag(goldStandardAnnotation)) {
                        // correct tag (no error)
                        evaluationResult.add(ResultType.CORRECT, goldStandardAnnotation, nerAnnotation);
                    } else {
                        // wrong tag (error3)
                        evaluationResult.add(ResultType.ERROR3, goldStandardAnnotation, nerAnnotation);
                    }
                    break;
                } else if (nerAnnotation.overlaps(goldStandardAnnotation)) {
                    // overlaps
                    taggedAnnotations.add(goldStandardAnnotation);
                    if (nerAnnotation.sameTag(goldStandardAnnotation)) {
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

    public abstract String getName();

    public void setTaggingFormat(TaggingFormat taggingFormat) {
        this.taggingFormat = taggingFormat;
    }

    public TaggingFormat getTaggingFormat() {
        return taggingFormat;
    }

}