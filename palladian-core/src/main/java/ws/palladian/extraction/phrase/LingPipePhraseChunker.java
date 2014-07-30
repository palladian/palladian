package ws.palladian.extraction.phrase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.helper.Cache;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.ImmutableAnnotation;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.ChunkFactory;
import com.aliasi.chunk.Chunking;
import com.aliasi.chunk.ChunkingImpl;
import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.tag.Tagging;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.util.FastCache;
import com.aliasi.util.Strings;

/**
 * Expects to chunk 1 sentence at a time. Needs lingPipe pos-tag model.
 * 
 * @author Martin Wunderwald
 * @author Philipp Katz
 */
public final class LingPipePhraseChunker implements PhraseChunker {

    // Determiners & Numerals
    // ABN, ABX, AP, AP$, AT, CD, CD$, DT, DT$, DTI, DTS, DTX, OD

    // Adjectives
    // JJ, JJ$, JJR, JJS, JJT

    // Nouns
    // NN, NN$, NNS, NNS$, NP, NP$, NPS, NPS$

    // Adverbs
    // RB, RB$, RBR, RBT, RN (not RP, the particle adverb)

    // Pronoun
    // PN, PN$, PP$, PP$$, PPL, PPLS, PPO, PPS, PPSS

    // Verbs
    // VB, VBD, VBG, VBN, VBZ

    // Auxiliaries
    // MD, BE, BED, BEDZ, BEG, BEM, BEN, BER, BEZ

    // Adverbs
    // RB, RB$, RBR, RBT, RN (not RP, the particle adverb)

    // Punctuation
    // ', ``, '', ., (, ), *, --, :, ,

    private static final Set<String> DETERMINER_TAGS = new HashSet<String>();
    private static final Set<String> ADJECTIVE_TAGS = new HashSet<String>();
    private static final Set<String> NOUN_TAGS = new HashSet<String>();
    private static final Set<String> PRONOUN_TAGS = new HashSet<String>();

    private static final Set<String> ADVERB_TAGS = new HashSet<String>();

    private static final Set<String> VERB_TAGS = new HashSet<String>();
    private static final Set<String> AUXILIARY_VERB_TAGS = new HashSet<String>();

    private static final Set<String> PUNCTUATION_TAGS = new HashSet<String>();

    private static final Set<String> START_VERB_TAGS = new HashSet<String>();
    private static final Set<String> CONTINUE_VERB_TAGS = new HashSet<String>();

    private static final Set<String> START_NOUN_TAGS = new HashSet<String>();
    private static final Set<String> CONTINUE_NOUN_TAGS = new HashSet<String>();

    static {
        DETERMINER_TAGS.add("abn");
        DETERMINER_TAGS.add("abx");
        DETERMINER_TAGS.add("ap");
        DETERMINER_TAGS.add("ap$");
        DETERMINER_TAGS.add("at");
        DETERMINER_TAGS.add("cd");
        DETERMINER_TAGS.add("cd$");
        DETERMINER_TAGS.add("dt");
        DETERMINER_TAGS.add("dt$");
        DETERMINER_TAGS.add("dti");
        DETERMINER_TAGS.add("dts");
        DETERMINER_TAGS.add("dtx");
        DETERMINER_TAGS.add("od");

        ADJECTIVE_TAGS.add("jj");
        ADJECTIVE_TAGS.add("jj$");
        ADJECTIVE_TAGS.add("jjr");
        ADJECTIVE_TAGS.add("jjs");
        ADJECTIVE_TAGS.add("jjt");
        ADJECTIVE_TAGS.add("*");
        ADJECTIVE_TAGS.add("ql");

        NOUN_TAGS.add("nn");
        NOUN_TAGS.add("nn$");
        NOUN_TAGS.add("nns");
        NOUN_TAGS.add("nns$");
        NOUN_TAGS.add("np");
        NOUN_TAGS.add("np$");
        NOUN_TAGS.add("nps");
        NOUN_TAGS.add("nps$");
        NOUN_TAGS.add("nr");
        NOUN_TAGS.add("nr$");
        NOUN_TAGS.add("nrs");

        PRONOUN_TAGS.add("pn");
        PRONOUN_TAGS.add("pn$");
        PRONOUN_TAGS.add("pp$");
        PRONOUN_TAGS.add("pp$$");
        PRONOUN_TAGS.add("ppl");
        PRONOUN_TAGS.add("ppls");
        PRONOUN_TAGS.add("ppo");
        PRONOUN_TAGS.add("pps");
        PRONOUN_TAGS.add("ppss");

        VERB_TAGS.add("vb");
        VERB_TAGS.add("vbd");
        VERB_TAGS.add("vbg");
        VERB_TAGS.add("vbn");
        VERB_TAGS.add("vbz");

        AUXILIARY_VERB_TAGS.add("to");
        AUXILIARY_VERB_TAGS.add("md");
        AUXILIARY_VERB_TAGS.add("be");
        AUXILIARY_VERB_TAGS.add("bed");
        AUXILIARY_VERB_TAGS.add("bedz");
        AUXILIARY_VERB_TAGS.add("beg");
        AUXILIARY_VERB_TAGS.add("bem");
        AUXILIARY_VERB_TAGS.add("ben");
        AUXILIARY_VERB_TAGS.add("ber");
        AUXILIARY_VERB_TAGS.add("bez");

        ADVERB_TAGS.add("rb");
        ADVERB_TAGS.add("rb$");
        ADVERB_TAGS.add("rbr");
        ADVERB_TAGS.add("rbt");
        ADVERB_TAGS.add("rn");
        ADVERB_TAGS.add("ql");
        ADVERB_TAGS.add("*"); // negation

        PUNCTUATION_TAGS.add("'");
        // PUNCTUATION_TAGS.add("``");
        // PUNCTUATION_TAGS.add("''");
        PUNCTUATION_TAGS.add(".");
        PUNCTUATION_TAGS.add("*");
        // PUNCTUATION_TAGS.add(","); // miss comma-separated phrases
        // PUNCTUATION_TAGS.add("(");
        // PUNCTUATION_TAGS.add(")");
        // PUNCTUATION_TAGS.add("*"); // negation "not"
        // PUNCTUATION_TAGS.add("--");
        // PUNCTUATION_TAGS.add(":");
    }

