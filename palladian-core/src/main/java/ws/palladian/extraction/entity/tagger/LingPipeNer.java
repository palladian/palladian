package ws.palladian.extraction.entity.tagger;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.TrainableNamedEntityRecognizer;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.tagger.helper.Conll2002ChunkTagParser;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.features.Annotated;

import com.aliasi.chunk.CharLmRescoringChunker;
import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.AbstractExternalizable;

/**
 * <p>
 * This class wraps the LingPipe implementation of a Named Entity Recognizer. We wrapped the slowest but most accurate
 * recognizer (CharLmRescoringChunker) since we are interested in the best possible results. LingPipe uses a
 * "long-distance character language model-based chunker that operates by resocring the output of a contained character
 * language model HMM chunker.
 * </p>
 * 
 * <p>
 * See also <a
 * href="http://alias-i.com/lingpipe/demos/tutorial/ne/read-me.html">http://alias-i.com/lingpipe/demos/tutorial
 * /ne/read-me.html</a>
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class LingPipeNer extends TrainableNamedEntityRecognizer {

    private static final int NUM_CHUNKINGS_RESCORED = 64;
    private static final int MAX_N_GRAM = 8;
    private static final int NUM_CHARS = 256;
    private static final double LM_INTERPOLATION = MAX_N_GRAM;

    private Chunker chunker;

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
            String trainingFilePath2 = trainingFilePath.replaceAll("\\.", "_tranformed.");

            FileFormatParser.removeWhiteSpaceInFirstColumn(trainingFilePath, trainingFilePath2, "_");
            FileFormatParser.tsvToSsv(trainingFilePath2, trainingFilePath2);

            FileFormatParser.columnToColumnBio(trainingFilePath2, trainingFilePath2, " ");

            File corpusFile = new File(trainingFilePath2);
            File modelFile = new File(modelFilePath);
            // File devFile = new File(developmentFilePath);

            LOGGER.info("setting up Chunker Estimator");
            TokenizerFactory factory = IndoEuropeanTokenizerFactory.INSTANCE;
            CharLmRescoringChunker chunkerEstimator = new CharLmRescoringChunker(factory, NUM_CHUNKINGS_RESCORED,
                    MAX_N_GRAM, NUM_CHARS, LM_INTERPOLATION);
            // HmmCharLmEstimator hmmEstimator = new
            // HmmCharLmEstimator(MAX_N_GRAM, NUM_CHARS, LM_INTERPOLATION);
            // CharLmHmmChunker chunkerEstimator = new CharLmHmmChunker(factory,
            // hmmEstimator);

            LOGGER.info("setting up Data Parser");
            // GeneTagParser parser = new GeneTagParser();
            Conll2002ChunkTagParser parser = new Conll2002ChunkTagParser();
            parser.setHandler(chunkerEstimator);

            LOGGER.info("training with data from file={}", corpusFile);
            parser.parse(corpusFile);

            // System.out.println("Training with Data from File=" + devFile);
            // parser.parse(devFile);

            LOGGER.info("compiling and writing model to file={}", modelFile);
            AbstractExternalizable.compileTo(chunkerEstimator, modelFile);

        } catch (IOException e) {
            LOGGER.error("{} failed training: {}", getName(), e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        StopWatch stopWatch = new StopWatch();

        File modelFile = new File(configModelFilePath);

        LOGGER.info("Reading chunker from file {}", modelFile);
        try {
            chunker = (Chunker)AbstractExternalizable.readObject(modelFile);
        } catch (Exception e) {
            LOGGER.error("{} error in loading model from {}: {}", new Object[] {getName(), modelFile, e.getMessage()});
            return false;
        }

        LOGGER.info("Model {} successfully loaded in {}", modelFile, stopWatch.getElapsedTimeString());
        return true;
    }

    @Override
    public List<Annotated> getAnnotations(String inputText) {
        Annotations<Annotated> annotations = new Annotations<Annotated>();

        String[] args = {inputText};
        Set<Chunk> chunkSet = new HashSet<Chunk>();
        for (int i = 0; i < args.length; ++i) {
            Chunking chunking = chunker.chunk(args[i]);
            LOGGER.debug("Chunking={}", chunking);
            chunkSet.addAll(chunking.chunkSet());
        }

        for (Chunk chunk : chunkSet) {
            int offset = chunk.start();
            String entityName = inputText.substring(offset, chunk.end());
            String tagName = chunk.type();
            annotations.add(new Annotation(offset, entityName, tagName));
        }

        // FileHelper.writeToFile("data/test/ner/lingPipeOutput.txt", tagText(inputText, annotations));
        // CollectionHelper.print(annotations);

        annotations.removeNested();
        annotations.sort();
        return annotations;
    }

    @Override
    public String getName() {
        return "LingPipe NER";
    }

    // public void evaluateNER(String modelFilePath, String testFilePath)
    // throws Exception {
    //
    // File chunkerFile = new File(modelFilePath);
    // File testFile = new File(testFilePath);
    //
    // @SuppressWarnings("rawtypes")
    // AbstractCharLmRescoringChunker<NBestChunker, Process, Sequence> chunker = (AbstractCharLmRescoringChunker)
    // AbstractExternalizable
    // .readObject(chunkerFile);
    //
    // ChunkerEvaluator evaluator = new ChunkerEvaluator(chunker);
    // evaluator.setVerbose(true);
    //
    // Conll2002ChunkTagParser parser = new Conll2002ChunkTagParser();
    // parser.setHandler(evaluator);
    //
    // parser.parse(testFile);
    //
    // System.out.println(evaluator.toString());
    // }
    //
    // public void scoreNER(String[] args) throws IOException {
    // File refFile = new File(args[0]);
    // File responseFile = new File(args[1]);
    //
    // Parser parser = new Muc6ChunkParser();
    // FileScorer scorer = new FileScorer(parser);
    // scorer.score(refFile, responseFile);
    //
    // System.out.println(scorer.evaluation().toString());
    // }

    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        LingPipeNer tagger = new LingPipeNer();

        // learn
        // lpt.trainNER("data/temp/esp.train", "data/temp/esp.testa",
        // "data/temp/ne-esp-muc6.model");

        // lpt.trainNER("data/temp/stanfordner/example/jane-austen-emma-ch1.tsv",
        // "","data/temp/ne-en-janeausten-lp.model");

        // lpt.trainNER("data/temp/allColumnBIO.tsv", "",
        // "data/temp/ne-en-mobilephone-lp.model");

        // use
        // lpt.useLearnedNER("data/temp/ne-en-news-muc6.AbstractCharLmRescoringChunker",
        // "John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle.");
        // lpt.useLearnedNER("data/temp/ne-esp-muc6.model",
        // "John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle.");

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

                    tagger.loadModel(cmd.getOptionValue("configFile"));
                    String taggedText = tagger.tag(cmd.getOptionValue("inputText"));

                    if (cmd.hasOption("outputFile")) {
                        FileHelper.writeToFile(cmd.getOptionValue("outputFile"), taggedText);
                    } else {
                        System.out.println("No output file given so tagged text will be printed to the console:");
                        System.out.println(taggedText);
                    }

                } else if (cmd.hasOption("train")) {

                    tagger.train(cmd.getOptionValue("trainingFile"), cmd.getOptionValue("configFile"));

                } else if (cmd.hasOption("evaluate")) {

                    tagger.loadModel(cmd.getOptionValue("configFile"));
                    tagger.evaluate(cmd.getOptionValue("trainingFile"), TaggingFormat.XML);

                }

            } catch (ParseException e) {
                LOGGER.debug("Command line arguments could not be parsed!");
                formatter.printHelp("FeedChecker", options);
            }

        }

        // // HOW TO USE ////
        // tagger
        // .tag(
        // "John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. The iphone 4 is a mobile phone.",
        // "data/models/lingpipe/data/ne-en-mobilephone-lp.model");
        //
        // tagger.useLearnedNER(
        // "data/temp/ne-en-mobilephone-lp.model",
        // "John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. The iphone 4 is a mobile phone.");
        // evaluate
        // lpt.evaluateNER("data/temp/ne-esp-muc6.model",
        // "data/temp/esp.testb");

        // using a column trainig and testing file
        // tagger.train("data/temp/nerEvaluation/www_eval_2_cleansed/allColumn.txt", "data/temp/lingPipeNER.model");
        tagger.train("data/datasets/ner/conll/training.txt", "data/temp/lingPipeNER.model");
        tagger.loadModel("data/temp/lingPipeNER.model");
        EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_final.txt", TaggingFormat.COLUMN);

        // tagger.train("C:\\My Dropbox\\taggedHierarchicalPrepared_train.txt", "data/temp/lingPipeNER2.model");
        // EvaluationResult er = tagger.evaluate("C:\\My Dropbox\\taggedHierarchicalPrepared_test.txt",
        // "data/temp/lingPipeNER2.model", TaggingFormat.COLUMN);

        // EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_final.txt",
        // "data/temp/lingPipeNER_.model",
        // TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());

        // using a dataset
        // Dataset trainingDataset = new Dataset();
        // trainingDataset.setPath("data/datasets/ner/www_test/index_split1.txt");
        // tagger.train(trainingDataset, "data/temp/lingpipe.model");
        //
        // Dataset testingDataset = new Dataset();
        // testingDataset.setPath("data/datasets/ner/www_test/index_split2.txt");
        // EvaluationResult er = tagger.evaluate(testingDataset, "data/temp/lingpipe.model");
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());

    }
}
