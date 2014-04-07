package ws.palladian.extraction.pos;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Instance;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.text.PreprocessingPipeline;
import ws.palladian.classification.universal.UniversalClassifier;
import ws.palladian.classification.universal.UniversalClassifier.ClassifierSetting;
import ws.palladian.classification.universal.UniversalClassifierModel;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.BasicFeatureVector;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * Palladian version of a text-classification-based part-of-speech tagger.
 * </p>
 * 
 * XXX this is a super use case for an universal classifier using different types of features + neighbors
 * 
 * @author David Urbansky
 */
public class PalladianPosTagger extends BasePosTagger {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianPosTagger.class);

    private static final String TAGGER_NAME = "Palladian POS-Tagger";

    private UniversalClassifier tagger;
    private UniversalClassifierModel model;

    public PalladianPosTagger(String modelFilePath) {
        try {
            model = FileHelper.deserialize(modelFilePath);
            tagger = getTagger();
        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }

    public PalladianPosTagger() {
        tagger = getTagger();
    }

    @Override
    public void tag(List<PositionAnnotation> annotations) {

        String previousTag = "";
        for (PositionAnnotation annotation : annotations) {

            FeatureVector featureVector = extractFeatures(previousTag, annotation.getValue());

            CategoryEntries categoryEntries = tagger.classify(featureVector, model);
            String tag = categoryEntries.getMostLikelyCategory();
            assignTag(annotation, Arrays.asList(new String[] {tag}));
            previousTag = tag;
        }
    }

    private UniversalClassifier getTagger() {
        FeatureSetting featureSetting = FeatureSettingBuilder.chars(1, 7).create();
        return new UniversalClassifier(EnumSet.of(ClassifierSetting.TEXT, ClassifierSetting.NOMINAL), featureSetting);
    }

    public UniversalClassifierModel trainModel(String folderPath, String modelFilePath) throws IOException {

        StopWatch stopWatch = new StopWatch();
        LOGGER.info("start training the tagger");

        List<Instance> trainingInstances = CollectionHelper.newArrayList();

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

                FeatureVector featureVector = extractFeatures(previousTag, wordAndTag[0]);
                Instance instance = new Instance(normalizeTag(wordAndTag[1]), featureVector);

                trainingInstances.add(instance);

                previousTag = wordAndTag[1];
            }

            progressMonitor.incrementAndPrintProgress();
        }

        LOGGER.info("all files read in {}", stopWatch.getElapsedTimeString());
        model = tagger.train(trainingInstances);

        // classifier.learnClassifierWeightsByCategory(trainingInstances);

        FileHelper.serialize(model, modelFilePath);

        LOGGER.info("finished training tagger in {}", stopWatch.getElapsedTimeString());

        return model;
    }

    private FeatureVector extractFeatures(String previousTag, String word) {

        String lastTwo = "";
        if (word.length() > 1) {
            lastTwo = word.substring(word.length() - 2);
        }

        // instance.setTextFeature(word);

        List<String> nominalFeatures = Arrays.asList(
                // previousTag
                String.valueOf(StringHelper.startsUppercase(word)), String.valueOf(word.length() == 1),
                String.valueOf(word.length() == 2), String.valueOf(word.length() == 3), String.valueOf(word.length()),
                String.valueOf(StringHelper.isNumberOrNumberWord(word)),
                String.valueOf(StringHelper.isCompletelyUppercase(word)),
                String.valueOf(word.replaceAll("[^`'\",.:;*\\(\\)]", "").length()), word.substring(word.length() - 1),
                word.substring(0, 1), lastTwo, word);
        // instance.setNumericFeatures(Arrays.asList((double)word.length()));
        // instance.setNominalFeatures(Arrays.asList(word));

        FeatureVector featureVector = new BasicFeatureVector();
        for (String nominalFeature : nominalFeatures) {
            String name = "nom" + featureVector.size();
            featureVector.add(new NominalFeature(name.intern(), nominalFeature));
        }

        PreprocessingPipeline preprocessingPipeline = new PreprocessingPipeline(tagger.getFeatureSetting());
        TextDocument textDocument = new TextDocument(word);
        preprocessingPipeline.process(textDocument);
        for (Feature<?> feature : textDocument.getFeatureVector()) {
            featureVector.add(feature);
        }

        return featureVector;
    }

    public void evaluate(String folderPath, String modelFilePath) {

        try {
            model = FileHelper.deserialize(modelFilePath);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

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

                FeatureVector featureVector = extractFeatures(previousTag, wordAndTag[0]);
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

    public static void main(String[] args) {
        // PalladianPosTagger palladianPosTagger = new PalladianPosTagger();

        // palladianPosTagger.trainModel("data/datasets/pos/all/", "ppos.gz");
        // palladianPosTagger.trainModel("data/datasets/pos/train/", "ppos.gz");
        // palladianPosTagger.evaluate("data/datasets/pos/test/", "ppos.gz");
        // palladianPosTagger.trainModel("data/datasets/pos/trainSmall/", "ppos.gz");
        // palladianPosTagger.evaluate("data/datasets/pos/testSmall/", "ppos.gz");

        // System.out.println(palladianPosTagger.tag("The quick brown fox jumps over the lazy dog", "ppos_.gz")
        // .getTaggedString());
        // System.out.println(palladianPosTagger.tag("The quick brown fox jumps over the lazy dog").getTaggedString());
    }

}
