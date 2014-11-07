package ws.palladian.extraction.feature;

public abstract class AbstractTermCorpus implements TermCorpus {

    @Override
    public final double getIdf(String term, boolean smoothing) {
        int s = smoothing ? 1 : 0;
        return (double)(getNumDocs() + s) / (getCount(term) + s);
    }

    @Override
    public double getProbability(String term) {
        return (double)getCount(term) / getNumDocs();
    }

}
