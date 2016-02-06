package ws.palladian.extraction.location.persistence;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

/**
 * This class collects search results; we need no scoring and accept the documents in any order here, which yields
 * in a great performance boost in contrast to Lucene's default hit-collecting logic.
 * 
 * @author Philipp Katz
 */
final class SimpleCollector extends Collector {
    final Set<Integer> docs = new HashSet<>();
    int docBase;

    @Override
    public void setScorer(Scorer scorer) throws IOException {
        // no scoring
    }

    @Override
    public void collect(int doc) throws IOException {
        docs.add(docBase + doc);
    }

    @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
        this.docBase = context.docBase;
    }

    @Override
    public boolean acceptsDocsOutOfOrder() {
        return true;
    }

}
