package ws.palladian.extraction.entity.tagger;

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

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
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
public class JulieNer extends NamedEntityRecognizer {

    /** Hold the configuration settings here instead of a file. */
    private String configFileContent = "";

    private NETagger tagger;

    public JulieNer() {
        setName("Julie NER");

        // alignContent(new File("data/temp/t.TXT"), "THURSDAY'S GAMES. NEW YORK");
        // alignContent(new File("data/temp/t.TXT"), "MOODY'S: Aaa");
        // alignContent(
        // new File("data/temp/t.TXT"),
        // "Jan Sergi Barjuan 40-1 Rafael Alkorta 33-1 Albert Ferrer 40-1 Chendo Porlan 33-1 Miguel Nadal 40-1 Laurent Blanc=-DOCSTART- SOCCER");
        
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

        this.tagger = tagger;
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

        for (String ppdSentence : ppdTestData) {
            try {
                sentences.add(tagger.PPDtoUnits(ppdSentence));
                // tagger.predict(tagger.PPDtoUnits(ppdSentence), showSegmentConfidence);
            } catch (JNETException e) {
                LOGGER.error(getName() + " error in creating annotations: " + e.getMessage());
            }
        } // tagger.readModel(modelFile.toString());
        File outFile = new File("data/temp/juliePredictionOutput.txt");
        try {

            ArrayList<String> predictIOB = tagger.predictIOB(sentences, showSegmentConfidence);
            Utils.writeFile(outFile, predictIOB);
            Utils.writeFile(new File("data/temp/juliePredictionOutput_original.txt"), predictIOB);

            // reformatOutput(outFile);
            alignContent(outFile, inputText);

        } catch (Exception e) {
            LOGGER.error(getName() + " error in creating annotations: " + e.getMessage());
        }
        annotations = FileFormatParser.getAnnotationsFromXmlFile(outFile.getPath());

        annotations.instanceCategoryToClassified();

        FileHelper.writeToFile("data/test/ner/julieOutput.txt", tagText(inputText, annotations));

        return annotations;
    }

    @Override
    public Annotations getAnnotations(String inputText, String configModelFilePath) {
        loadModel(configModelFilePath);
        return getAnnotations(inputText);
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

        JulieNer tagger = new JulieNer();

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
        // tagger.train("data/datasets/ner/conll/training.txt", "data/temp/juliener.mod");
        // EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_final.txt",
        // "data/temp/juliener.mod",
        // TaggingFormat.COLUMN);

        // EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_final.txt",
        // "data/temp/nerEvaluation/Julie NER_nerModel_2..gz",
        // TaggingFormat.COLUMN);

        //tagger.train("data/datasets/ner/tud/tud2011_train.txt", "data/temp/julieNER2.model");
        tagger.train("data/datasets/ner/conll/training_verysmall.txt", "data/temp/julieNER.model");
        EvaluationResult er = tagger.evaluate("data/datasets/ner/tud/tud2011_test.txt",
                "data/temp/julieNER.model", TaggingFormat.COLUMN);

        // tagger.train("data/datasets/ner/conll/training_small.txt", "data/temp/juliener_small.mod");
        // EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_validation.txt",
        // "data/temp/juliener_small.mod", TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());

        // tagger.train("data/datasets/ner/politician/text/training.tsv", "data/temp/juliener.mod");
        // EvaluationResult er = tagger.evaluate("data/datasets/ner/politician/text/testing.tsv",
        // "data/temp/juliener.mod", TaggingFormat.COLUMN);
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

    }

}