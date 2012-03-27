package ws.palladian.external.lingpipe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.aliasi.chunk.Chunking;
import com.aliasi.chunk.ChunkingEvaluation;
import com.aliasi.corpus.Parser;
import com.aliasi.corpus.parsers.Muc6ChunkParser;

public class FileScorer {

    private final Parser<com.aliasi.corpus.ObjectHandler<Chunking>> mParser;

    private final ChunkingEvaluation mEvaluation = new ChunkingEvaluation();

    public FileScorer(Parser<com.aliasi.corpus.ObjectHandler<Chunking>> parser) {
        mParser = parser;
    }

    public ChunkingEvaluation evaluation() {
        return mEvaluation;
    }

    public void score(File refFile, File responseFile) throws IOException {
        ChunkingCollector refCollector = new ChunkingCollector();
        mParser.setHandler(refCollector);
        mParser.parse(refFile);
        List<Chunking> refChunkings = refCollector.mChunkingList;

        ChunkingCollector responseCollector = new ChunkingCollector();
        mParser.setHandler(responseCollector);
        mParser.parse(responseFile);
        List<Chunking> responseChunkings = responseCollector.mChunkingList;

        if (refChunkings.size() != responseChunkings.size()) {
            throw new IllegalArgumentException("chunkings not same size");
        }

        for (int i = 0; i < refChunkings.size(); ++i) {
            mEvaluation.addCase(refChunkings.get(i), responseChunkings.get(i));
        }
    }

    private static class ChunkingCollector implements com.aliasi.corpus.ObjectHandler<Chunking> {

        private final List<Chunking> mChunkingList = new ArrayList<Chunking>();

        public void handle(Chunking chunking) {
            mChunkingList.add(chunking);
        }
    }

    public static void main(String[] args) throws IOException {
        File refFile = new File(args[0]);
        File responseFile = new File(args[1]);

        Parser parser = new Muc6ChunkParser();
        FileScorer scorer = new FileScorer(parser);
        scorer.score(refFile, responseFile);

        System.out.println(scorer.evaluation().toString());
    }

}