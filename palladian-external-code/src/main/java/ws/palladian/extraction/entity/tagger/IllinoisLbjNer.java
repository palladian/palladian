package ws.palladian.extraction.entity.tagger;

import java.io.FileWriter;
import java.io.IOException;

import lbj.NETaggerLevel1;
import lbj.NETaggerLevel2;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import ws.palladian.external.lbj.IO.Keyboard;
import ws.palladian.external.lbj.Tagger.BracketFileManager;
import ws.palladian.external.lbj.Tagger.DemoEngine;
import ws.palladian.external.lbj.Tagger.LearningCurve;
import ws.palladian.external.lbj.Tagger.NETagPlain;
import ws.palladian.external.lbj.Tagger.NETester;
import ws.palladian.external.lbj.Tagger.Parameters;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import LBJ2.classify.Classifier;

import com.ibm.icu.util.StringTokenizer;

/**
 * <p>
 * This class wraps the Learning Java Based Illinois Named Entity Tagger. It uses conditional random fields for tagging.
 * The implementation is in an external library and the approach is explained in the following paper by L. Ratinov and
 * D. Roth:<br>
 * "Design Challenges and Misconceptions in Named Entity Recognition", CoNLL 2009
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
 * 
 */
public class IllinoisLbjNer extends NamedEntityRecognizer {

    /** Hold the configuration settings here instead of a file. */
    private String configFileContent = "";

    /** Number of rounds for training. */
    private int trainingRounds = 20;

    /** Set this true if you evaluate on the CoNLL 2003 corpus. */
    private boolean conllEvaluation = false;

    public IllinoisLbjNer() {
        setName("Lbj NER");
        buildConfigFile();
    }

    private void buildConfigFile() {
        configFileContent = "";
        configFileContent += "BIO" + "\n";
        configFileContent += "###MODEL_FILE###" + "\n";
        configFileContent += "DualTokenizationScheme" + "\n";
        configFileContent += "rounds\t" + getTrainingRounds() + "\n";
        configFileContent += "GazetteersFeatures\t0" + "\n";
        configFileContent += "Forms\t1" + "\n";
        configFileContent += "Capitalization\t1" + "\n";
        configFileContent += "WordTypeInformation\t1" + "\n";
        configFileContent += "Affixes\t1" + "\n";
        configFileContent += "PreviousTag1\t1" + "\n";
        configFileContent += "PreviousTag2\t1" + "\n";
        // if BrownClusterPaths = 1, the brown models must be at
        // data/models/illinoisner/data/BrownHierarchicalWordClusters/brownBllipClusters
        configFileContent += "BrownClusterPaths\t1" + "\n";
        configFileContent += "NEShapeTaggerFeatures\t0" + "\n";
        configFileContent += "aggregateContext\t1" + "\n";
        configFileContent += "aggregateGazetteerMatches\t1" + "\n";
        configFileContent += "prevTagsForContext\t1" + "\n";
        configFileContent += "PatternFeatures\t1" + "\n";
        configFileContent += "PredictionsLevel1\t1" + "\n";
    }

    public int getTrainingRounds() {
        return trainingRounds;
    }

    public void setTrainingRounds(int trainingRounds) {
        this.trainingRounds = trainingRounds;
        buildConfigFile();
    }

