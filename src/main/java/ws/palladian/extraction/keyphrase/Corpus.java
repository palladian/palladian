package ws.palladian.extraction.keyphrase;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

import ws.palladian.classification.FastWordCorrelationMatrix;
import ws.palladian.classification.WordCorrelation;
import ws.palladian.classification.WordCorrelationMatrix;
import ws.palladian.helper.io.FileHelper;

/**
 * The corpus which can be serialized. It contains: all known phrases in the document collection, list of human assigned
 * keyphrases in the training set, and a correlation matrix with manually assigned keyphrases.
 * 
 * @author Philipp Katz
 * 
 */
public class Corpus implements Serializable {

    private static final long serialVersionUID = 5266995506326505479L;

    /** All known phrases and their occurrence counts. */
    private Bag<String> phrases = new HashBag<String>();

    /** All explicitly assigned keyphrases in the training set. */
    private Bag<String> keyphrases = new HashBag<String>();

    /** Number of documents in this corpus. */
    private int documentCount;

    /** Matrix with keyphrase correlations. */
    private WordCorrelationMatrix correlations = new FastWordCorrelationMatrix();

    /**
     * Add phrases for the inverse document frequency index.
     * 
     * @param phrases
     */
    public void addPhrases(List<Token> phrases) {

        // only count each phrase once
        Set<String> phraseSet = new HashSet<String>();
        for (Token token : phrases) {
            phraseSet.add(token.getStemmedValue());
        }
        this.phrases.addAll(phraseSet);
        documentCount++;
    }

    /**
     * Add an explicitly assigned/trained keyphrase to the corpus.
     * 
     * @param keyphrases
     */
    public void addKeyphrases(Set<String> keyphrases) {
        this.keyphrases.addAll(keyphrases);
        correlations.updateGroup(keyphrases);
    }

    /**
     * Get the inverse document frequency for the specified phrase.
     * 
     * @param phrase
     * @return
     */
    public float getInverseDocumentFrequency(Candidate candidate) {
        int termCount = phrases.getCount(candidate.getStemmedValue());
        float idf = (float) Math.log10((float) documentCount / (termCount + 1));

        // do not return negative values.
        return Math.max(0, idf);
    }

    /**
     * Get the occurrence probability for the specified phrase.
     * 
     * @param phrase
     * @return
     */
    public float getPrior(Candidate candidate) {

        // average occurrence of each specific phrase
        float avgOccurence = (float) keyphrases.size() / keyphrases.uniqueSet().size();

        // probability of occurrence of specified phrase
        // TODO toLowerCase necessary?
        float prior = (float) keyphrases.getCount(candidate.getStemmedValue()/*.toLowerCase()*/) / avgOccurence;

        // for debugging
        assert !Float.isNaN(prior);

        return prior;
    }
    
    // TODO experimental
    public boolean isKeyphrase(Candidate candidate) {
        return keyphrases.contains(candidate.getStemmedValue());
    }

    /**
     * Calculate relative {@link WordCorrelation} scores for the matrix.
     */
    public void makeRelativeScores() {
        correlations.makeRelativeScores();
    }

    /**
     * Get the {@link WordCorrelation} for two {@link Candidate}s.
     * 
     * @param candidate1
     * @param candidate2
     * @return
     */
    public WordCorrelation getCorrelation(Candidate candidate1, Candidate candidate2) {

        // special treatment of phrases -- if we have a value like "San Francisco"
        // we simply concatenate the *unstemmed* terms together, like "sanfrancisco";
        // this resembles human tagging behavior, for example in Delicious.
        String value1 = candidate1.getStemmedValue();
        /// XXX
                    if (value1.contains(" ")) {
                        value1 = candidate1.getValue().replaceAll(" ", "").toLowerCase();
                    }
        String value2 = candidate2.getStemmedValue();
                    if (value2.contains(" ")) {
                        value2 = candidate2.getValue().replaceAll(" ", "").toLowerCase();
                    }

        WordCorrelation correlation = correlations.getCorrelation(value1, value2);
        return correlation;

    }
    
    // TODO no longer necessary
//    public List<WordCorrelation> getCorrelations(Candidate candidate) {
//        String value1 = candidate.getStemmedValue();
//        if (value1.contains(" ")) {
//            value1 = candidate.getValue().replaceAll(" ", "").toLowerCase();
//        }
//        return correlations.getCorrelations(value1, -1);
//    }
    
    /**
     * Clear all contents from this corpus.
     */
    public void clear() {
        phrases.clear();
        keyphrases.clear();
        correlations.clear();
        documentCount = 0;
    }
    
    @Override
    public String toString() {
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("Corpus\n");
        sb.append("# phrases: ").append(phrases.uniqueSet().size()).append("\n");
        sb.append("# keyphrases: ").append(keyphrases.uniqueSet().size()).append("\n");
        sb.append("# correlations: ").append(correlations.getCorrelations().size()).append("\n");
        
        return sb.toString();
        
        
    }
    
    public static void main(String[] args) {
        
        Corpus corpus = FileHelper.deserialize("data/xyz.ser");
        System.out.println(corpus.toString());
        
        // System.out.println(corpus.phrases.getCount("ipad"));
        // System.out.println(corpus.keyphrases.getCount("ipad"));
        
        
    }

}
