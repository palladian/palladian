package ws.palladian.external.lingpipe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.aliasi.chunk.BioTagChunkCodec;
import com.aliasi.chunk.CharLmRescoringChunker;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.ChunkerEvaluator;
import com.aliasi.chunk.Chunking;
import com.aliasi.chunk.ChunkingEvaluation;
import com.aliasi.chunk.TagChunkCodec;
import com.aliasi.classify.PrecisionRecallEvaluation;
import com.aliasi.hmm.HmmCharLmEstimator;
import com.aliasi.io.FileLineReader;
import com.aliasi.tag.StringTagging;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.AbstractExternalizable;
import com.aliasi.util.Strings;

public class ANERXval {

    static final int NUM_FOLDS = 6;

    static final String LOC_TAG = "LOC";
    static final String PERS_TAG = "PERS";
    static final String ORG_TAG = "ORG";
    static final String MISC_TAG = "MISC";

    static final String[] REPORT_TYPES = new String[] { LOC_TAG, PERS_TAG, ORG_TAG, MISC_TAG };

    static final String TOKENIZER_REGEX = "\\S+";
    static int HMM_N_GRAM_LENGTH = -1;
    static int NUM_CHARS = -1;
    static double HMM_INTERPOLATION_RATIO = -1.0;
    static int NUM_ANALYSES_RESCORED = -1;

    static boolean USE_DICTIONARY = false;
    static boolean INCLUDE_MISC = true;

    // expected file layout
    // $ANER/ANERCorp
    // $ANER/ANERGazet/AlignedLocGazetteer
    // $ANER/ANERGazet/AlignedOrgGazetteer
    // $ANER/ANERGazet/AlignedPerGazetteer
    public static void main(String[] args) throws IOException, ClassNotFoundException {

        File anerDir = new File(args[0]);
        USE_DICTIONARY = Boolean.valueOf(args[1]);
        INCLUDE_MISC = Boolean.valueOf(args[2]);
        HMM_N_GRAM_LENGTH = Integer.valueOf(args[3]);
        HMM_INTERPOLATION_RATIO = Double.valueOf(args[4]);
        NUM_CHARS = Integer.valueOf(args[5]);
        NUM_ANALYSES_RESCORED = Integer.valueOf(args[6]);

        File corpusFile = new File(anerDir, "ANERCorp");
        File gazDir = new File(anerDir, "ANERGazet");
        File locGazFile = new File(gazDir, "AlignedLocGazetteer");
        File orgGazFile = new File(gazDir, "AlignedOrgGazetteer");
        File persGazFile = new File(gazDir, "AlignedPersGazetteer");

        System.out.println("Input Files");
        report("    NE Corpus", corpusFile);
        report("    Location Gazetteer", locGazFile);
        report("    Organization Gazetteer", orgGazFile);
        report("    Person Gazetteer", persGazFile);

        System.out.println("\nParameters");
        System.out.println("   N-Gram=" + HMM_N_GRAM_LENGTH);
        System.out.println("   Num chars=" + NUM_CHARS);
        System.out.println("   Interpolation Ratio=" + HMM_INTERPOLATION_RATIO);
        System.out.println("   Number of Analyses Rescored=" + NUM_ANALYSES_RESCORED);
        System.out.println("   Including MISC entity type=" + INCLUDE_MISC);
        System.out.println("   Use dictionary=" + USE_DICTIONARY);

        System.out.println();

        // sequences of letters, single non
        TokenizerFactory tokenizerFactory = new RegExTokenizerFactory(TOKENIZER_REGEX);

        String[] locDict = FileLineReader.readLineArray(locGazFile, Strings.UTF8);
        String[] orgDict = FileLineReader.readLineArray(orgGazFile, Strings.UTF8);
        String[] persDict = FileLineReader.readLineArray(persGazFile, Strings.UTF8);

        System.out.println("\nCorpus Statistics");
        System.out.println("    Location Dict Entries=" + locDict.length);
        System.out.println("    Organization Dict Entries=" + orgDict.length);
        System.out.println("    Person Dict Entries=" + persDict.length);

        List<Chunking> sentences = parseANER(corpusFile);
        System.out.println();
        // shuffling's cheating here because of intra-doc correlations
        // among entities
        // Collections.shuffle(sentences,new Random(42));

        int numSentences = sentences.size();

        ChunkerEvaluator evaluator = new ChunkerEvaluator(null); // set chunker in loop
        evaluator.setMaxConfidenceChunks(1);
        evaluator.setMaxNBest(1);
        evaluator.setVerbose(false);

        for (int fold = NUM_FOLDS; --fold >= 0;) {
            int startTestFold = fold * numSentences / NUM_FOLDS;
            int endTestFold = (fold + 1) * numSentences / NUM_FOLDS;
            if (fold == NUM_FOLDS - 1) {
                endTestFold = numSentences; // round up on last fold
            }
            System.out.println("-----------------------------------------------");
            System.out.printf("FOLD=%2d  start sent=%5d  end sent=%5d\n", fold, startTestFold, endTestFold);

            ChunkerEvaluator foldEvaluator = new ChunkerEvaluator(null);
            foldEvaluator.setMaxConfidenceChunks(1);
            foldEvaluator.setMaxNBest(1);
            foldEvaluator.setVerbose(false);

            // comment to use plain HmmChunker
            HmmCharLmEstimator hmmEstimator = new HmmCharLmEstimator(HMM_N_GRAM_LENGTH, NUM_CHARS,
                    HMM_INTERPOLATION_RATIO);

            // uncomment to evaluation plain Hmm chunker
            // CharLmHmmChunker chunker
            // = new CharLmHmmChunker(tokenizerFactory,hmmEstimator,true);

            CharLmRescoringChunker chunker = new CharLmRescoringChunker(tokenizerFactory, NUM_ANALYSES_RESCORED,
                    HMM_N_GRAM_LENGTH, NUM_CHARS, HMM_INTERPOLATION_RATIO, true);

            System.out.println("training labeled data");
            for (int i = 0; i < startTestFold; ++i) {
                chunker.handle(sentences.get(i));
            }
            for (int i = endTestFold; i < numSentences; ++i) {
                chunker.handle(sentences.get(i));
            }
            if (USE_DICTIONARY) {
                System.out.println("training dictionary");
                for (String locName : locDict) {
                    if (locName.length() > 0) {
                        chunker.trainDictionary(locName, LOC_TAG);
                    }
                }
                for (String orgName : orgDict) {
                    if (orgName.length() > 0) {
                        chunker.trainDictionary(orgName, ORG_TAG);
                    }
                }
                for (String persName : persDict) {
                    if (persName.length() > 0) {
                        chunker.trainDictionary(persName, PERS_TAG);
                    }
                }
            }

            System.out.println("compiling");
            Chunker compiledChunker = (Chunker) AbstractExternalizable.compile(chunker);

            evaluator.setChunker(compiledChunker);
            foldEvaluator.setChunker(compiledChunker);

            System.out.println("evaluating");
            for (int i = startTestFold; i < endTestFold; ++i) {
                try {
                    evaluator.handle(sentences.get(i));
                    foldEvaluator.handle(sentences.get(i));
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    System.out.println(sentences.get(i));
                }
            }
            printEval("FOLD=" + fold, foldEvaluator);
        }
        System.out.println("\n===============================================");
        System.out.println("COMBINED CROSS-VALIDATION RESULTS");
        printEval("X-Val", evaluator);
    }