    public void demo(boolean forceSentenceSplitsOnNewLines, String configFilePath) throws IOException {

        Parameters.readConfigAndLoadExternalData(configFilePath);
        Parameters.forceNewSentenceOnLineBreaks = forceSentenceSplitsOnNewLines;

        System.out.println("loading the tagger");
        NETaggerLevel1 tagger1 = new NETaggerLevel1();
        tagger1 = (NETaggerLevel1) Classifier.binaryRead(Parameters.pathToModelFile + ".level1");
        NETaggerLevel2 tagger2 = new NETaggerLevel2();
        tagger2 = (NETaggerLevel2) Classifier.binaryRead(Parameters.pathToModelFile + ".level2");
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
        buildConfigFile();
        configFileContent = configFileContent.replaceAll("###MODEL_FILE###", modelFilePath);
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
        if (FileHelper.getFiles(FileHelper.getFilePath(modelFilePath)).length == l1) {
            return false;
        }

        return true;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        // set the location to the training and the model file in the configs and save the file
        configFileContent = configFileContent.replaceAll("###MODEL_FILE###", configModelFilePath);
        FileHelper.writeToFile("data/temp/illinoislbjNerConfig.config", configFileContent);

        Parameters.readConfigAndLoadExternalData("data/temp/illinoislbjNerConfig.config");
        Parameters.forceNewSentenceOnLineBreaks = true;

        setModel(new Object());

        return true;
    }

    @Override
    public Annotations getAnnotations(String inputText) {

        String inputTextPath = "data/temp/illinoisInputText.txt";
        FileHelper.writeToFile(inputTextPath, inputText);

        String taggedFilePath = inputTextPath.replaceAll("\\.txt", "_tagged.txt");

        // last parameter is debug mode
        NETagPlain.tagFile(inputTextPath, taggedFilePath, false);

        // transform text back to online line since the tagger puts one sentence on each line
        String taggedFilePathTransformed = inputTextPath.replaceAll("\\.txt", "_tagged_transformed.txt");

        try {
            final FileWriter fileWriter = new FileWriter(taggedFilePathTransformed);

            LineAction la = new LineAction() {

                @Override
                public void performAction(String line, int lineNumber) {
                    try {
                        line = line.substring(0, line.length() - 3) + ". ";
                        fileWriter.write(line);
                        fileWriter.flush();
                    } catch (IOException e) {
                        LOGGER.error("could not write line, " + e.getMessage());
                    }
                }
            };

            FileHelper.performActionOnEveryLine(taggedFilePath, la);
            fileWriter.close();

        } catch (IOException e) {
            LOGGER.error("could not transform tagged text, " + e.getMessage());
        }

        if (isConllEvaluation()) {
            cleanFile(taggedFilePathTransformed);
        }

        // FileFormatParser.bracketToXML(taggedFilePathTransformed, taggedFilePathTransformed);
        FileFormatParser.bracketToColumn(taggedFilePathTransformed, taggedFilePathTransformed, "\t");

        alignContent(taggedFilePathTransformed, inputText);

        Annotations annotations = FileFormatParser.getAnnotationsFromXmlFile(taggedFilePathTransformed);

        annotations.instanceCategoryToClassified();

        FileHelper.writeToFile("data/test/ner/illinoisOutput.txt", tagText(inputText, annotations));

        return annotations;
    }

    /**
     * Retransform something the tokenizer might have destroyed. XXX this is only for the conll corpus so far and does
     * not even cover everything needed
     * 
     * @param taggedFilePath The path of the tagged file.
     */
    private void cleanFile(String taggedFilePath) {
        String content = FileHelper.readFileToString(taggedFilePath);
        content = content.replace(".\"", ".''");
        content = content.replace(":\"", ":''");
        content = content.replace(",\"", ",''");
        FileHelper.writeToFile(taggedFilePath, content);
    }

    @Override
    public Annotations getAnnotations(String inputText, String configModelFilePath) {
        loadModel(configModelFilePath);
        return getAnnotations(inputText);
    }

    public void testNER(String testingFilePath, boolean forceSentenceSplitsOnNewLines,
            String configFilePath) {

        Parameters.readConfigAndLoadExternalData(configFilePath);
        Parameters.forceNewSentenceOnLineBreaks = forceSentenceSplitsOnNewLines;

        NETester.test(testingFilePath, "-c");
    }

    public void setConllEvaluation(boolean conllEvaluation) {
        this.conllEvaluation = conllEvaluation;
    }

