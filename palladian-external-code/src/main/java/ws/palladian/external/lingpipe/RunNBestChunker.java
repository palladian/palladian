package ws.palladian.external.lingpipe;

import java.io.File;
import java.util.Iterator;

import com.aliasi.chunk.Chunking;
import com.aliasi.chunk.NBestChunker;
import com.aliasi.util.AbstractExternalizable;
import com.aliasi.util.ScoredObject;

public class RunNBestChunker {


    static final int MAX_N_BEST = 8;

    public static void main(String[] args) throws Exception {
	File modelFile = new File(args[0]);

	System.out.println("Reading chunker from file=" + modelFile);
	NBestChunker chunker 
	    = (NBestChunker) AbstractExternalizable.readObject(modelFile);

	for (int i = 1; i < args.length; ++i) {
	    char[] cs = args[i].toCharArray();
	    Iterator<ScoredObject<Chunking>> it = chunker.nBest(cs,0,cs.length,MAX_N_BEST);
	    System.out.println(args[i]);
	    for (int n = 0; it.hasNext(); ++n) {
		ScoredObject<Chunking> so = it.next();
		double jointProb = so.score();
		Chunking chunking = so.getObject();
		System.out.println(n + " " + jointProb 
				   + " " + chunking.chunkSet());
	    }
	    System.out.println();
	}
    }
}
