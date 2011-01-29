package tud.iir.extraction.entity.ner.tagger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

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
import tud.iir.extraction.entity.ner.evaluation.EvaluationResult;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StopWatch;
import de.julielab.jnet.tagger.JNETException;
import de.julielab.jnet.tagger.NETagger;
import de.julielab.jnet.tagger.Sentence;
import de.julielab.jnet.tagger.Tags;
import de.julielab.jnet.utils.Utils;

/**
 * <p>
 * This class wraps the Julie Named Entity Recognizer which uses conditional random fields.
 * </p>
 * 
 * <p>
 * The recognizer was trained on 3 bio-models on PennBioIE corpus (genes, malignancies, variation events), models are
 * available online. They are not part of the models distribution of this toolkit.
 * </p>
 * 
 * <p>
 * See also <a
 * href="http://www.julielab.de/Resources/Software/NLP+Tools/Download/Stand_alone+Tools.html">http://www.julielab
 * .de/Resources/Software/NLP+Tools/Download/Stand_alone+Tools.html</a>
 * </p>
 * 
 * <pre>
 * pos_feat_enabled = true
 * pos_feat_unit = pos
 * pos_feat_position = 1
 * pos_begin_flag = false
 * 
 * offset_conjunctions = (-1)(1)
 * 
 * gap_character = @
 * 
 * stemming_enabled = true
 * feat_wc_enabled = true
 * feat_bwc_enabled = true
 * feat_bioregexp_enabled = true
 * </pre>
 * 
 * @author David Urbansky
 * 
 */
public class JulieNER extends NamedEntityRecognizer {

    /** Hold the configuration settings here instead of a file. */
    private String configFileContent = "";

    public JulieNER() {
        setName("Julie NER");

        configFileContent += "pos_feat_enabled = false" + "\n";
        configFileContent += "pos_feat_unit = pos" + "\n";
        configFileContent += "pos_feat_position = 1" + "\n";
        configFileContent += "pos_begin_flag = false" + "\n";
        configFileContent += "offset_conjunctions = (-1)(1)" + "\n";
        configFileContent += "gap_character = @" + "\n";
        configFileContent += "stemming_enabled = true" + "\n";
        configFileContent += "feat_wc_enabled = true" + "\n";
        configFileContent += "feat_bwc_enabled = true" + "\n";
        configFileContent += "feat_bioregexp_enabled = true" + "\n";
    }

    public void demo() {
        String inputText = "John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. The iphone 4 is a mobile phone.";
        demo(inputText);
    }

    public void demo(String inputText) {
        // train
        train("data/datasets/ner/sample/trainingColumn.tsv", "data/temp/personPhoneCity.mod");

        // tag
        String taggedText = tag(inputText, "data/temp/personPhoneCity.mod.gz");
        System.out.println(taggedText);
    }

    @Override
    public String getModelFileEnding() {
        return "gz";
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return true;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        StopWatch stopWatch = new StopWatch();

        if (!configModelFilePath.endsWith("." + getModelFileEnding())) {
            configModelFilePath += "." + getModelFileEnding();
        }

        File modelFile = new File(configModelFilePath);

        NETagger tagger = new NETagger();

        try {
            tagger.readModel(modelFile.toString());
        } catch (Exception e) {
            LOGGER.error(getName() + " error in loading model: " + e.getMessage());
            return false;
        }

        setModel(tagger);
        LOGGER.info("model " + modelFile.toString() + " successfully loaded in " + stopWatch.getElapsedTimeString());

        return true;
    }

    @Override
    public Annotations getAnnotations(String inputText) {
        Annotations annotations = new Annotations();

        FileHelper.writeToFile("data/temp/julieInputText.txt", inputText);
        FileFormatParser.textToColumn("data/temp/julieInputText.txt", "data/temp/julieInputTextColumn.txt", " ");
        FileFormatParser.columnToSlash("data/temp/julieInputTextColumn.txt", "data/temp/julieTrainingSlash.txt", " ",
        "|");

        File testDataFile = new File("data/temp/julieTrainingSlash.txt");

        // TODO assign confidence values for predicted labels (see JNET documentation)
        boolean showSegmentConfidence = false;

        ArrayList<String> ppdTestData = Utils.readFile(testDataFile);
        ArrayList<Sentence> sentences = new ArrayList<Sentence>();

        NETagger tagger = (NETagger) getModel();

        for (String ppdSentence : ppdTestData) {
            try {
                sentences.add(tagger.PPDtoUnits(ppdSentence));
            } catch (JNETException e) {
                LOGGER.error(getName() + " error in creating annotations: " + e.getMessage());
            }
        } // tagger.readModel(modelFile.toString());
        File outFile = new File("data/temp/juliePredictionOutput.txt");
        try {

            Utils.writeFile(outFile, tagger.predictIOB(sentences, showSegmentConfidence));

            reformatOutput(outFile);

        } catch (Exception e) {
            LOGGER.error(getName() + " error in creating annotations: " + e.getMessage());
        }
        annotations = FileFormatParser.getAnnotationsFromXMLFile(outFile.getPath());

        FileHelper.writeToFile("data/test/ner/julieOutput.txt", tagText(inputText, annotations));
        // CollectionHelper.print(annotations);

        return annotations;
    }