    static void printEval(String msg, ChunkerEvaluator evaluator) {
        ChunkingEvaluation chunkingEval = evaluator.evaluation();

        for (String tag : REPORT_TYPES) {
            PrecisionRecallEvaluation prEvalByType = chunkingEval.perTypeEvaluation(tag).precisionRecallEvaluation();
            System.out.printf("%10s    %6s P=%5.3f R=%5.3f F=%5.3f\n", msg, tag, prEvalByType.precision(),
                    prEvalByType.recall(), prEvalByType.fMeasure());
        }

        PrecisionRecallEvaluation prEval = chunkingEval.precisionRecallEvaluation();
        System.out.println("");
        System.out.printf("%10s  COMBINED P=%5.3f R=%5.3f F=%5.3f\n", msg, prEval.precision(), prEval.recall(),
                prEval.fMeasure());

        // uncomment to dump out precsion-recall evals
        // ScoredPrecisionRecallEvaluation scoredPrEval
        // = evaluator.confidenceEvaluation();
        // double[][] prCurve = scoredPrEval.prCurve(true);
        // System.out.printf("%5s %5s\n","REC","PREC");
        // for (double[] rp : prCurve) {
        // if (rp[0] < 0.7) continue;
        // System.out.printf("%5.3f %5.3f\n",
        // rp[0],rp[1]);
        // }
        // System.out.printf("MAX F=%5.3f\n",scoredPrEval.maximumFMeasure());
    }

    static List<Chunking> parseANER(File corpusFile) throws IOException {
        List<Chunking> sentences = new ArrayList<Chunking>();
        List<String> tokenBuf = new ArrayList<String>();
        List<String> tagBuf = new ArrayList<String>();
        String[] lines = FileLineReader.readLineArray(corpusFile, Strings.UTF8);
        int numTokens = 0;
        for (String line : lines) {
            int pos = line.lastIndexOf(" ");
            String token = null;
            String tag = null;
            if (pos < 0) {
                if (line.equals(".O")) {
                    // fix broken line in corpus
                    token = ".";
                    tag = "O";
                } else {
                    System.out.println("Illegal line=" + line);
                    continue;
                }
            } else {
                token = line.substring(0, pos);
                tag = line.substring(pos + 1);
            }
            if (!INCLUDE_MISC && tag.indexOf(MISC_TAG) >= 0) {
                tag = "O";
            }
            tokenBuf.add(token);
            tagBuf.add(tag);
            ++numTokens;
            if (isEos(token, tag)) {
                sentences.add(toChunking(tokenBuf, tagBuf));
                tokenBuf.clear();
                tagBuf.clear();
            }
        }
        sentences.add(toChunking(tokenBuf, tagBuf));
        System.out.println("    # sentences=" + sentences.size());
        System.out.println("    # tokens=" + numTokens);
        return sentences;
    }

    static Chunking toChunking(List<String> tokens, List<String> tags) {
        boolean enforceConsistency = false;
        TagChunkCodec codec = new BioTagChunkCodec(null, enforceConsistency, "B-", "I-", "O");
        StringBuilder sb = new StringBuilder();
        int[] tokenStarts = new int[tokens.size()];
        int[] tokenEnds = new int[tokens.size()];
        for (int i = 0; i < tokens.size(); ++i) {
            if (i > 0) {
                sb.append(' ');
            }
            tokenStarts[i] = sb.length();
            sb.append(tokens.get(i));
            tokenEnds[i] = sb.length();
        }
        StringTagging tagging = new StringTagging(tokens, tags, sb, tokenStarts, tokenEnds);
        Chunking chunking = codec.toChunking(tagging);
        return chunking;
    }

    static boolean isEos(String token, String tag) {
        return "O".equals(tag) && (".".equals(token) || "!".equals(token));
    }

    static void report(String msg, File file) throws IOException {
        System.out.println(msg + ": " + file.getCanonicalPath());
    }

}