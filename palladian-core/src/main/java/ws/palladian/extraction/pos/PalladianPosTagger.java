package ws.palladian.extraction.pos;

import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.Instance;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.PreprocessingPipeline;
import ws.palladian.classification.text.FeatureSetting.TextFeatureType;
import ws.palladian.classification.universal.UniversalClassifier;
import ws.palladian.classification.universal.UniversalClassifier.ClassifierSetting;
import ws.palladian.classification.universal.UniversalClassifierModel;
import ws.palladian.helper.Cache;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.Feature;
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
        model = (UniversalClassifierModel)Cache.getInstance().getDataObject(modelFilePath);
        if (model == null) {
            model = FileHelper.deserialize(modelFilePath);
            Cache.getInstance().putDataObject(modelFilePath, model);
        }
        tagger = getTagger();
    }

    public PalladianPosTagger() {
        tagger = getTagger();
    }

    @Override
    public void tag(List<PositionAnnotation> annotations) {

        String previousTag = "";
        for (PositionAnnotation annotation : annotations) {

            // TODO this should only be a featureVector
            Instance instance = new Instance("test");
            setFeatures(instance, previousTag, annotation.getValue());

            CategoryEntries categoryEntries = tagger.classify(instance.getFeatureVector(), model);
            String tag = categoryEntries.getMostLikelyCategoryEntry().getName();
            assignTag(annotation, Arrays.asList(new String[] {tag}));
            previousTag = tag;
        }
    }

    private UniversalClassifier getTagger() {
        FeatureSetting featureSetting = new FeatureSetting(TextFeatureType.CHAR_NGRAMS, 1, 7);
        return new UniversalClassifier(EnumSet.of(ClassifierSetting.TEXT, ClassifierSetting.NOMINAL), featureSetting);
    }

    public void trainModel(String folderPath, String modelFilePath) {

        StopWatch stopWatch = new StopWatch();
        LOGGER.info("start training the tagger");

        List<Instance> trainingInstances = CollectionHelper.newArrayList();

        int c = 1;
        File[] trainingFiles = FileHelper.getFiles(folderPath);
        for (File file : trainingFiles) {

            String content = FileHelper.readFileToString(file);

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

                Instance instance = new Instance(normalizeTag(wordAndTag[1]));
                setFeatures(instance, previousTag, wordAndTag[0]);
                // instance.setInstanceCategory();

                trainingInstances.add(instance);

                previousTag = wordAndTag[1];
            }

            ProgressHelper.printProgress(c++, trainingFiles.length, 1);
        }

        LOGGER.info("all files read in " + stopWatch.getElapsedTimeString());
        model = tagger.train(trainingInstances);

        // classifier.learnClassifierWeightsByCategory(trainingInstances);

        FileHelper.serialize(model, modelFilePath);
        Cache.getInstance().putDataObject(modelFilePath, model);

        LOGGER.info("finished training tagger in " + stopWatch.getElapsedTimeString());
    }

    private void setFeatures(Instance instance, String previousTag, String word) {

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

        for (String nominalFeature : nominalFeatures) {
            String name = "nom" + instance.getFeatureVector().size();
            instance.getFeatureVector().add(new NominalFeature(name.intern(), nominalFeature));
        }

        try {
            PreprocessingPipeline preprocessingPipeline = new PreprocessingPipeline(tagger.getFeatureSetting());
            TextDocument textDocument = new TextDocument(word);
            preprocessingPipeline.process(textDocument);
            for (Feature<?> feature : textDocument.getFeatureVector()) {
                instance.getFeatureVector().add(feature);
            }
        } catch (DocumentUnprocessableException e) {
            throw new IllegalStateException(e);
        }
    }

    public void evaluate(String folderPath, String modelFilePath) {

        model = (UniversalClassifierModel)Cache.getInstance().getDataObject(modelFilePath, new File(modelFilePath));

        StopWatch stopWatch = new StopWatch();
        LOGGER.info("start evaluating the tagger");

        ConfusionMatrix matrix = new ConfusionMatrix();

        int c = 1;
        int correct = 0;
        int total = 0;

        // List<UniversalInstance> instances = CollectionHelper.newArrayList();

        File[] testFiles = FileHelper.getFiles(folderPath);
        for (File file : testFiles) {

            String content = FileHelper.readFileToString(file);

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

                // TextInstance result = tagger.classify(wordAndTag[0]);

                // TODO this should only be a FeatureVector and no instance.
                Instance instance = new Instance("test");
                setFeatures(instance, previousTag, wordAndTag[0]);

                CategoryEntries categoryEntries = tagger.classify(instance.getFeatureVector(), model);
                String assignedTag = categoryEntries.getMostLikelyCategoryEntry().getName();
                String correctTag = normalizeTag(wordAndTag[1]).toLowerCase();

                previousTag = assignedTag;
                matrix.add(correctTag, assignedTag);

                if (assignedTag.equals(correctTag)) {
                    correct++;
                }
                total++;
            }

            ProgressHelper.printProgress(c++, testFiles.length, 1);
        }

        LOGGER.info("all files read in " + stopWatch.getElapsedTimeString());

        LOGGER.info("Accuracy: " + MathHelper.round(100.0 * correct / total, 2) + "%");
        LOGGER.info("\n" + matrix);

        LOGGER.info("finished evaluating the tagger in " + stopWatch.getElapsedTimeString());
    }

    @Override
    public String getName() {
        return TAGGER_NAME;
    }

    public static void main(String[] args) {
        PalladianPosTagger palladianPosTagger = new PalladianPosTagger();

        // palladianPosTagger.trainModel("data/datasets/pos/all/", "ppos.gz");
        // palladianPosTagger.trainModel("data/datasets/pos/train/", "ppos.gz");
        // palladianPosTagger.evaluate("data/datasets/pos/test/", "ppos.gz");
        palladianPosTagger.trainModel("data/datasets/pos/trainSmall/", "ppos.gz");
        palladianPosTagger.evaluate("data/datasets/pos/testSmall/", "ppos.gz");

        // System.out.println(palladianPosTagger.tag("The quick brown fox jumps over the lazy dog", "ppos_.gz")
        // .getTaggedString());
        System.out.println(palladianPosTagger.tag("The quick brown fox jumps over the lazy dog").getTaggedString());
    }

}
