package ws.palladian.extraction.entity.tagger;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.Annotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TrainableNamedEntityRecognizer;
import ws.palladian.helper.io.FileHelper;
import edu.illinois.cs.cogcomp.LbjNer.LbjTagger.LearningCurveMultiDataset;
import edu.illinois.cs.cogcomp.LbjNer.LbjTagger.NETagPlain;
import edu.illinois.cs.cogcomp.LbjNer.LbjTagger.Parameters;

/**
 * <p>
 * This class wraps the Learning Java Based Illinois Named Entity Tagger. It uses conditional random fields for tagging.
 * The implementation is in an external library and the approach is explained in the following paper by L. Ratinov and
 * D. Roth: "Design Challenges and Misconceptions in Named Entity Recognition", CoNLL 2009.
 * 
 * @see <a href="http://cogcomp.cs.illinois.edu/page/software_view/4">Illinois Named Entity Tagger</a>
 * @author David Urbansky
 * @author Philipp Katz
 */
public class IllinoisNer extends TrainableNamedEntityRecognizer {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(IllinoisNer.class);

    /** The name of the configuration file in the target directory. */
    private static final String CONFIG_FILE_NAME = "IllinoisNer.config";

    /** The default number of training rounds. */
    public static final int DEFAULT_TRAINING_ROUNDS = -1;

    /** Number of rounds for training. */
    private final int trainingRounds;

    /**
     * <p>
     * Create a new {@link IllinoisNer} using specified number of iterations for training.
     * 
     * @param trainingRounds The number of iterations for training, greater zero, or -1 to use an automatic convergence
     *            criterion.
     */
    public IllinoisNer(int trainingRounds) {
        Validate.isTrue(trainingRounds == -1 || trainingRounds > 0,
                "trainingRounds must be greater zero, or -1 to use automatic convergence criterion");
        this.trainingRounds = trainingRounds;
    }

    /**
     * <p>
     * Create a new {@link IllinoisNer} using the automatic convergence criterion for training.
     */
    public IllinoisNer() {
        this(DEFAULT_TRAINING_ROUNDS);
    }

    private static String buildConfig(String modelFile, Set<String> tags) {
        StringBuilder config = new StringBuilder();
        // # Required fields
        config.append("configFilename ").append("IllinoisNER").append('\n');
        config.append("pathToModelFile ").append(modelFile).append('\n');
        config.append("taggingEncodingScheme BIO\n");
        config.append("tokenizationScheme LbjTokenizationScheme\n");
        // # Optional fields
        // config.append("beamSize 5\n");
        config.append("forceNewSentenceOnLineBreaks true\n");
        config.append("labelTypes ").append(StringUtils.join(tags, " ")).append('\n');
        config.append("logging false\n");
        // config.append("inferenceMethod GREEDY\n");
        // config.append("normalizeTitleText false\n");
        // config.append("sortLexicallyFilesInFolders true\n");
        // config.append("thresholdPrediction false\n");
        // config.append("treatAllFilesInFolderAsOneBigDocument true\n");
        config.append("debug true\n");
        // # Features
        config.append("Forms 1\n");
        config.append("Capitalization 1\n");
        config.append("WordTypeInformation 1\n");
        config.append("Affixes 1\n");
        config.append("PreviousTag1 1\n");
        config.append("PreviousTag2 1\n");
        config.append("PreviousTagPatternLevel1 1\n");
        config.append("PreviousTagPatternLevel2 1\n");
        config.append("AggregateContext 0\n");
        config.append("AggregateGazetteerMatches 0\n");
        config.append("PrevTagsForContext 1\n");
        config.append("PredictionsLevel1 1\n");
        return config.toString();
    }

    @Override
    public String getModelFileEnding() {
        return "";
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return true;
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {
        try {
            File modelFile = new File(modelFilePath);
            if (modelFile.isDirectory() && !modelFile.mkdirs()) {
                throw new IllegalArgumentException("Could not create directory '" + modelFilePath + "'.");
            }
            Set<String> tags = FileFormatParser.getTagsFromColumnFile(trainingFilePath, "\t");
            LOGGER.debug("Available tags in training data: {}", tags);
            String configuration = buildConfig(modelFilePath, tags);
            LOGGER.debug("Configuration\n{}", configuration);
            File configFile = new File(modelFilePath, CONFIG_FILE_NAME);
            FileHelper.writeToFile(configFile.getPath(), configuration);
            Parameters.readConfigAndLoadExternalData(configFile.getPath(), true);
            File trainFile = FileHelper.getTempFile();
            FileFormatParser.columnToBracket(trainingFilePath, trainFile.getPath(), "\t");
            File testFile = trainFile; // XXX problem if training = testing? ask Lev who wrote the LBJ tagger
            LearningCurveMultiDataset.getLearningCurve(trainingRounds, trainFile.getPath(), testFile.getPath(), "-r");
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("Exception during training", e);
        }
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        Validate.isTrue(new File(configModelFilePath).isDirectory(),
                "The given path must point to a directory with the model files.");
        try {
            String configFilePath = new File(configModelFilePath, CONFIG_FILE_NAME).getPath();
            Parameters.readConfigAndLoadExternalData(configFilePath);
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("Exception when loading model from '" + configModelFilePath + "'.", e);
        }
    }

    @Override
    public List<Annotation> getAnnotations(String inputText) {
        try {
            File inputFile = FileHelper.getTempFile();
            File bracketFile = FileHelper.getTempFile();
            File xmlFile = FileHelper.getTempFile();
            FileHelper.writeToFile(inputFile.getPath(), inputText);
            NETagPlain.tagData(inputFile.getPath(), bracketFile.getPath(), false);
            FileFormatParser.bracketToXml(bracketFile.getPath(), xmlFile.getPath());
            String xmlText = FileHelper.readFileToString(xmlFile);
            String alignedXmlText = NerHelper.alignContentText(xmlText, inputText);
            return FileFormatParser.getAnnotationsFromXmlText(alignedXmlText);
        } catch (Exception e) {
            throw new IllegalStateException("Exception during tagging", e);
        }
    }

    @Override
    public String getName() {
        return "Illinois NER";
    }

}
