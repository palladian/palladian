package ws.palladian.preprocessing.featureextraction;

import gnu.trove.TObjectIntHashMap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ws.palladian.helper.FileHelper;

public class TroveTermCorpus implements Serializable, TermCorpus {
    
    private static final long serialVersionUID = 1L;
    private TObjectIntHashMap<String> termMap;
    private int numDocs;
    
    public TroveTermCorpus() {
        //terms = new HashBag<String>();
        termMap = new TObjectIntHashMap<String>();
        numDocs = 0;
    }
    
    /* (non-Javadoc)
     * @see com.newsseecr.xperimental.preprocessing.TermCorpus#addTermsFromDocument(java.util.Set)
     */
    @Override
    public void addTermsFromDocument(Set<String> terms) {
        for (String term : terms) {
            this.termMap.adjustOrPutValue(term, 1, 1);            
        }
        numDocs++;
    }
    
    /* (non-Javadoc)
     * @see com.newsseecr.xperimental.preprocessing.TermCorpus#serialize(java.lang.String)
     */
    @Override
    public void serialize(String filePath) {
        FileHelper.serialize(this, filePath);
    }
    
    public static TermCorpus deserialize(String filePath) {
        return (TroveTermCorpus) FileHelper.deserialize(filePath);
    }
    
    /* (non-Javadoc)
     * @see com.newsseecr.xperimental.preprocessing.TermCorpus#getDf(java.lang.String)
     */
    @Override
    public double getDf(String term) {
        int termCount = this.termMap.get(term);
        // add 1; prevent division by zero
        double documentFrequency = Math.log10((double) numDocs / (termCount + 1));
        // double documentFrequency = (double) termCount / numDocs;
        return documentFrequency;
    }
    
    /* (non-Javadoc)
     * @see com.newsseecr.xperimental.preprocessing.TermCorpus#getNumDocs()
     */
    @Override
    public int getNumDocs() {
        return numDocs;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TroveTermCorpus");
        sb.append(" numDocs=").append(numDocs);
        // sb.append(" numUniqueTerms=").append(terms.uniqueSet().size());
        sb.append(" numUniqueTerms=").append(termMap.size());
        // sb.append(" numTerms=").append(terms.size());
        return sb.toString();
    }
    
    
    public static void main(String[] args) {
        
        TermCorpus corpus = TroveTermCorpus.deserialize("data/titleCountCorpus.ser");
        System.out.println(corpus.toString());
        System.out.println(corpus.getDf("apple"));
        System.out.println(corpus.getDf("und"));
        System.out.println(corpus.getDf("mit"));
        System.out.println(corpus.getDf("microsoft"));
        System.out.println(corpus.getDf("askldjalksd"));
        
        System.exit(0);
        
        
        corpus = new TroveTermCorpus();
        corpus.addTermsFromDocument(new HashSet<String>(Arrays.asList("one", "two", "three")));
        corpus.addTermsFromDocument(new HashSet<String>(Arrays.asList("one", "four", "seven")));
        System.out.println(corpus.getDf("one"));
        corpus.serialize("data/temp/tempSer.ser");

        corpus = null;
        corpus = TroveTermCorpus.deserialize("data/temp/tempSer.ser");
        System.out.println(corpus.getDf("one"));
    }

}