    static {

        START_NOUN_TAGS.addAll(DETERMINER_TAGS);
        START_NOUN_TAGS.addAll(ADJECTIVE_TAGS);
        START_NOUN_TAGS.addAll(NOUN_TAGS);
        START_NOUN_TAGS.addAll(PRONOUN_TAGS);

        CONTINUE_NOUN_TAGS.addAll(START_NOUN_TAGS);
        CONTINUE_NOUN_TAGS.addAll(ADVERB_TAGS);
        CONTINUE_NOUN_TAGS.addAll(PUNCTUATION_TAGS);
        CONTINUE_NOUN_TAGS.add("cc");

        START_VERB_TAGS.addAll(VERB_TAGS);
        START_VERB_TAGS.addAll(AUXILIARY_VERB_TAGS);
        START_VERB_TAGS.addAll(ADVERB_TAGS);

        CONTINUE_VERB_TAGS.addAll(START_VERB_TAGS);
        CONTINUE_VERB_TAGS.addAll(PUNCTUATION_TAGS);

    }
    
    private static final String PHRASE_CHUNKER_NAME = "LingPipe Phrase Chunker";
    
    /** The model used by the LingPipe phrase chunker. */
    private final HiddenMarkovModel model;

    /**
     * constructor
     */
    public LingPipePhraseChunker(File modelFile) {
        model = loadModel(modelFile);
    }

