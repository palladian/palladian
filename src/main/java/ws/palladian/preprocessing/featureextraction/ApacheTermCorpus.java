package ws.palladian.preprocessing.featureextraction;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.collections15.bag.HashBag;

import ws.palladian.helper.FileHelper;

public class ApacheTermCorpus implements Serializable, TermCorpus {

    private static final long serialVersionUID = 1L;
    private HashBag<String> terms;
    private int numDocs;

    public ApacheTermCorpus() {
        terms = new HashBag<String>();
        numDocs = 0;
    }

    @Override
    public void addTermsFromDocument(Set<String> terms) {
        this.terms.addAll(terms);
        numDocs++;
    }

    @Override
    public void serialize(String filePath) {
        FileHelper.serialize(this, filePath);
    }

    @Override
    public double getDf(String term) {
        int termCount = terms.getCount(term);
        // add 1; prevent division by zero
        double documentFrequency = Math.log10((double) numDocs / (termCount + 1));
        // double documentFrequency = (double) termCount / numDocs;
        return documentFrequency;
    }

    @Override
    public int getNumDocs() {
        return numDocs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ApacheTermCorpus");
        sb.append(" numDocs=").append(numDocs);
        sb.append(" numUniqueTerms=").append(terms.uniqueSet().size());
        sb.append(" numTerms=").append(terms.size());
        return sb.toString();
    }

    public static TermCorpus deserialize(String filePath) {
        return (ApacheTermCorpus) FileHelper.deserialize(filePath);
    }

}