    @Override
    public Annotations getAnnotations(String inputText, String configModelFilePath) {
        loadModel(configModelFilePath);
        return getAnnotations(inputText);
    }

    /**
     * The output of the named entity recognition is not well formatted and we need to reformat it.
     * 
     * @param file The file where the prediction output is written in BIO format. This file will be overwritten.
     */
    private void reformatOutput(File file) {

        // transform to XML
        FileFormatParser.columnToXML(file.getPath(), file.getPath(), "\t");

        String content = FileHelper.readFileToString(file);

        // =-
        content = content.replace("=- ", "=-");

        // O'Brien
        content = content.replaceAll("(?<= [A-Z])' (?=(\\<.{1,100}\\>)?[A-Z])", "'");


        content = content.replaceAll("(?<=[A-Z]\\</.{1,100}\\>)' (?=(\\<.{1,100}\\>)?[A-Z])", "'");
        
        // Tom's
        content = content.replaceAll("' [s|S](?=\\W)", "'s");

        // I'm
        content = content.replaceAll("' [m|M](?=\\W)", "'m");

        // won't
        content = content.replaceAll("' [t|T](?=\\W)", "'t");

        // they're
        content = content.replaceAll("' re(?=\\W)", "'re");

        // I've
        content = content.replaceAll("' ve(?=\\W)", "'ve");

        // we'll
        content = content.replaceAll("' ll(?=\\W)", "'ll");

        // x-based
        content = content.replace("- based", "-based");

        // @reuters
        content = content.replaceAll("@ (?=\\w)", "@");

        // @ 101
        content = content.replaceAll("@(?=\\d)", "@ ");

        // `
        content = content.replace("` ", "`");

        // Gama'a
        content = content.replace("Gama' a", "Gama'a");
        content = content.replace("Gama</PER>' a", "Gama</PER>'a");
        content = content.replace("' o.", "'o.");
        content = content.replace("' o ", "'o ");
        content = content.replace("</PER>' o", "</PER>'o");

        // d'a
        content = content.replace("d' a", "d'a");

        // +
        content = content.replace("+ ", "+");

        // 6/
        content = content.replaceAll("(?<=\\d)/ ", "/");

        // - 4
        content = content.replaceAll("(?<=(\\d|\\(|\\)))- (?=\\d+(\\s))", "-");

        // $ 6= 4
        content = content.replaceAll("(?<=\\d)= (?=\\d+(\\s))", "=");

        // 4:
        content = content.replaceAll("(?<=\\d): ", ":");

        // )-1
        // content = content.replaceAll("(?<=\\))- (?=\\d)", "-");
        FileHelper.writeToFile(file.getPath(), content);

    }