    public boolean isConllEvaluation() {
        return conllEvaluation;
    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        IllinoisLbjNer tagger = new IllinoisLbjNer();

        // lbt.demo(true, "data/temp/lbj/baselineFeatures.config");

        // learn
        // lbt.trainNER("data/temp/esp.train", "data/temp/esp.testa", true, "data/temp/lbj/baselineFeatures.config");

        // lbt.trainNER("data/temp/allColumnBIO.tsv", "data/temp/allColumnBIO_test.tsv",
        // true,"data/temp/lbj/baselineFeatures.config");
        // lbt.trainNER("data/temp/allColumnBIO.tsv", "data/temp/allColumnBIO_test.tsv",
        // true,"data/temp/lbj/baselineFeatures.config");

        if (args.length > 0) {

            Options options = new Options();
            options.addOption(OptionBuilder.withLongOpt("mode").withDescription("whether to tag or train a model")
                    .create());

            OptionGroup modeOptionGroup = new OptionGroup();
            modeOptionGroup.addOption(OptionBuilder.withArgName("tg").withLongOpt("tag").withDescription("tag a text")
                    .create());
            modeOptionGroup.addOption(OptionBuilder.withArgName("tr").withLongOpt("train")
                    .withDescription("train a model").create());
            modeOptionGroup.addOption(OptionBuilder.withArgName("ev").withLongOpt("evaluate")
                    .withDescription("evaluate a model").create());
            modeOptionGroup.addOption(OptionBuilder.withArgName("dm").withLongOpt("demo")
                    .withDescription("demo mode of the tagger").create());
            modeOptionGroup.setRequired(true);
            options.addOptionGroup(modeOptionGroup);

            options.addOption(OptionBuilder.withLongOpt("trainingFile")
                    .withDescription("the path and name of the training file for the tagger (only if mode = train)")
                    .hasArg().withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder
                    .withLongOpt("testFile")
                    .withDescription(
                    "the path and name of the test file for evaluating the tagger (only if mode = evaluate)")
                    .hasArg().withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder.withLongOpt("configFile")
                    .withDescription("the path and name of the config file for the tagger").hasArg()
                    .withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder.withLongOpt("inputText")
                    .withDescription("the text that should be tagged (only if mode = tag)")
                    .hasArg().withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder.withLongOpt("outputFile")
                    .withDescription("the path and name of the file where the tagged text should be saved to").hasArg()
                    .withArgName("text").withType(String.class).create());

            HelpFormatter formatter = new HelpFormatter();

            CommandLineParser parser = new PosixParser();
            CommandLine cmd = null;
            try {
                cmd = parser.parse(options, args);

                if (cmd.hasOption("tag")) {

                    String taggedText = tagger.tag(cmd.getOptionValue("inputText"), cmd.getOptionValue("configFile"));

                    if (cmd.hasOption("outputFile")) {
                        FileHelper.writeToFile(cmd.getOptionValue("outputFile"), taggedText);
                    } else {
                        System.out.println("No output file given so tagged text will be printed to the console:");
                        System.out.println(taggedText);
                    }

                } else if (cmd.hasOption("train")) {

                    tagger.train(cmd.getOptionValue("trainingFile"), cmd.getOptionValue("configFile"));

                } else if (cmd.hasOption("evaluate")) {

                    tagger.evaluate(cmd.getOptionValue("trainingFile"), cmd.getOptionValue("configFile"),
                            TaggingFormat.XML);

                } else if (cmd.hasOption("demo")) {

                    try {
                        tagger.demo(true, cmd.getOptionValue("configFile"));
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage());
                    }

                }


            } catch (ParseException e) {
                LOGGER.debug("Command line arguments could not be parsed!");
                formatter.printHelp("FeedChecker", options);
            }

        }

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
        tagger.setConllEvaluation(true);
        EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_final.txt", "data/temp/lbj.model",
                TaggingFormat.COLUMN);

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