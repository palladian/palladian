package tud.iir.extraction.keyphrase;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.wikipedia.miner.model.Wikipedia;

import tud.iir.classification.controlledtagging.Token;
import tud.iir.classification.controlledtagging.TokenizerPlus;
import tud.iir.helper.StopWatch;

import maui.filters.MauiFilter;
import maui.util.Candidate;

public class Test {
    
    public static void main(String[] args) {
        
        try {
            new Wikipedia("localhost", "wikipedia", "", "");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.exit(0);
        
        
        String text = "the quick brown fox. the quick brown car. the lazy fox and the quick dog. the fox is brown.";
        StopWatch sw = new StopWatch();
        for (int i = 0; i < 10000000; i++) {
            text.replaceAll(" ", "");
            // text.replace(" ", ""); // 1m:3s:445ms
        }
        System.out.println(sw);
        System.exit(0);
        
        MauiFilter filter = new MauiFilter();
        filter.setMaxPhraseLength(2);
        filter.setVocabularyName("none");
        
        TokenizerPlus tokenizer = new TokenizerPlus();
        
        HashMap<String, Candidate> candidates = filter.getCandidates(text);
        
        Set<Entry<String, Candidate>> entrySet = candidates.entrySet();
        for (Entry<String, Candidate> entry : entrySet) {
            Candidate candidate = entry.getValue();
            System.out.println(candidate+ "\t" + candidate.getFrequency());
            // System.out.println(candidate.getInfo());
        }
        
        List<Token> tokens = tokenizer.tokenize(text);
        List<Token> collocations = tokenizer.makeCollocations(tokens, 5);
        
        for (Token token : collocations) {
            System.out.println(token);
        }
        
    }

}
