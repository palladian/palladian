package ws.palladian.extraction.feature;

import java.util.Iterator;

public abstract class AbstractTermCorpus implements TermCorpus {

    @Override
    public final double getIdf(String term, boolean smoothing) {
        int s = smoothing ? 1 : 0;
        return Math.log((double) getNumDocs() / (getCount(term) + s));
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
