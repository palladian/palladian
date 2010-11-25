package tud.iir.classification.controlledtagging;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

import tud.iir.classification.FastWordCorrelationMatrix;
import tud.iir.classification.WordCorrelation;
import tud.iir.classification.WordCorrelationMatrix;

public class Corpus implements Serializable {

    private static final long serialVersionUID = 5266995506326505479L;

    private Bag<String> idfIndex = new HashBag<String>();
    private Bag<String> controlledIndex = new HashBag<String>();

    private int idfCount;
    
    // private WordCorrelationMatrix wcm = new WordCorrelationMatrix();
    private WordCorrelationMatrix wcm = new FastWordCorrelationMatrix();

    public void addTokens(List<Token> tokens) {
        Set<String> temp = new HashSet<String>();
        for (Token token : tokens) {
            temp.add(token.getStemmedValue());
        }
        idfIndex.addAll(temp);
        idfCount++;
    }
    
    // TODO rename
    public void addTags(Set<String> tags) {
        controlledIndex.addAll(tags);
        // TODO no wcm for now.
        //addToWcm(tags);
        wcm.updateGroup(tags);
    }
    
    public float getInverseDocumentFrequency(String term) {
        int termCount = idfIndex.getCount(term);
        //float idf = (float) Math.log10((float) idfCount / (termCount));
        float idf = (float) Math.log10((float) idfCount / (termCount + 1 ));

        // do not return negative values.
        idf = Math.max(0, idf);
        return idf;
    }
    
    public float getPrior(String term) {
        float avgOccurence = (float) controlledIndex.size() / controlledIndex.uniqueSet().size();
        float prior = (float) controlledIndex.getCount(term) / avgOccurence;
//        if (Float.isNaN(prior)) {
//            System.out.println(avgOccurence);
//        }
         assert !Float.isNaN(prior);
        return prior;
    }
    
    public void calcRelCorrelations() {
        wcm.makeRelativeScores();
    }
    
    public WordCorrelation getCorrelation(String term1, String term2) {
        return wcm.getCorrelation(term1, term2);
    }
    
    // I moved this to WCM.
    
//    /**
//     * Add a list of tags to the WordCorrelationMatrix, for a set with size n, we will add
//     * <code>(n - 1) + (n - 2) + ... + 1 = (n * (n - 1)) / 2</code> correlations.
//     * 
//     * TODO move this to the CorrelationMatrix?
//     * 
//     * @param tags
//     */
//    private void addToWcm(Set<String> tags) {
//        String[] tagArray = tags.toArray(new String[tags.size()]);
//
//        for (int i = 0; i < tagArray.length; i++) {
//            for (int j = i + 1; j < tagArray.length; j++) {
//                wcm.updatePair(tagArray[i], tagArray[j]);
//            }
//        }
//    }

}
