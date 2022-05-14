package ws.palladian.extraction.feature;

import org.apache.commons.math3.util.FastMath;

import java.util.Iterator;

public abstract class AbstractTermCorpus implements TermCorpus {

    @Override
    public final double getIdf(String term, boolean smoothing) {
        int s = smoothing ? 1 : 0;
        // 1 + to avoid negative idf values (this is the way Lucene does it):
        // https://lucene.apache.org/core/4_5_1/core/org/apache/lucene/search/similarities/TFIDFSimilarity.html
        // https://stackoverflow.com/a/19959938 (comments)
        return 1 + FastMath.log((double) getNumDocs() / (getCount(term) + s));
    }

    @Override
    public double getProbability(String term) {
        return (double)getCount(term) / getNumDocs();
    }
    
    @Override
    public Iterator<String> iterator() {
    		throw new UnsupportedOperationException();
    }

}
