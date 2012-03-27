package ws.palladian.external.lingpipe;

import java.io.File;
import java.util.Iterator;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.ConfidenceChunker;
import com.aliasi.util.AbstractExternalizable;

public class RunConfidenceChunker {


    static final int MAX_N_BEST_CHUNKS = 8;

    public static void main(String[] args) throws Exception {
        File modelFile = new File(args[0]);

        System.out.println("Reading chunker from file=" + modelFile);
        ConfidenceChunker chunker
            = (ConfidenceChunker) AbstractExternalizable.readObject(modelFile);

        for (int i = 1; i < args.length; ++i) {
            char[] cs = args[i].toCharArray();
            Iterator<Chunk> it
                = chunker.nBestChunks(cs,0,cs.length,MAX_N_BEST_CHUNKS);
            System.out.println(args[i]);
            System.out.println("Rank      Conf      Span    Type     Phrase");
            for (int n = 0; it.hasNext(); ++n) {
                Chunk chunk = it.next();
                double conf = java.lang.Math.pow(2.0,chunk.score());
                int start = chunk.start();
                int end = chunk.end();
                String phrase = args[i].substring(start,end);
                System.out.println(n + " "
                                   + String.format("%12.4f",conf)
                                   + "   (" + start
                                   + ", " + end
                                   + ")   " + chunk.type()
                                   + "     " + phrase);

            }
            System.out.println();
        }
    }
}
