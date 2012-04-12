package ws.palladian.external.lingpipe;

import java.util.Set;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.dict.ApproxDictionaryChunker;
import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.TrieDictionary;
import com.aliasi.spell.FixedWeightEditDistance;
import com.aliasi.spell.WeightedEditDistance;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

public class ApproximateChunkerDemo {

    public static void main(String[] args) {
        DictionaryEntry<String> entry1 = new DictionaryEntry<String>("P53", "P53");
        DictionaryEntry<String> entry2 = new DictionaryEntry<String>("protein 53", "P53");
        DictionaryEntry<String> entry3 = new DictionaryEntry<String>("Mdm", "Mdm");
        TrieDictionary<String> dict = new TrieDictionary<String>();
        dict.addEntry(entry1);
        dict.addEntry(entry2);
        dict.addEntry(entry3);

        System.out.println("Dictionary=" + dict);

        TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;

        WeightedEditDistance editDistance = new FixedWeightEditDistance(0, -1, -1, -1, Double.NaN);

        double maxDistance = 2.0;

        ApproxDictionaryChunker chunker = new ApproxDictionaryChunker(dict, tokenizerFactory, editDistance, maxDistance);

        for (String text : args) {
            System.out.println("\n\n " + text + "\n");
            Chunking chunking = chunker.chunk(text);
            CharSequence cs = chunking.charSequence();
            Set<Chunk> chunkSet = chunking.chunkSet();

            System.out.printf("%15s  %15s   %8s\n", "Matched Phrase", "Dict Entry", "Distance");
            for (Chunk chunk : chunkSet) {
                int start = chunk.start();
                int end = chunk.end();
                CharSequence str = cs.subSequence(start, end);
                double distance = chunk.score();
                String match = chunk.type();
                System.out.printf("%15s  %15s   %8.1f\n", str, match, distance);
            }
        }

    }

}