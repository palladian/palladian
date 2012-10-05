package ws.palladian.extraction.pos;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.classification.Instances;
import ws.palladian.classification.UniversalClassifier;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.classification.text.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.text.evaluation.FeatureSetting;
import ws.palladian.helper.Cache;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.features.Annotation;

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

    private static final long serialVersionUID = -1692291622423394544L;

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(PalladianPosTagger.class);

    private static final String TAGGER_NAME = "Palladian POS-Tagger";

    private UniversalClassifier tagger;

    public PalladianPosTagger(String modelFilePath) {
        tagger = (UniversalClassifier)Cache.getInstance().getDataObject(modelFilePath);
        if (tagger == null) {
            tagger = FileHelper.deserialize(modelFilePath);
            Cache.getInstance().putDataObject(modelFilePath, tagger);
        }
    }

    public PalladianPosTagger() {
    }

    @Override
    public void tag(List<Annotation<String>> annotations) {

        Instances<UniversalInstance> instances = new Instances<UniversalInstance>();

        String previousTag = "";
        for (Annotation<String> annotation : annotations) {

            UniversalInstance instance = new UniversalInstance(instances);
            setFeatures(instance, previousTag, annotation.getValue());

            tagger.classify(instance);
            String tag = instance.getMainCategoryEntry().getCategory().getName();
            assignTag(annotation, tag);
            previousTag = tag;
        }
    }

    // @Override
    // public PosTagger tag(String sentence) {
    //
    // Instances<UniversalInstance> instances = new Instances<UniversalInstance>();
    // TagAnnotations tagAnnotations = new TagAnnotations();
    //
    // String previousTag = "";
    // String[] words = sentence.split("\\s");
    // for (String word : words) {
    // // TextInstance result = tagger.classify(word);
    // // String tag = result.getMainCategoryEntry().getCategory().getName();
    // // TagAnnotation tagAnnotation = new TagAnnotation(sentence.indexOf(word), tag.toUpperCase(), word);
    // // tagAnnotations.add(tagAnnotation);
    //
    // UniversalInstance instance = new UniversalInstance(instances);
    // setFeatures(instance, previousTag, word);
    //
    // tagger.classify(instance);
    // String tag = instance.getMainCategoryEntry().getCategory().getName();
    // TagAnnotation tagAnnotation = new TagAnnotation(sentence.indexOf(word), tag.toUpperCase(), word);
    // tagAnnotations.add(tagAnnotation);
    //
    // previousTag = tag;
    // }
    //
    // setTagAnnotations(tagAnnotations);
    //
    // return this;
    // }

    public void trainModel(String folderPath, String modelFilePath) {

        StopWatch stopWatch = new StopWatch();
        LOGGER.info("start training the tagger");

        tagger = new UniversalClassifier();
        tagger.switchClassifiers(true, false, true);
        FeatureSetting featureSetting = new FeatureSetting();
        featureSetting.setMinNGramLength(1);
        featureSetting.setMaxNGramLength(7);
        featureSetting.setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
        ClassificationTypeSetting cts = new ClassificationTypeSetting();
        cts.setClassificationType(ClassificationTypeSetting.TAG);
        Instances<UniversalInstance> trainingInstances = new Instances<UniversalInstance>();

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

                UniversalInstance instance = new UniversalInstance(trainingInstances);
                setFeatures(instance, previousTag, wordAndTag[0]);
                instance.setInstanceCategory(normalizeTag(wordAndTag[1]));

                trainingInstances.add(instance);

                previousTag = wordAndTag[1];
            }

            ProgressHelper.showProgress(c++, trainingFiles.length, 1);
        }

        LOGGER.info("all files read in " + stopWatch.getElapsedTimeString());
        tagger.setTrainingInstances(trainingInstances);
        tagger.trainAll(cts, featureSetting);

        // classifier.learnClassifierWeightsByCategory(trainingInstances);

        FileHelper.serialize(tagger, modelFilePath);
        Cache.getInstance().putDataObject(modelFilePath, tagger);

        LOGGER.info("finished training tagger in " + stopWatch.getElapsedTimeString());
    }

    private void setFeatures(UniversalInstance instance, String previousTag, String word) {

        String lastTwo = "";
        if (word.length() > 1) {
            lastTwo = word.substring(word.length() - 2);
        }

        instance.setTextFeature(word);
        instance.setNominalFeatures(Arrays.asList(/*
         * previousTag,
         */String.valueOf(StringHelper.startsUppercase(word)),
         String.valueOf(word.length() == 1), String.valueOf(word.length() == 2),
         String.valueOf(word.length() == 3), String.valueOf(word.length()),
         String.valueOf(StringHelper.isNumberOrNumberWord(word)),
         String.valueOf(StringHelper.isCompletelyUppercase(word)),
         String.valueOf(word.replaceAll("[^`'\",.:;*\\(\\)]", "").length()),
         word.substring(word.length() - 1), word.substring(0, 1), lastTwo, word));
        // instance.setNumericFeatures(Arrays.asList((double)word.length()));
        // instance.setNominalFeatures(Arrays.asList(word));

    }

    public void evaluate(String folderPath, String modelFilePath) {

        tagger = (UniversalClassifier)Cache.getInstance().getDataObject(modelFilePath, new File(modelFilePath));

        StopWatch stopWatch = new StopWatch();
        LOGGER.info("start evaluating the tagger");

        ConfusionMatrix matrix = new ConfusionMatrix();

        int c = 1;
        int correct = 0;
        int total = 0;

        Instances<UniversalInstance> instances = new Instances<UniversalInstance>();

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

                UniversalInstance instance = new UniversalInstance(instances);
                setFeatures(instance, previousTag, wordAndTag[0]);

                tagger.classify(instance);
                String assignedTag = instance.getMainCategoryEntry().getCategory().getName();
                String correctTag = normalizeTag(wordAndTag[1]).toLowerCase();

                previousTag = assignedTag;
                matrix.increment(correctTag, assignedTag);

                if (assignedTag.equals(correctTag)) {
                    correct++;
                }
                total++;
            }

            ProgressHelper.showProgress(c++, testFiles.length, 1);
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
        // /palladianPosTagger.trainModel("data/datasets/pos/train/", "ppos.gz");
        // palladianPosTagger.evaluate("data/datasets/pos/test/", "ppos.gz");
        // palladianPosTagger.trainModel("data/datasets/pos/trainSmall/", "ppos.gz");
        palladianPosTagger.evaluate("data/datasets/pos/testSmall/", "ppos.gz");

        // System.out.println(palladianPosTagger.tag("The quick brown fox jumps over the lazy dog", "ppos_.gz")
        // .getTaggedString());
        System.out.println(palladianPosTagger.tag("The quick brown fox jumps over the lazy dog").getTaggedString());
    }

}