    /**
     * The internal chunking method.
     * 
     * @param characters
     * @param start
     * @param end
     * @return
     */
    private Chunking chunk(char[] characters, int start, int end) {

        // tokenize
        List<String> tokenList = new ArrayList<String>();
        List<String> whiteList = new ArrayList<String>();
        TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
        Tokenizer tokenizer = tokenizerFactory.tokenizer(characters, start, end - start);
        tokenizer.tokenize(tokenList, whiteList);
        String[] tokens = tokenList.<String> toArray(new String[tokenList.size()]);
        String[] whites = whiteList.<String> toArray(new String[whiteList.size()]);

        // part-of-speech tag
        int cacheSize = 100;
        FastCache<String, double[]> cache = new FastCache<String, double[]>(cacheSize);

        HmmDecoder posTagger = new HmmDecoder(model, null, cache);

        Tagging<String> tagging = posTagger.tag(tokenList);

        ChunkingImpl chunking = new ChunkingImpl(characters, start, end);
        int startChunk = 0;
        for (int i = 0; i < tagging.size();) {

            startChunk += whites[i].length();
            if (START_NOUN_TAGS.contains(tagging.tag(i))) {

                int endChunk = startChunk + tokens[i].length();
                ++i;
                while (i < tokens.length && CONTINUE_NOUN_TAGS.contains(tagging.tag(i))) {
                    endChunk += whites[i].length() + tokens[i].length();
                    ++i;
                }
                // this separation allows internal punctuation, but not final
                // punctuation
                int trimmedEndChunk = endChunk;
                for (int k = i; --k >= 0 && PUNCTUATION_TAGS.contains(tagging.tag(k));) {
                    trimmedEndChunk -= whites[k].length() + tokens[k].length();
                }
                if (startChunk >= trimmedEndChunk) {
                    startChunk = endChunk;
                    continue;
                }
                Chunk chunk = ChunkFactory.createChunk(startChunk, trimmedEndChunk, "NP");
                chunking.add(chunk);
                startChunk = endChunk;

            } else if (START_VERB_TAGS.contains(tagging.tag(i))) {
                int endChunk = startChunk + tokens[i].length();
                ++i;
                while (i < tokens.length && CONTINUE_VERB_TAGS.contains(tagging.tag(i))) {
                    endChunk += whites[i].length() + tokens[i].length();
                    ++i;
                }
                int trimmedEndChunk = endChunk;
                for (int k = i; --k >= 0 && PUNCTUATION_TAGS.contains(tagging.tag(k));) {
                    trimmedEndChunk -= whites[k].length() + tokens[k].length();
                }
                if (startChunk >= trimmedEndChunk) {
                    startChunk = endChunk;
                    continue;
                }
                Chunk chunk = ChunkFactory.createChunk(startChunk, trimmedEndChunk, "VP");
                chunking.add(chunk);
                startChunk = endChunk;

            } else {
                startChunk += tokens[i].length();
                ++i;
            }
        }
        return chunking;
    }

//    /*
//     * (non-Javadoc)
//     * @see
//     * tud.iir.extraction.event.AbstractPhraseChunker#chunk(java.lang.String)
//     */
//    @Override
//    public LingPipePhraseChunker chunk(String sentence) {
//        final char[] characters = Strings.toCharArray(sentence);
//        final Chunking chunking = this.chunk(characters, 0, characters.length);
//        final TagAnnotations tagAnnotations = new TagAnnotations();
//        for (final Chunk chunk : chunking.chunkSet()) {
//            final TagAnnotation tagAnnotation = new TagAnnotation(chunk.start(), chunk.type(), sentence.substring(chunk
//                    .start(), chunk.end()));
//            tagAnnotations.add(tagAnnotation);
//        }
//
//        setTagAnnotations(tagAnnotations);
//        return this;
//    }
    
    @Override
    public List<Annotation> chunk(String sentence) {
      char[] characters = Strings.toCharArray(sentence);
      Chunking chunking = this.chunk(characters, 0, characters.length);
      List<Annotation> tagAnnotations = CollectionHelper.newArrayList();
      for (Chunk chunk : chunking.chunkSet()) {
            Annotation tagAnnotation = new ImmutableAnnotation(chunk.start(), sentence.substring(chunk.start(),
                    chunk.end()), chunk.type());
            tagAnnotations.add(tagAnnotation);
      }
      return tagAnnotations;
    }

//    /*
//     * (non-Javadoc)
//     * @see
//     * tud.iir.extraction.event.AbstractPhraseChunker#chunk(java.lang.String,
//     * java.lang.String)
//     */
//    @Override
//    public final LingPipePhraseChunker chunk(final String sentence, final String modelFilePath) {
//        loadModel(modelFilePath);
//        return this.chunk(sentence);
//
//    }

//    /*
//     * (non-Javadoc)
//     * @see tud.iir.extraction.event.AbstractPhraseChunker#loadModel()
//     */
//    @Override
//    public final LingPipePhraseChunker loadDefaultModel() {
//        return loadModel(MODEL);
//    }

    /*
     * (non-Javadoc)
     * @see
     * tud.iir.extraction.event.AbstractPhraseChunker#loadModel(java.lang.String
     * )
     */
//    @Override
//    public final LingPipePhraseChunker loadModel(final String modelFilePath) {
    private final HiddenMarkovModel loadModel(File modelFile) {
        String modelFilePath = modelFile.getAbsolutePath();
        HiddenMarkovModel hmm = (HiddenMarkovModel) Cache.getInstance().getDataObject(modelFilePath);
        if (hmm == null) {
            ObjectInputStream inputStream = null;
            try {
                inputStream = new ObjectInputStream(new FileInputStream(modelFile));
                hmm = (HiddenMarkovModel) inputStream.readObject();
                Cache.getInstance().putDataObject(modelFilePath, hmm);
            } catch (IOException e) {
                throw new IllegalStateException("Error while loading model file \"" + modelFilePath + "\": "
                        + e.getMessage());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Error while loading model file \"" + modelFilePath + "\": "
                        + e.getMessage());
            } finally {
                FileHelper.close(inputStream);
            }
        }
        return hmm;
    }
    
    @Override
    public String getName() {
        return PHRASE_CHUNKER_NAME;
    }



}
