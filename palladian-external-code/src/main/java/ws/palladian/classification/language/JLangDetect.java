package ws.palladian.classification.language;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import me.champeau.ld.AbstractGramTree;
import me.champeau.ld.GramTreeBuilder;
import me.champeau.ld.LangDetector;
import me.champeau.ld.LangDetector.Score;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Wrapper for <a href="http://www.jroller.com/melix/entry/jlangdetect_0_3_released_with">JLangDetect</a>.
 * </p>
 * 
 * @author Philipp Katz
 */
public class JLangDetect implements LanguageClassifier {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(JLangDetect.class);

    /** The confidence threshold for a language. This is the minimal percentage of n-grams which have to match. */
    private static final float THRESHOLD = 0.0f;

    /** The wrapped language detector. */
    private final LangDetector langDetector;

    /**
     * <p>
     * Instantiate a new {@link JLangDetect} using the supplied implementation of {@link LangDetector}.
     * </p>
     * 
     * @param langDetector The {@link LangDetector} to use, not <code>null</code>.
     */
    public JLangDetect(LangDetector langDetector) {
        Validate.notNull(langDetector, "langDetector must not be null");
        this.langDetector = langDetector;
    }

    /**
     * <p>
     * Instantiate a new {@link JLangDetect}.
     * </p>
     * 
     * @param modelPath The path to the directory with the model files. Their names need to comply with the
     *            following naming convention: <code>xx_tree.bin</code>, where xx denotes the language. Not
     *            <code>null</code> or empty.
     */
    public JLangDetect(File modelPath) {
        Validate.notNull(modelPath, "modelPath must not be null");
        if (!modelPath.isDirectory()) {
            throw new IllegalStateException("The path " + modelPath + " does not point to a directory.");
        }
        this.langDetector = loadTreeFiles(modelPath);
    }

    private final LangDetector loadTreeFiles(File modelPath) {
        LangDetector result = new LangDetector();
        File[] files = FileHelper.getFiles(modelPath.getPath(), "_tree.bin");
        for (File file : files) {
            AbstractGramTree tree = (AbstractGramTree)FileHelper.deserialize(file.getPath());
            String lang = file.getName().substring(0, 2);
            result.register(lang, tree);
        }
        return result;
    }

    /**
     * <p>
     * Train the language detector on a dataset.
     * </p>
     * 
     * @param dataset The dataset to train on.
     * @param modelPath The path to the directory where the trained models are stored, not <code>null</code>.
     */
    public static void train(Dataset dataset, File modelPath) {

        if (!modelPath.exists()) {
            boolean directoryCreated = modelPath.mkdirs();
            if (!directoryCreated) {
                throw new IllegalStateException("Could not create the directory " + modelPath);
            }
        }
        if (!modelPath.isDirectory()) {
            throw new IllegalStateException("The path " + modelPath + " does not point to a directory.");
        }

        StopWatch stopWatch = new StopWatch();
        LOGGER.info("Start training jLangDetect");

        // a map holding all file links for each class
        LazyMap<String, GramTreeBuilder> treeBuilders = LazyMap.create(new Factory<GramTreeBuilder>() {
            @Override
            public GramTreeBuilder create() {
                GramTreeBuilder builder = new GramTreeBuilder(1, 3);
                builder.setTruncationThreshold(0.1);
                return builder;
            }
        });

        // group entries by language
        List<String> documentIndexEntries = FileHelper.readFileToArray(dataset.getPath());
        for (String line : documentIndexEntries) {
            String[] parts = line.split(" ");
            String language = parts[1];
            String link = parts[0];
            String text = FileHelper.tryReadFileToString(dataset.getRootPath() + link);
            treeBuilders.get(language).learn(text);
        }

        // build the trees and serialize them
        for (String language : treeBuilders.keySet()) {
            GramTreeBuilder treeBuilder = treeBuilders.get(language);
            AbstractGramTree tree = treeBuilder.build();
            File serializePath = new File(modelPath, language + "_tree.bin");
            FileHelper.serialize(tree, serializePath.getPath());
        }

        LOGGER.info("Trained jLangDetect in {}", stopWatch.getElapsedTimeString());
    }

    @Override
    public Language classify(String text) {
        Collection<Score> languages = langDetector.scoreLanguages(text);
        Iterator<Score> iterator = languages.iterator();
        if (iterator.hasNext()) {
            Score langScore = iterator.next();
            // wtf are those attributes private? So we have to parse them from toString()
            String scoreString = langScore.toString();
            String lang = StringHelper.getSubstringBetween(scoreString, "Score{language='", "', score=");
            double score = Double.valueOf(StringHelper.getSubstringBetween(scoreString, "score=", "}"));
            if (score > THRESHOLD) {
                return Language.getByIso6391(lang);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "JLangDetect";
    }

    public static void main(String[] args) {
        Dataset dataset = new Dataset();
        dataset.setPath("/Users/pk/Dropbox/Uni/Datasets/Wikipedia76Languages/languageDocumentIndex.txt");
        dataset.setFirstFieldLink(true);
        dataset.setSeparationString(" ");
        JLangDetect.train(dataset, new File("jLangDetectModel"));
    }

}