    /**
     * Create a file containing all entity types from the training file.
     * 
     * @param trainingFilePath
     * @return
     */
    private File createTagsFile(String trainingFilePath, String columnSeparator) {

        Set<String> tags = FileFormatParser.getTagsFromColumnFile(trainingFilePath, columnSeparator);

        StringBuilder tagsFile = new StringBuilder();
        for (String tag : tags) {
            tagsFile.append(tag).append("\n");
        }
        if (!tags.contains("O")) {
            tagsFile.append("O").append("\n");
        }

        FileHelper.writeToFile("data/temp/julieTags.txt", tagsFile);

        return new File("data/temp/julieTags.txt");
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {
        FileHelper.writeToFile("data/temp/julieNerConfig.config", configFileContent);
        return train(trainingFilePath, modelFilePath, "data/temp/julieNerConfig.config");
    }

    public boolean train(String trainingFilePath, String modelFilePath, String configFilePath) {

        String tempFilePath = "data/temp/julieTraining.txt";
        FileFormatParser.removeWhiteSpaceInFirstColumn(trainingFilePath, tempFilePath, "_");
        FileFormatParser.columnToSlash(tempFilePath, tempFilePath, "\t", "|");

        // put sentences on lines splitting on " .|O "
        String[] taggedSentences = FileHelper.readFileToString(tempFilePath).split(" \\.\\|O ");
        FileWriter fw;
        try {
            fw = new FileWriter(tempFilePath);
            for (String sentence : taggedSentences) {
                if (sentence.trim().length() > 0) {
                    fw.append(sentence).append(" .|O\n");
                    fw.flush();
                }
            }
            fw.close();
        } catch (IOException e1) {
            LOGGER.error(e1.getMessage());
        }

        File trainFile = new File(tempFilePath);
        File tagsFile = createTagsFile(trainingFilePath, "\t");

        // configFilePath = "config/defaultFeatureConf.conf";

        File featureConfigFile = null;
        if (configFilePath.length() > 0) {
            featureConfigFile = new File(configFilePath);
        }

        ArrayList<String> ppdSentences = Utils.readFile(trainFile);
        ArrayList<Sentence> sentences = new ArrayList<Sentence>();
        Tags tags = new Tags(tagsFile.toString());

        NETagger tagger;
        if (featureConfigFile != null) {
            tagger = new NETagger(featureConfigFile);
        } else {
            tagger = new NETagger();
        }
        for (String ppdSentence : ppdSentences) {
            try {
                sentences.add(tagger.PPDtoUnits(ppdSentence));
            } catch (JNETException e) {
                e.printStackTrace();
            }
        }
        tagger.train(sentences, tags);
        tagger.writeModel(modelFilePath);

        return true;
    }

    /**
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) throws Exception {

        JulieNER tagger = new JulieNER();

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
                    .withDescription("the text that should be tagged (only if mode = tag)").hasArg()
                    .withArgName("text").withType(String.class).create());

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

                    EvaluationResult evResult = tagger.evaluate(cmd.getOptionValue("trainingFile"),
                            cmd.getOptionValue("configFile"), TaggingFormat.XML);
                    System.out.println(evResult);

                } else if (cmd.hasOption("demo")) {

                    tagger.demo(cmd.getOptionValue("inputText"));

                }

            } catch (ParseException e) {
                LOGGER.debug("Command line arguments could not be parsed!");
                formatter.printHelp("JulieNER", options);
            }

        }

        // // HOW TO USE (some functions require the models in
        // data/models/juliener) ////
        // // train
        // tagger.train("data/datasets/ner/sample/trainingColumn.tsv", "data/temp/personPhoneCity.mod");

        // // tag
        // String taggedText = "";
        //
        // tagger.loadModel("data/temp/personPhoneCity.mod.gz");
        // taggedText = tagger
        // .tag("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. The iphone 4 is a mobile phone.");
        //
        // taggedText = tagger
        // .tag("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. The iphone 4 is a mobile phone.",
        // "data/temp/personPhoneCity.mod.gz");
        // System.out.println(taggedText);

        // // demo
        // tagger.demo();

        // // evaluate
        // System.out.println(tagger.evaluate("data/datasets/ner/sample/testingXML.xml",
        // "data/temp/personPhoneCity.mod.gz", TaggingFormat.XML));

        // String a =
        // "=-DOCSTART-|O EU|ORG rejects|O German|MISC call|O to|O boycott|O British|MISC lamb|O .|O Peter|PER Blackburn|PER";
        // String[] tokens = a.trim().split("[\t ]+");
        // String features[];
        // String label, word;
        // String featureName; // name of feature for units given by featureConfig
        // for (int i = 0; i < tokens.length; i++) {
        //
        // features = tokens[i].split("\\|+");
        //
        // word = features[0];
        // label = features[features.length - 1];
        // }
        //
        // FileHelper.writeToFile("data/temp/abc.txt", a);
        // ArrayList<String> ppdSentences = Utils.readFile(new File("data/temp/julieTraining.txt"));
        // ArrayList<Sentence> sentences = new ArrayList<Sentence>();
        //
        // NETagger tagger2;
        // tagger2 = new NETagger();
        //
        // for (String ppdSentence : ppdSentences) {
        // try {
        // sentences.add(tagger2.PPDtoUnits(ppdSentence));
        // } catch (JNETException e) {
        // e.printStackTrace();
        // }
        // }

        // /////////////////////////// train and test /////////////////////////////
        // using a column trainig and testing file
        tagger.train("data/test/ner/training.txt", "data/temp/juliener.mod");
        EvaluationResult er = tagger.evaluate("data/test/ner/test.txt", "data/temp/juliener.mod",
                TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());

        // tagger.train("data/datasets/ner/politician/text/training.tsv", "data/temp/juliener.mod");
        // EvaluationResult er = tagger.evaluate("data/datasets/ner/politician/text/testing.tsv",
        // "data/temp/juliener.mod", TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

    }

}