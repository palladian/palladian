package ws.palladian.extraction.pos;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.universal.UniversalClassifier;
import ws.palladian.classification.universal.UniversalClassifier.ClassifierSetting;
import ws.palladian.classification.universal.UniversalClassifierModel;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.collection.ArrayIterator;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Palladian version of a text-classification-based part-of-speech tagger.
 * </p>
 * 
 * @author David Urbansky
 */
public class PalladianPosTagger extends AbstractPosTagger {

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
    protected List<String> getTags(List<String> tokens) {
        List<String> tags = new ArrayList<>();
        for (String token : tokens) {
            FeatureVector featureVector = extractFeatures(token);
            CategoryEntries categoryEntries = tagger.classify(featureVector, model);
            tags.add(categoryEntries.getMostLikelyCategory());
        }
        return tags;
    }

    private static UniversalClassifier getTagger() {
        FeatureSetting featureSetting = FeatureSettingBuilder.chars(1, 7).create();
        return new UniversalClassifier(featureSetting, ClassifierSetting.TEXT, ClassifierSetting.BAYES);
    }

    /**
     * An iterator for the Brown corpus dataset. Converts the individual documents to single token instances.
     * 
     * @author Philipp Katz
     */
    private static final class BrownCorpusIterator extends AbstractIterator2<Instance> {

        final ProgressMonitor progressMonitor;
        final Iterator<File> trainingFiles;
        Iterator<Instance> currentInstances;

        BrownCorpusIterator(String trainingDirectory) {
            File[] trainingFilesArray = FileHelper.getFiles(trainingDirectory);
            trainingFiles = new ArrayIterator<File>(trainingFilesArray);
            progressMonitor = new ProgressMonitor();
            progressMonitor.startTask(null, trainingFilesArray.length);
        }

        @Override
        protected Instance getNext() {
            if (currentInstances != null && currentInstances.hasNext()) {
                return currentInstances.next();
            }
            if (trainingFiles.hasNext()) {
                progressMonitor.increment();
                currentInstances = createInstances(trainingFiles.next());
                return currentInstances.next();
            }
            return finished();
        }

        private Iterator<Instance> createInstances(File inputFile) {
            String content;
            try {
                content = FileHelper.readFileToString(inputFile);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            String[] wordsAndTagPairs = content.split("\\s");
            List<Instance> trainingInstances = new ArrayList<>();
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
                FeatureVector featureVector = extractFeatures(wordAndTag[0]);
                trainingInstances.add(new InstanceBuilder().add(featureVector).create(tag));
            }
            return trainingInstances.iterator();
        }

    }

    public static UniversalClassifierModel trainModel(final String folderPath) {
        Validate.notEmpty(folderPath, "folderPath must not be empty");
        StopWatch stopWatch = new StopWatch();
        LOGGER.info("start training the tagger");
        UniversalClassifierModel model = getTagger().train(new Iterable<Instance>() {
            @Override
            public Iterator<Instance> iterator() {
                return new BrownCorpusIterator(folderPath);
            }
        });
        LOGGER.info("finished training tagger in {}", stopWatch.getElapsedTimeString());
        return model;
    }

    private static FeatureVector extractFeatures(String word) {
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
        builder.setText(word);
        return builder.create();
    }

    public ConfusionMatrix evaluate(final String folderPath) {
        Validate.notEmpty(folderPath, "folderPath must not be empty");
        Iterable<Instance> testData = new Iterable<Instance>() {
            @Override
            public Iterator<Instance> iterator() {
                return new BrownCorpusIterator(folderPath);
            }
        };
        return ClassifierEvaluation.evaluate(tagger, testData, model);
    }

    @Override
    public String getName() {
        return TAGGER_NAME;
    }

}
