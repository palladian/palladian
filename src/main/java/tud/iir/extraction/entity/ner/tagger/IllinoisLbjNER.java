package tud.iir.extraction.entity.ner.tagger;

import java.io.IOException;
import java.util.StringTokenizer;

import lbj.NETaggerLevel1;
import lbj.NETaggerLevel2;
import ner.lbj.IO.Keyboard;
import ner.lbj.LbjTagger.BracketFileManager;
import ner.lbj.LbjTagger.DemoEngine;
import ner.lbj.LbjTagger.LearningCurve;
import ner.lbj.LbjTagger.NETagPlain;
import ner.lbj.LbjTagger.NETester;
import ner.lbj.LbjTagger.Parameters;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import tud.iir.extraction.entity.ner.Annotations;
import tud.iir.extraction.entity.ner.FileFormatParser;
import tud.iir.extraction.entity.ner.NamedEntityRecognizer;
import tud.iir.extraction.entity.ner.TaggingFormat;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.FileHelper;
import LBJ2.classify.Classifier;

/**
 * <p>
 * This class wraps the Learning Java Based Illinois Named Entity Tagger. The implementation is in an external library
 * and the approach is explained in the following paper by L. Ratinov and D. Roth:<br>
 * "Design Challenges and Misconceptions in Named Entity Recognition", CoNLL 2009
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
public class IllinoisLbjNER extends NamedEntityRecognizer {

    public IllinoisLbjNER() {
        setName("Lbj NER");
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
            int len = 0;
            StringTokenizer st = new StringTokenizer(res);
            StringBuffer output = new StringBuffer();
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                output.append(" " + s);
                len += s.length();
            }
            System.out.println(BracketFileManager.replaceSubstring(output.toString(), "\n", " "));
        }
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {

        Parameters.readConfigAndLoadExternalData(modelFilePath);
        Parameters.forceNewSentenceOnLineBreaks = true;

        String trainingFilePath2 = trainingFilePath.replaceAll("\\.", "_tranformed.");
        FileFormatParser.tsvToSsv(trainingFilePath, trainingFilePath2);

        // TODO is it a problem if training = testing? ask Lev who wrote the Lbj tagger
        String testingFilePath = trainingFilePath;

        String testingFilePath2 = testingFilePath.replaceAll("\\.", "_tranformed.");
        FileFormatParser.tsvToSsv(testingFilePath, testingFilePath2);

        // trainingFilePath2 = "data/temp/reuters2003.tsv";
        // testingFilePath2 = "data/temp/reuters2003.tsv";
        LearningCurve.getLearningCurve(trainingFilePath2, testingFilePath2);

        return true;
    }

    @Override
    public Annotations getAnnotations(String inputText, String configModelFilePath) {

        Parameters.readConfigAndLoadExternalData(configModelFilePath);
        Parameters.forceNewSentenceOnLineBreaks = true;

        String inputTextPath = "data/temp/lbj/inputText.txt";
        FileHelper.writeToFile(inputTextPath, inputText);

        String taggedFilePath = inputTextPath.replaceAll("\\.txt", "_tagged.txt");

        // last parameter is debug mode
        NETagPlain.tagFile(inputTextPath, taggedFilePath, false);

        FileFormatParser.bracketToXML(taggedFilePath, taggedFilePath);
        Annotations annotations = FileFormatParser.getAnnotationsFromXMLFile(taggedFilePath);

        CollectionHelper.print(annotations);

        return annotations;
    }

    @Deprecated
    public void trainNER(String trainingFilePath, String testingFilePath, boolean forceSentenceSplitsOnNewLines,
            String configFilePath) {
        Parameters.readConfigAndLoadExternalData(configFilePath);
        Parameters.forceNewSentenceOnLineBreaks = forceSentenceSplitsOnNewLines;

        FileFormatParser ffp = new FileFormatParser();
        String trainingFilePath2 = trainingFilePath.replaceAll("\\.", "_tranformed.");
        ffp.tsvToSsv(trainingFilePath, trainingFilePath2);

        String testingFilePath2 = testingFilePath.replaceAll("\\.", "_tranformed.");
        ffp.tsvToSsv(testingFilePath, testingFilePath2);

        // trainingFilePath2 = "data/temp/reuters2003.tsv";
        // testingFilePath2 = "data/temp/reuters2003.tsv";
        LearningCurve.getLearningCurve(trainingFilePath2, testingFilePath2);
    }

    public void testNER(String testingFilePath, boolean forceSentenceSplitsOnNewLines,
            String configFilePath) {

        Parameters.readConfigAndLoadExternalData(configFilePath);
        Parameters.forceNewSentenceOnLineBreaks = forceSentenceSplitsOnNewLines;

        NETester.test(testingFilePath, "-c");
    }
    
    
    @Deprecated
    public void useLearnedNER(String inputText, boolean forceSentenceSplitsOnNewLines, String configFilePath) {

        Parameters.readConfigAndLoadExternalData(configFilePath);
        Parameters.forceNewSentenceOnLineBreaks = forceSentenceSplitsOnNewLines;

        String inputTextPath = "data/temp/lbj/inputText.txt";
        FileHelper.writeToFile(inputTextPath, inputText);

        String taggedFilePath = inputTextPath.replaceAll("\\.txt", "_tagged.txt");

        // last parameter is debug mode
        NETagPlain.tagFile(inputTextPath, taggedFilePath, false);

        FileFormatParser ffp = new FileFormatParser();
        ffp.bracketToXML(taggedFilePath, taggedFilePath);
        Annotations annotations = ffp.getAnnotationsFromXMLFile(taggedFilePath);

        CollectionHelper.print(annotations);
    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        IllinoisLbjNER tagger = new IllinoisLbjNER();

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
        // lbt.trainNER("data/temp/allColumnBIO.tsv", "data/temp/allColumnBIO.tsv", true,
        // "data/temp/lbj/baselineFeatures.config");
        // TODO repack lbj sources because of absolute paths that are still set in there
        // tagger.train("data/datasets/ner/sample/trainingColumn.tsv",
        // "data/models/illinoisner/baselineFeatures.config");
        //
        // // use
        // lbt.useLearnedNER(
        // "John J. Smith and the Nexus One location iphone 4 mention Seattle in the text John J. Smith lives in Seattle.",
        // true, "data/temp/lbj/baselineFeatures.config"); // allLayer1.config
        //
        // // evaluate
        // //lbt.evaluateNER("data/temp/ne-esp-muc6.model", "data/temp/esp.testb");

    }

}
