package ws.palladian.extraction.entity.tagger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import lbj.NETaggerLevel1;
import lbj.NETaggerLevel2;
import ws.palladian.external.lbj.IO.Keyboard;
import ws.palladian.external.lbj.Tagger.BracketFileManager;
import ws.palladian.external.lbj.Tagger.DemoEngine;
import ws.palladian.external.lbj.Tagger.LearningCurve;
import ws.palladian.external.lbj.Tagger.NETagPlain;
import ws.palladian.external.lbj.Tagger.NETester;
import ws.palladian.external.lbj.Tagger.Parameters;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.TrainableNamedEntityRecognizer;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.processing.features.Annotated;
import LBJ2.classify.Classifier;

import com.ibm.icu.util.StringTokenizer;

/**
 * <p>
 * This class wraps the Learning Java Based Illinois Named Entity Tagger. It uses conditional random fields for tagging.
 * The implementation is in an external library and the approach is explained in the following paper by L. Ratinov and
 * D. Roth: "Design Challenges and Misconceptions in Named Entity Recognition", CoNLL 2009.
 * </p>
 * 
 * <p>
 * Changes to the original source (repackaged in tud.iir.external.lbjEdited-1.2):
 * <ul>
 * <li>changed file path in BrownClusters.java on line 29 to
 * data/models/illinoisner/data/BrownHierarchicalWordClusters/brownBllipClusters</li>
 * <li>changed file path in Parameters.java on line 52 to data/models/illinoisner/data/knownLists</li>
 * <li>added SimpleColumnParser</li>
 * <li>changed getLearningCurve() in LbjTagger.LearningCurve</li>
 * <li>changed NETagger</li>
 * <li>added .lc file to jar</li>
 * </ul>
 * </p>
 * 
 * <p>
 * See also <a
 * href="http://l2r.cs.uiuc.edu/~cogcomp/asoftware.php?skey=FLBJNE">http://l2r.cs.uiuc.edu/~cogcomp/asoftware
 * .php?skey=FLBJNE</a>
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class IllinoisLbjNer extends TrainableNamedEntityRecognizer {

    /** The default number of training rounds. */
    public static final int DEFAULT_TRAINING_ROUNDS = 20;

    /** Number of rounds for training. */
    private final int trainingRounds;

    /** Set this true if you evaluate on the CoNLL 2003 corpus. */
    private final boolean conllEvaluation;

    public IllinoisLbjNer(int trainingRounds, boolean conllEvaluation) {
        this.trainingRounds = trainingRounds;
        this.conllEvaluation = conllEvaluation;
    }

    public IllinoisLbjNer() {
        this(DEFAULT_TRAINING_ROUNDS, false);
    }

    private String buildConfigFile(int trainingRounds, String modelFile) {
        StringBuilder configFileContent = new StringBuilder();
        configFileContent.append("BIO").append("\n");
        configFileContent.append(modelFile).append("\n");
        configFileContent.append("DualTokenizationScheme").append("\n");
        configFileContent.append("rounds\t").append(trainingRounds).append("\n");
        configFileContent.append("GazetteersFeatures\t0").append("\n");
        configFileContent.append("Forms\t1").append("\n");
        configFileContent.append("Capitalization\t1").append("\n");
        configFileContent.append("WordTypeInformation\t1").append("\n");
        configFileContent.append("Affixes\t1").append("\n");
        configFileContent.append("PreviousTag1\t1").append("\n");
        configFileContent.append("PreviousTag2\t1").append("\n");
        // if BrownClusterPaths = 1, the brown models must be at
        // data/models/illinoisner/data/BrownHierarchicalWordClusters/brownBllipClusters
        configFileContent.append("BrownClusterPaths\t1").append("\n");
        configFileContent.append("NEShapeTaggerFeatures\t0").append("\n");
        configFileContent.append("aggregateContext\t1").append("\n");
        configFileContent.append("aggregateGazetteerMatches\t1").append("\n");
        configFileContent.append("prevTagsForContext\t1").append("\n");
        configFileContent.append("PatternFeatures\t1").append("\n");
        configFileContent.append("PredictionsLevel1\t1").append("\n");
        return configFileContent.toString();
    }

    public void demo(boolean forceSentenceSplitsOnNewLines, String configFilePath) throws IOException {

        Parameters.readConfigAndLoadExternalData(configFilePath);
        Parameters.forceNewSentenceOnLineBreaks = forceSentenceSplitsOnNewLines;

        System.out.println("loading the tagger");
        NETaggerLevel1 tagger1 = new NETaggerLevel1();
        tagger1 = (NETaggerLevel1)Classifier.binaryRead(Parameters.pathToModelFile + ".level1");
        NETaggerLevel2 tagger2 = new NETaggerLevel2();
        tagger2 = (NETaggerLevel2)Classifier.binaryRead(Parameters.pathToModelFile + ".level2");
        System.out.println("Done- loading the tagger");
        String input = "";
        while (true) {
            input = Keyboard.readLine();
            if (input.startsWith(" **")) {
                Parameters.forceNewSentenceOnLineBreaks = false;
                input = input.substring(3);
            }
            if (input.startsWith(" *true*")) {
                Parameters.forceNewSentenceOnLineBreaks = true;
                input = input.substring(" *true*".length());
            }
            input = BracketFileManager.replaceSubstring(input, "*newline*", "\n");
            String res = DemoEngine.tagLine(input, tagger1, tagger2);
            StringTokenizer st = new StringTokenizer(res);
            StringBuffer output = new StringBuffer();
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                output.append(" " + s);
            }
            System.out.println(BracketFileManager.replaceSubstring(output.toString(), "\n", " "));
        }
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

        // set the location to the training and the model file in the configs and save the file
        String configFileContent = buildConfigFile(trainingRounds, modelFilePath);
        FileHelper.writeToFile("data/temp/illinoislbjNerConfig.config", configFileContent);

        // count the number of models
        int l1 = FileHelper.getFiles(FileHelper.getFilePath(modelFilePath)).length;

        Parameters.readConfigAndLoadExternalData("data/temp/illinoislbjNerConfig.config");
        Parameters.forceNewSentenceOnLineBreaks = true;

        String trainingFilePath2 = trainingFilePath.replaceAll("\\.", "_tranformed.");
        FileFormatParser.tsvToSsv(trainingFilePath, trainingFilePath2);

        FileFormatParser.columnToColumnBio(trainingFilePath2, trainingFilePath2, " ");

        // TODO is it a problem if training = testing? ask Lev who wrote the Lbj tagger
        // String testingFilePath = FileHelper.appendToFileName(trainingFilePath, "_");
        //
        // String testingFilePath2 = testingFilePath.replaceAll("\\.", "_tranformed.");
        // FileFormatParser.tsvToSsv(testingFilePath, testingFilePath2);
        String testingFilePath2 = trainingFilePath2;

        // a new model is only added if precision and recall are not 0 or 100
        LearningCurve.getLearningCurve(trainingFilePath2, testingFilePath2);

        // check if a new model has been added, if not return false
        boolean modelAdded = FileHelper.getFiles(FileHelper.getFilePath(modelFilePath)).length > l1;
        return modelAdded;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        // set the location to the training and the model file in the configs and save the file
        String configFileContent = buildConfigFile(trainingRounds, configModelFilePath);
        FileHelper.writeToFile("data/temp/illinoislbjNerConfig.config", configFileContent);

        Parameters.readConfigAndLoadExternalData("data/temp/illinoislbjNerConfig.config");
        Parameters.forceNewSentenceOnLineBreaks = true;

        return true;
    }

    @Override
    public List<Annotated> getAnnotations(String inputText) {

        String inputTextPath = "data/temp/illinoisInputText.txt";
        FileHelper.writeToFile(inputTextPath, inputText);

        String taggedFilePath = inputTextPath.replaceAll("\\.txt", "_tagged.txt");

        // last parameter is debug mode
        NETagPlain.tagFile(inputTextPath, taggedFilePath, false);

        final StringBuilder outputBuffer = new StringBuilder();
        FileHelper.performActionOnEveryLine(taggedFilePath, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                outputBuffer.append(line.substring(0, line.length() - 3) + ". ");
            }
        });

        String bracketOutput = outputBuffer.toString();

        if (conllEvaluation) {
            bracketOutput = clean(bracketOutput);
        }

        String xmlOutput = FileFormatParser.bracketToXmlText(bracketOutput);
        String xmlOutputAligned = NerHelper.alignContentText(xmlOutput, inputText);
        List<ContextAnnotation> annotations = FileFormatParser.getAnnotationsFromXmlText(xmlOutputAligned);

        FileHelper.writeToFile("data/test/ner/illinoisOutput.txt", tagText(inputText, annotations));

        return Collections.<Annotated> unmodifiableList(annotations);
    }

    /**
     * <p>
     * Re-transform something the tokenizer might have destroyed. XXX this is only for the CoNLL corpus so far and does
     * not even cover everything needed.
     * </p>
     * 
     * @param content The content to re-transform.
     * @return The re-transformed content.
     */
    private String clean(String content) {
        content = content.replace(".\"", ".''");
        content = content.replace(":\"", ":''");
        content = content.replace(",\"", ",''");
        return content;
    }

    public void testNER(String testingFilePath, boolean forceSentenceSplitsOnNewLines, String configFilePath) {

        Parameters.readConfigAndLoadExternalData(configFilePath);
        Parameters.forceNewSentenceOnLineBreaks = forceSentenceSplitsOnNewLines;

        NETester.test(testingFilePath, "-c");
    }

    @Override
    public String getName() {
        return "Lbj NER";
    }

    public static void main(String[] args) {

        IllinoisLbjNer tagger = new IllinoisLbjNer();

        // lbt.demo(true, "data/temp/lbj/baselineFeatures.config");

        // learn
        // lbt.trainNER("data/temp/esp.train", "data/temp/esp.testa", true, "data/temp/lbj/baselineFeatures.config");

        // lbt.trainNER("data/temp/allColumnBIO.tsv", "data/temp/allColumnBIO_test.tsv",
        // true,"data/temp/lbj/baselineFeatures.config");
        // lbt.trainNER("data/temp/allColumnBIO.tsv", "data/temp/allColumnBIO_test.tsv",
        // true,"data/temp/lbj/baselineFeatures.config");

        // // HOW TO USE ////
        // tagger.loadModel("data/models/illinoisner/baselineFeatures.config");
        // tagger.tag("John J. Smith and the Nexus One location iphone 4 mention Seattle in the text John J. Smith lives in Seattle.");
        // tagger.tag("John J. Smith and the Nexus One location iphone 4 mention Seattle in the text John J. Smith lives in Seattle.");

        // tagger.train("data/datasets/ner/sample/trainingColumn.tsv",
        // "data/models/illinoisner/baselineFeatures2.config");
        //
        // // use
        // tagger.tag("John J. Smith and the Nexus One location iphone 4 mention Seattle in the text John J. Smith lives in Seattle.","data/models/illinoisner/baselineFeatures2.config");
        // // allLayer1.config
        //
        // // evaluate
        // //lbt.evaluateNER("data/temp/ne-esp-muc6.model", "data/temp/esp.testb");

        // String a = "abc [MISC A$  ] def";
        // a = a.replace("[MISC A$  ]", "<MISC>A$</MISC>");
        //
        // FileFormatParser.bracketToXML("data/temp/illinoisInputText_tagged.txt",
        // "data/temp/illinoisInputText_tagged.txt");
        // System.exit(0);

        // using a column trainig and testing file
        // tagger.train("data/datasets/ner/conll/training.txt", "data/temp/lbj.model");
        tagger = new IllinoisLbjNer(DEFAULT_TRAINING_ROUNDS, true);
        tagger.loadModel("data/temp/lbj.model");
        EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_final.txt", TaggingFormat.COLUMN);

        // tagger.train("C:\\My Dropbox\\taggedHierarchicalPrepared_train.txt", "data/temp/lbj2.model");
        // EvaluationResult er = tagger.evaluate("C:\\My Dropbox\\taggedHierarchicalPrepared_test.txt",
        // "data/temp/lbj2.model", TaggingFormat.COLUMN);

        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());

        // Dataset trainingDataset = new Dataset();
        // trainingDataset.setPath("data/datasets/ner/www_test/index_split1.txt");
        // tagger.train(trainingDataset, "data/temp/illinoislbjner." + tagger.getModelFileEnding());
        //
        // Dataset testingDataset = new Dataset();
        // testingDataset.setPath("data/datasets/ner/www_test/index_split2.txt");
        // EvaluationResult er = tagger.evaluate(testingDataset,
        // "data/temp/illinoislbjner." + tagger.getModelFileEnding());
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

    }

}
