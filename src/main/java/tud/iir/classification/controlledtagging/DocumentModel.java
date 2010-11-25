package tud.iir.classification.controlledtagging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.lang.StringUtils;

import tud.iir.classification.WordCorrelation;
import tud.iir.helper.StringHelper;

public class DocumentModel {

    private Corpus corpus;

    // TODO make LazyMap.
    private Map<String, List<Token>> tokens = new LinkedHashMap<String, List<Token>>();
    
    private List<Candidate> candidates;
    private int wordCount;

    public DocumentModel(Corpus corpus) {
        this.corpus = corpus;
    }

    public void addToken(Token token) {

        List<Token> tokenList;
        if (tokens.containsKey(token.getStemmedValue())) {
            tokenList = tokens.get(token.getStemmedValue());
        } else {
            tokenList = new ArrayList<Token>();
            tokens.put(token.getStemmedValue(), tokenList);
        }
        tokenList.add(token);
        // if (token.getWordCount() == 1) {
        wordCount++;
        // }

    }

    public void createCandidates() {

        List<Candidate> candidates = new ArrayList<Candidate>();
        Iterator<Entry<String, List<Token>>> iterator = tokens.entrySet().iterator();

        while (iterator.hasNext()) {

            Entry<String, List<Token>> current = iterator.next();
            List<Token> tokens = current.getValue();
            Bag<String> unStemBag = new HashBag<String>();

            Candidate candidate = new Candidate(this);
            candidates.add(candidate);
            for (Token token : tokens) {

                unStemBag.add(token.getUnstemmedValue());
                candidate.setStemmedValue(token.getStemmedValue());
                candidate.addPosition(token.getTextPosition());
                candidate.incrementCount();

                boolean notAtSentenceStart = token.getSentencePosition() > 0;
                boolean startsUppercase = StringHelper.startsUppercase(token.getUnstemmedValue());
                boolean notCompletelyUppercase = !StringHelper.isCompletelyUppercase(token.getUnstemmedValue());
                
                if (notAtSentenceStart && startsUppercase && notCompletelyUppercase) {
                    candidate.incrementCapitalCount();
                }

            }

            // determine the unstemmed representations
            String bestUnStemCand = null;
            int bestCount = 0;

            for (String unstemmed : unStemBag.uniqueSet()) {
                int currentCount = unStemBag.getCount(unstemmed);
                if (currentCount > bestCount) {
                    bestCount = currentCount;
                    bestUnStemCand = unstemmed;
                }
            }

            candidate.setValue(bestUnStemCand);
        }

        this.candidates = candidates;
        
        // save memory
        tokens.clear();
        
        
        calculateCorrelations();

    }
    
    private void calculateCorrelations() {
        
        Candidate[] candidateArray = candidates.toArray(new Candidate[0]);
        
        for (int i = 0; i < candidateArray.length; i++) {
            Candidate outerCand = candidateArray[i];
            for (int j = i; j < candidateArray.length; j++) {
                Candidate innerCand = candidateArray[j];

                // 2010-11-24
                String innerValue = innerCand.getStemmedValue();
                if (innerValue.contains(" ")) {
                    innerValue = innerCand.getValue().replaceAll(" ", "").toLowerCase();
                }
                String outerValue = outerCand.getStemmedValue();
                if (outerValue.contains(" ")) {
                    outerValue = outerCand.getValue().replaceAll(" ", "").toLowerCase();
                }
                // //

                WordCorrelation correlation = corpus.getCorrelation(outerValue, innerValue);
                if (correlation != null) {
                    double correlationValue = correlation.getRelativeCorrelation(); 
                    innerCand.addCorrelation(correlationValue);
                    outerCand.addCorrelation(correlationValue);

                }

            }
        }
        
    }

    public int getCandidateCount() {
        int count = 0;
        for (Candidate candidate : candidates) {
            count += candidate.getCount();
        }
        return count;
    }

    public int getWordCount() {
        return wordCount;
    }

    public Collection<Candidate> getCandidates() {
        return candidates;
    }

    public Collection<Candidate> getCandidates(int minCount) {
        List<Candidate> result = new ArrayList<Candidate>();
        for (Candidate candidate : candidates) {
            if (candidate.getCount() >= minCount) {
                result.add(candidate);
            }
        }
        return result;
    }
    
    public void cleanCandidates(int minOccurrenceCount) {
        ListIterator<Candidate> listIterator = candidates.listIterator();
        while (listIterator.hasNext()) {
            Candidate candidate = listIterator.next();
            if (candidate.getCount() < minOccurrenceCount) {
                listIterator.remove();
            }
        }
    }

    public float getInverseDocumentFrequency(Candidate candidate) {
        return corpus.getInverseDocumentFrequency(candidate.getStemmedValue());
    }

    public float getPrior(String tag) {
        return corpus.getPrior(tag);
    }

    public WordCorrelation getCorrelation(String term1, String term2) {
        return corpus.getCorrelation(term1, term2);
    }

    public String toCSV() {
        return toCVS(false);
    }

    public String toCVS(boolean writeHeader) {
        StringBuilder sb = new StringBuilder();

        // write CSV header with features names
        if (writeHeader) {
            Candidate first = candidates.iterator().next();
            Set<String> featureNames = first.getFeatures().keySet();
            sb.append("#").append(StringUtils.join(featureNames, ";")).append("\n");
        }

        // write all values
        for (Candidate candidate : candidates) {
            Collection<Double> feautureValues = candidate.getFeatures().values();
            sb.append(StringUtils.join(feautureValues, ";")).append("\n");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Candidate candidate : candidates) {
            builder.append(candidate).append("\n");
        }
        builder.append("# of non-unique candidates : " + getCandidateCount()).append("\n");
        builder.append("# of unique candidates : " + candidates.size()).append("\n");
        builder.append("# words : " + wordCount);
        return builder.toString();
    }

}