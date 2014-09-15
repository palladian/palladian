package ws.palladian.extraction.entity.tagger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.TrainableNamedEntityRecognizer;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.entity.tagger.helper.Conll2002ChunkTagParser;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;

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
 * "long-distance character language model-based chunker that operates by rescoring the output of a contained character
 * language model HMM chunker.
 * </p>
 * 
 * @see <a href="http://alias-i.com/lingpipe/demos/tutorial/ne/read-me.html">LingPipeNamed: Named Entity Tutorial</a>
 * @author David Urbansky
 */
public class LingPipeNer extends TrainableNamedEntityRecognizer {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LingPipeNer.class);

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
            File tempDirectory = FileHelper.getTempDir();
            String transformedPath = new File(tempDirectory, "LingPipeNer-" + UUID.randomUUID() + ".txt").getPath();

            FileFormatParser.removeWhiteSpaceInFirstColumn(trainingFilePath, transformedPath, "_");
            FileFormatParser.tsvToSsv(transformedPath, transformedPath);
            FileFormatParser.columnToColumnBio(transformedPath, transformedPath, " ");

            LOGGER.debug("Setting up Chunker Estimator");
            TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
            CharLmRescoringChunker chunkerEstimator = new CharLmRescoringChunker(tokenizerFactory,
                    NUM_CHUNKINGS_RESCORED, MAX_N_GRAM, NUM_CHARS, LM_INTERPOLATION);
            // HmmCharLmEstimator hmmEstimator = new HmmCharLmEstimator(MAX_N_GRAM, NUM_CHARS, LM_INTERPOLATION);
            // CharLmHmmChunker chunkerEstimator = new CharLmHmmChunker(tokenizerFactory, hmmEstimator);

            LOGGER.debug("Setting up Data Parser");
            // GeneTagParser parser = new GeneTagParser();
            Conll2002ChunkTagParser parser = new Conll2002ChunkTagParser();
            parser.setHandler(chunkerEstimator);

            File corpusFile = new File(transformedPath);
            LOGGER.trace("Training with data from file={}", corpusFile);
            parser.parse(corpusFile);

            File modelFile = new File(modelFilePath);
            LOGGER.debug("Compiling and writing model to file={}", modelFile);
            AbstractExternalizable.compileTo(chunkerEstimator, modelFile);

        } catch (IOException e) {
            LOGGER.error("IOException during training", e);
            return false;
        }

        return true;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        StopWatch stopWatch = new StopWatch();
        File modelFile = new File(configModelFilePath);
        LOGGER.debug("Reading chunker from file {}", modelFile);
        try {
            chunker = (Chunker)AbstractExternalizable.readObject(modelFile);
        } catch (IOException e) {
            LOGGER.error("IOException when loading model", e);
        } catch (ClassNotFoundException e) {
            LOGGER.error("ClassNotFoundException when loading model", e);
        }
        LOGGER.debug("Model {} successfully loaded in {}", modelFile, stopWatch);
        return true;
    }

    @Override
    public List<Annotation> getAnnotations(String inputText) {
        Annotations<Annotation> annotations = new Annotations<Annotation>();
        Chunking chunking = chunker.chunk(inputText);
        for (Chunk chunk : chunking.chunkSet()) {
            int offset = chunk.start();
            String entityName = inputText.substring(offset, chunk.end());
            String tagName = chunk.type();
            annotations.add(new ImmutableAnnotation(offset, entityName, tagName));
        }
        annotations.removeNested();
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
