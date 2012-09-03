package ws.palladian.external.lingpipe;

import java.io.File;

import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;
import com.aliasi.util.AbstractExternalizable;

public class RunChunker {

    public static void main(String[] args) throws Exception {
	File modelFile = new File(args[0]);

	System.out.println("Reading chunker from file=" + modelFile);
	Chunker chunker 
	    = (Chunker) AbstractExternalizable.readObject(modelFile);

	for (int i = 1; i < args.length; ++i) {
	    Chunking chunking = chunker.chunk(args[i]);
	    System.out.println("Chunking=" + chunking);
	}

    }

}
