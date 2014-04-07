package ws.palladian.extraction.pos;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.InstanceBuilder;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.universal.UniversalClassifier;
import ws.palladian.classification.universal.UniversalClassifier.ClassifierSetting;
import ws.palladian.classification.universal.UniversalClassifier.UniversalTrainable;
import ws.palladian.classification.universal.UniversalClassifierModel;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * Palladian version of a text-classification-based part-of-speech tagger.
 * </p>
 * 
 * @author David Urbansky
 */
public class PalladianPosTagger extends BasePosTagger {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianPosTagger.class);

    private static final String TAGGER_NAME = "Palladian POS-Tagger";

    private final UniversalClassifier tagger = getTagger();

    private final UniversalClassifierModel model;

    public PalladianPosTagger(String modelFilePath) {
        try {
            model = FileHelper.deserialize(modelFilePath);
        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }

    public PalladianPosTagger(UniversalClassifierModel model) {
        this.model = model;
    }

    @Override
    public void tag(List<PositionAnnotation> annotations) {
        String previousTag = "";
        for (PositionAnnotation annotation : annotations) {
            FeatureVector featureVector = extractFeatures(previousTag, annotation.getValue(), null);
            CategoryEntries categoryEntries = tagger.classify(featureVector, model);
            String tag = categoryEntries.getMostLikelyCategory();
            assignTag(annotation, Arrays.asList(new String[] {tag}));
            previousTag = tag;
        }
    }

    private static UniversalClassifier getTagger() {
        FeatureSetting featureSetting = FeatureSettingBuilder.chars(1, 7).create();
        return new UniversalClassifier(EnumSet.of(ClassifierSetting.TEXT, ClassifierSetting.BAYES), featureSetting);
    }

    public static UniversalClassifierModel trainModel(String folderPath) {

        StopWatch stopWatch = new StopWatch();
        LOGGER.info("start training the tagger");

        List<UniversalTrainable> trainingInstances = CollectionHelper.newArrayList();

        File[] trainingFiles = FileHelper.getFiles(folderPath);
        ProgressMonitor progressMonitor = new ProgressMonitor(trainingFiles.length, 1);
        for (File file : trainingFiles) {

            String content = FileHelper.tryReadFileToString(file);

            String[] wordsAndTagPairs = content.split("\\s");

            String previousTag = "";

            for (String wordAndTagPair : wordsAndTagPairs) {

                if (wordAndTagPair.isEmpty()) {
                    continue;
                }

                String[] wordAndTag = wordAndTagPair.split("/");
                String word = wordAndTag[0];

                if (wordAndTag.length < 2 || word.isEmpty()) {
                    continue;
                }

                String tag = normalizeTag(wordAndTag[1]);
                if (tag.isEmpty()) {
                    continue;
                }
                UniversalTrainable featureVector = extractFeatures(previousTag, wordAndTag[0], tag);
                trainingInstances.add(featureVector);

                previousTag = wordAndTag[1];
            }

            progressMonitor.incrementAndPrintProgress();
        }

        LOGGER.info("all files read in {}", stopWatch.getElapsedTimeString());
        UniversalClassifierModel model = getTagger().train(trainingInstances);
        LOGGER.info("finished training tagger in {}", stopWatch.getElapsedTimeString());
        return model;
    }

    private static UniversalTrainable extractFeatures(String previousTag, String word, String targetClass) {

        int wordLength = word.length();
        InstanceBuilder builder = new InstanceBuilder();
        builder.set("startsUppercase", StringHelper.startsUppercase(word));
        builder.set("length1", wordLength == 1);
        builder.set("length2", wordLength == 2);
        builder.set("length3", wordLength == 3);
        builder.set("length", String.valueOf(wordLength));
        builder.set("number", StringHelper.isNumberOrNumberWord(word));
        builder.set("completelyUppercase", StringHelper.isCompletelyUppercase(word));
        builder.set("normalizedLength", String.valueOf(word.replaceAll("[^`'\",.:;*\\(\\)]", EMPTY).length()));
        builder.set("lastCharacter", word.substring(wordLength - 1));
        builder.set("firstCharacter", word.substring(0, 1));
        builder.set("lastTwoCharacters", wordLength > 1 ? word.substring(wordLength - 2) : EMPTY);
        builder.set("word", word);

        // instance.setNumericFeatures(Arrays.asList((double)word.length()));
        // instance.setNominalFeatures(Arrays.asList(word));

        FeatureVector featureVector = builder.create();
        return new UniversalTrainable(word, featureVector, targetClass);
    }

    public void evaluate(String folderPath) {
        StopWatch stopWatch = new StopWatch();
        LOGGER.info("start evaluating the tagger");

        ConfusionMatrix matrix = new ConfusionMatrix();

        int correct = 0;
        int total = 0;

        File[] testFiles = FileHelper.getFiles(folderPath);
        ProgressMonitor progressMonitor = new ProgressMonitor(testFiles.length, 1);
        for (File file : testFiles) {

            String content = FileHelper.tryReadFileToString(file);

            String[] wordsAndTagPairs = content.split("\\s");

            String previousTag = "";

            for (String wordAndTagPair : wordsAndTagPairs) {

                if (wordAndTagPair.isEmpty()) {
                    continue;
                }

                String[] wordAndTag = wordAndTagPair.split("/");
                String word = wordAndTag[0];

                if (wordAndTag.length < 2 || word.isEmpty()) {
                    continue;
                }

                FeatureVector featureVector = extractFeatures(previousTag, wordAndTag[0], null);
                CategoryEntries categoryEntries = tagger.classify(featureVector, model);
                String assignedTag = categoryEntries.getMostLikelyCategory();
                String correctTag = normalizeTag(wordAndTag[1]).toLowerCase();

                previousTag = assignedTag;
                matrix.add(correctTag, assignedTag);

                if (assignedTag.equals(correctTag)) {
                    correct++;
                }
                total++;
            }

            progressMonitor.incrementAndPrintProgress();
        }

        LOGGER.info("all files read in {}", stopWatch.getElapsedTimeString());

        LOGGER.info("Accuracy: {}", MathHelper.round(100.0 * correct / total, 2) + "%");
        LOGGER.info("\n{}", matrix);

        LOGGER.info("finished evaluating the tagger in {}", stopWatch.getElapsedTimeString());
    }

    @Override
    public String getName() {
        return TAGGER_NAME;
    }

}
