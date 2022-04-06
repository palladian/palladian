package ws.palladian.extraction.location.persistence;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.index.LeafReaderContext;

/**
 * This class collects search results; we need no scoring and accept the documents in any order here, which yields
 * in a great performance boost in contrast to Lucene's default hit-collecting logic.
 * 
 * @author Philipp Katz
 */
final class SimpleCollector extends org.apache.lucene.search.SimpleCollector {
    final Set<Integer> docs = new HashSet<>();
    int docBase;
    @Override
    public void collect(int doc) throws IOException {
        docs.add(docBase + doc);
    }

    @Override
    protected void doSetNextReader(LeafReaderContext context) throws IOException {
        this.docBase = context.docBase;
    }

    @Override
    public boolean needsScores() {
        return false;
    }

}
