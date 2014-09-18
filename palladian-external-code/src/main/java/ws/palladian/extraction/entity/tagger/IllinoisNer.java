package ws.palladian.extraction.entity.tagger;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.Validate;

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

    /** The default number of training rounds. */
    public static final int DEFAULT_TRAINING_ROUNDS = 20;

    /** Number of rounds for training. */
    private final int trainingRounds;

    public IllinoisNer(int trainingRounds) {
        Validate.isTrue(trainingRounds > 0, "trainingRounds must be greater zero");
        this.trainingRounds = trainingRounds;
    }

    public IllinoisNer() {
        this(DEFAULT_TRAINING_ROUNDS);
    }

    private static String buildConfig(String modelFile) {
        StringBuilder config = new StringBuilder();
        // # Required fields
        config.append("configFilename config").append('\n');
        config.append("pathToModelFile ").append(modelFile).append('\n');
        config.append("taggingEncodingScheme BIO\n");
        config.append("tokenizationScheme LbjTokenizationScheme\n");
        // # Optional fields
        config.append("beamSize 5\n");
        config.append("forceNewSentenceOnLineBreaks true\n");
        config.append("logging false\n");
        config.append("inferenceMethod GREEDY\n");
        config.append("normalizeTitleText false\n");
        config.append("sortLexicallyFilesInFolders true\n");
        config.append("thresholdPrediction false\n");
        config.append("treatAllFilesInFolderAsOneBigDocument true\n");
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
        return "model";
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return false;
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {
        try {
            File modelFile = new File(modelFilePath);
            if (modelFile.isDirectory() && !modelFile.mkdirs()) {
                throw new IllegalArgumentException("Could not create directory '" + modelFilePath + "'.");
            }
            String configuration = buildConfig(modelFilePath);
            File configFile = FileHelper.getTempFile();
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
            String configFileContent = buildConfig(configModelFilePath);
            File configFile = FileHelper.getTempFile();
            FileHelper.writeToFile(configFile.getPath(), configFileContent);
            Parameters.readConfigAndLoadExternalData(configFile.getPath());
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
        return "Lbj NER";
    }

}
