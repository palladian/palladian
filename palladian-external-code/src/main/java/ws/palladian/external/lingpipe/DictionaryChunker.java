package ws.palladian.external.lingpipe;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.ExactDictionaryChunker;
import com.aliasi.dict.MapDictionary;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;

public class DictionaryChunker {

    static final double CHUNK_SCORE = 1.0;

    public static void main(String[] args) {

        MapDictionary<String> dictionary = new MapDictionary<String>();
        dictionary.addEntry(new DictionaryEntry<String>("50 Cent", "PERSON", CHUNK_SCORE));
        dictionary.addEntry(new DictionaryEntry<String>("XYZ120 DVD Player", "DB_ID_1232", CHUNK_SCORE));
        dictionary.addEntry(new DictionaryEntry<String>("cent", "MONETARY_UNIT", CHUNK_SCORE));
        dictionary.addEntry(new DictionaryEntry<String>("dvd player", "PRODUCT", CHUNK_SCORE));

        ExactDictionaryChunker dictionaryChunkerTT = new ExactDictionaryChunker(dictionary,
                IndoEuropeanTokenizerFactory.INSTANCE, true, true);

        ExactDictionaryChunker dictionaryChunkerTF = new ExactDictionaryChunker(dictionary,
                IndoEuropeanTokenizerFactory.INSTANCE, true, false);

        ExactDictionaryChunker dictionaryChunkerFT = new ExactDictionaryChunker(dictionary,
                IndoEuropeanTokenizerFactory.INSTANCE, false, true);

        ExactDictionaryChunker dictionaryChunkerFF = new ExactDictionaryChunker(dictionary,
                IndoEuropeanTokenizerFactory.INSTANCE, false, false);

        System.out.println("\nDICTIONARY\n" + dictionary);

        for (int i = 0; i < args.length; ++i) {
            String text = args[i];
            System.out.println("\n\nTEXT=" + text);

            chunk(dictionaryChunkerTT, text);
            chunk(dictionaryChunkerTF, text);
            chunk(dictionaryChunkerFT, text);
            chunk(dictionaryChunkerFF, text);
        }

    }

    static void chunk(ExactDictionaryChunker chunker, String text) {
        System.out.println("\nChunker." + " All matches=" + chunker.returnAllMatches() + " Case sensitive="
                + chunker.caseSensitive());
        Chunking chunking = chunker.chunk(text);
        for (Chunk chunk : chunking.chunkSet()) {
            int start = chunk.start();
            int end = chunk.end();
            String type = chunk.type();
            double score = chunk.score();
            String phrase = text.substring(start, end);
            System.out.println("     phrase=|" + phrase + "|" + " start=" + start + " end=" + end + " type=" + type
                    + " score=" + score);
        }
    }

}
