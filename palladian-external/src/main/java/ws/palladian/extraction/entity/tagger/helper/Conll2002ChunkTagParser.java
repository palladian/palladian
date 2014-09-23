package ws.palladian.extraction.entity.tagger.helper;

import com.aliasi.chunk.BioTagChunkCodec;
import com.aliasi.chunk.Chunking;
import com.aliasi.chunk.TagChunkCodec;
import com.aliasi.chunk.TagChunkCodecAdapters;
import com.aliasi.corpus.StringParser;
import com.aliasi.tag.LineTaggingParser;
import com.aliasi.tag.Tagging;

/**
 * Here's a corpus sample from CoNLL 2002 test file <code>ned.testa</code>,
 * which is encoded in the character set ISO-8859-1 (aka Latin1):
 * 
 * <blockquote>
 * 
 * <pre>
 * ...
 * de Art O
 * orde N O
 * . Punc O
 * 
 * -DOCSTART- -DOCSTART- O
 * Met Prep O
 * tien Num O
 * miljoen Num O
 * komen V O
 * we Pron O
 * , Punc O
 * denk V O
 * ik Pron O
 * , Punc O
 * al Adv O
 * een Art O
 * heel Adj O
 * eind N O
 * . Punc O
 * 
 * Dirk N B-PER
 * ...
 * </pre>
 * 
 * </blockquote>
 */
public class Conll2002ChunkTagParser extends StringParser<com.aliasi.corpus.ObjectHandler<Chunking>> {

    static final String TOKEN_TAG_LINE_REGEX = "(\\S+)\\s(\\S+\\s)?(O|[B|I]-\\S+)"; // token ?posTag entityTag
    static final int TOKEN_GROUP = 1; // token
    static final int TAG_GROUP = 3; // entityTag
    static final String IGNORE_LINE_REGEX = "-DOCSTART(.*)"; // lines that start with "-DOCSTART"
    static final String EOS_REGEX = "\\A\\Z"; // empty lines

    static final String BEGIN_TAG_PREFIX = "B-";
    static final String IN_TAG_PREFIX = "I-";
    static final String OUT_TAG = "O";

    private final LineTaggingParser mParser = new LineTaggingParser(TOKEN_TAG_LINE_REGEX, TOKEN_GROUP, TAG_GROUP,
            IGNORE_LINE_REGEX, EOS_REGEX);

    private final TagChunkCodec mCodec = new BioTagChunkCodec(null, // no tokenizer
            false, // don't enforce consistency
            BEGIN_TAG_PREFIX, // custom BIO tag coding matches regex
            IN_TAG_PREFIX, OUT_TAG);

    @Override
    public void parseString(char[] cs, int start, int end) {
        mParser.parseString(cs, start, end);
    }

    @Override
    public void setHandler(com.aliasi.corpus.ObjectHandler<Chunking> handler) {
        com.aliasi.corpus.ObjectHandler<Tagging<String>> taggingHandler = TagChunkCodecAdapters.chunkingToTagging(
                mCodec, handler);
        mParser.setHandler(taggingHandler);
    }

}
