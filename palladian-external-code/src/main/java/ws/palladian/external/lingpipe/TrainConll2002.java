package ws.palladian.external.lingpipe;

import java.io.File;
import java.io.IOException;

import ws.palladian.extraction.entity.tagger.helper.Conll2002ChunkTagParser;

import com.aliasi.chunk.CharLmRescoringChunker;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.AbstractExternalizable;

public class TrainConll2002 {

    static final int NUM_CHUNKINGS_RESCORED = 64;
    static final int MAX_N_GRAM = 12;
    static final int NUM_CHARS = 256;
    static final double LM_INTERPOLATION = MAX_N_GRAM; // default behavior
    static final boolean SMOOTH_TAGS = true;

    // java TrainGeneTag <trainingInputFile> <modelOutputFile>
    public static void main(String[] args) throws IOException {
        File modelFile = new File(args[0]);
        File trainFile = new File(args[1]);
        File devFile = new File(args[2]);

        System.out.println("Setting up Chunker Estimator");
        TokenizerFactory factory
            = IndoEuropeanTokenizerFactory.INSTANCE;
        CharLmRescoringChunker chunkerEstimator
            = new CharLmRescoringChunker(factory,
                                         NUM_CHUNKINGS_RESCORED,
                                         MAX_N_GRAM,
                                         NUM_CHARS,
                                         LM_INTERPOLATION,
                                         SMOOTH_TAGS);

        System.out.println("Setting up Data Parser");
        Conll2002ChunkTagParser parser
            = new Conll2002ChunkTagParser();
        parser.setHandler(chunkerEstimator);

        System.out.println("Training with Data from File=" + trainFile);
        parser.parse(trainFile);
        System.out.println("Training with Data from File=" + devFile);
        parser.parse(devFile);

        System.out.println("Compiling and Writing Model to File=" + modelFile);
        AbstractExternalizable.compileTo(chunkerEstimator,modelFile);
    }

}
