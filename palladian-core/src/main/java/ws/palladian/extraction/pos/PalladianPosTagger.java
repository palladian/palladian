package ws.palladian.extraction.pos;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
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
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.ArrayIterator;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;
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
        for (PositionAnnotation annotation : annotations) {
            FeatureVector featureVector = extractFeatures(annotation.getValue(), null);
            CategoryEntries categoryEntries = tagger.classify(featureVector, model);
            String tag = categoryEntries.getMostLikelyCategory();
            assignTag(annotation, Collections.singletonList(tag));
        }
    }

    private static UniversalClassifier getTagger() {
        FeatureSetting featureSetting = FeatureSettingBuilder.chars(1, 7).create();
        return new UniversalClassifier(EnumSet.of(ClassifierSetting.TEXT, ClassifierSetting.BAYES), featureSetting);
    }
    
    private static final class BrownCorpusIterator extends AbstractIterator<UniversalTrainable> {
        
        final ProgressMonitor progressMonitor;
        final Iterator<File> trainingFiles;
        Iterator<UniversalTrainable> currentInstances;
        
        public BrownCorpusIterator(String trainingDirectory) {
            File[] trainingFilesArray = FileHelper.getFiles(trainingDirectory);
            trainingFiles = new ArrayIterator<File>(trainingFilesArray);
            progressMonitor = new ProgressMonitor(trainingFilesArray.length, 1);
        }

        @Override
        protected UniversalTrainable getNext() throws Finished {
            if (currentInstances != null && currentInstances.hasNext()) {
                return currentInstances.next();
            }
            if (trainingFiles.hasNext()) {
                progressMonitor.incrementAndPrintProgress();
                currentInstances = createInstances(trainingFiles.next());
                return currentInstances.next();
            }
            throw FINISHED;
        }

        private Iterator<UniversalTrainable> createInstances(File inputFile) {
            String content;
            try {
                content = FileHelper.readFileToString(inputFile);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            String[] wordsAndTagPairs = content.split("\\s");
            List<UniversalTrainable> trainingInstances = CollectionHelper.newArrayList();
            for (String wordAndTagPair : wordsAndTagPairs) {
                String[] wordAndTag = wordAndTagPair.split("/");
                String word = wordAndTag[0];
                if (wordAndTag.length < 2 || word.isEmpty()) {
                    continue;
                }
                String tag = normalizeTag(wordAndTag[1]);
                if (tag.isEmpty()) {
                    continue;
                }
                UniversalTrainable featureVector = extractFeatures(wordAndTag[0], tag);
                trainingInstances.add(featureVector);
            }
            return trainingInstances.iterator();
        }
        
    }

    public static UniversalClassifierModel trainModel(final String folderPath) {
        StopWatch stopWatch = new StopWatch();
        LOGGER.info("start training the tagger");
        UniversalClassifierModel model = getTagger().train(new Iterable<UniversalTrainable>() {
            @Override
            public Iterator<UniversalTrainable> iterator() {
                return new BrownCorpusIterator(folderPath);
            }
        });
        LOGGER.info("finished training tagger in {}", stopWatch.getElapsedTimeString());
        return model;
    }

    private static UniversalTrainable extractFeatures(String word, String targetClass) {
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
        return new UniversalTrainable(word, builder.create(), targetClass);
    }

    public ConfusionMatrix evaluate(String folderPath) {
        ConfusionMatrix matrix = new ConfusionMatrix();
        File[] testFiles = FileHelper.getFiles(folderPath);
        ProgressMonitor progressMonitor = new ProgressMonitor(testFiles.length, 1);
        for (File file : testFiles) {

            String content = FileHelper.tryReadFileToString(file);

            String[] wordsAndTagPairs = content.split("\\s");

            for (String wordAndTagPair : wordsAndTagPairs) {

                if (wordAndTagPair.isEmpty()) {
                    continue;
                }

                String[] wordAndTag = wordAndTagPair.split("/");
                String word = wordAndTag[0];

                if (wordAndTag.length < 2 || word.isEmpty()) {
                    continue;
                }

                FeatureVector featureVector = extractFeatures(wordAndTag[0], null);
                CategoryEntries categoryEntries = tagger.classify(featureVector, model);
                String assignedTag = categoryEntries.getMostLikelyCategory();
                String correctTag = normalizeTag(wordAndTag[1]).toLowerCase();
                matrix.add(correctTag, assignedTag);
            }
            progressMonitor.incrementAndPrintProgress();
        }
        return matrix;
    }

    @Override
    public String getName() {
        return TAGGER_NAME;
    }

}
