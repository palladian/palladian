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
import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.collections15.map.LazyMap;
import org.apache.commons.lang.StringUtils;

import tud.iir.classification.WordCorrelation;
import tud.iir.helper.StringHelper;

/**
 * The DocumentModel represents a document with its tokens/phrases. From all tokens we determine a list of Candidates
 * and their features which can then be ranked.
 * 
 * @author Philipp Katz
 * 
 */
public class DocumentModel extends ArrayList<Candidate> {

    private static final long serialVersionUID = 1L;

    /** Reference to the document corpus. */
    private Corpus corpus;

    /** Map containing stem as key, all belonging tokens as values. */
    private Map<String, List<Token>> tokens;

    /** Number of tokens in the document. */
    private int tokenCount;

    /**
     * Create a new DocumentModel.
     * 
     * @param corpus
     */
    public DocumentModel(Corpus corpus) {
        this.corpus = corpus;

        // LazyMap; automatically create ArrayList on demand.
        tokens = LazyMap.decorate(new LinkedHashMap<String, List<Token>>(), new Factory<List<Token>>() {
            @Override
            public List<Token> create() {
                return new ArrayList<Token>();
            }
        });

    }

    /**
     * Add a {@link Token} to the document model.
     * 
     * @param token
     */
    public void addToken(Token token) {
        List<Token> tokenList = tokens.get(token.getStemmedValue());
        tokenList.add(token);
    }

    /**
     * Add a List of {@link Token}s to the document model.
     * 
     * @param tokens
     */
    public void addTokens(List<Token> tokens) {
        for (Token token : tokens) {
            addToken(token);
        }
    }

    /**
     * Determine the candidates from all stored Tokens.
     */
    public void createCandidates() {

        // first clear the list, if we should already have candidates.
        clear();

        Iterator<Entry<String, List<Token>>> iterator = tokens.entrySet().iterator();

        while (iterator.hasNext()) {

            Entry<String, List<Token>> current = iterator.next();
            List<Token> tokens = current.getValue();

            // store all unstemmed representations
            Bag<String> unStemBag = new HashBag<String>();

            Candidate candidate = new Candidate(this);
            add(candidate);

            // from the list of tokens extract the candidate features
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

                tokenCount++;

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

        // save memory; we don't need the Tokens any longer.
        tokens.clear();

        calculateCorrelations();

    }

    /**
     * Determine the correlation feature for the candidates.
     */
    private void calculateCorrelations() {

        Candidate[] candidateArray = toArray(new Candidate[size()]);

        for (int i = 0; i < candidateArray.length; i++) {
            Candidate cand1 = candidateArray[i];
            for (int j = i; j < candidateArray.length; j++) {
                Candidate cand2 = candidateArray[j];

                WordCorrelation correlation = corpus.getCorrelation(cand1, cand2);
                if (correlation != null) {
                    double correlationValue = correlation.getRelativeCorrelation();
                    cand2.addCorrelation(correlationValue);
                    cand1.addCorrelation(correlationValue);
                }

            }
        }
    }
    
    // TODO experimental
    public void removeNonKeyphrases() {
        
        ListIterator<Candidate> iterator = this.listIterator();
        while (iterator.hasNext()) {
            Candidate current = iterator.next();
            if (!corpus.isKeyphrase(current)) {
                iterator.remove();
            }
        }
        
    }

    /**
     * Get the number of tokens in the document. Non-unique tokens are counted multiple times.
     * 
     * @return
     */
    public int getTokenCount() {
        return tokenCount;
    }

    /**
     * Get the {@link Corpus}.
     * 
     * @return
     */
    public Corpus getCorpus() {
        return corpus;
    }

    /**
     * Get a CSV representation of the DocumentModel with all Candidates and their features.
     * 
     * @return
     */
    public String toCSV() {
        return toCSV(false);
    }

    /**
     * Get a CSV representation of the DocumentModel with all {@link Candidate}s and their features.
     * 
     * @param writeHeader <code>true</code> to add header with column labels.
     * @return
     */
    @Deprecated
    public String toCSV(boolean writeHeader) {
        StringBuilder sb = new StringBuilder();

        // write CSV header with features names
        if (writeHeader) {
            Candidate first = iterator().next();
            Set<String> featureNames = first.getFeatures().keySet();
            sb.append("#").append(StringUtils.join(featureNames, ";")).append("\n");
        }

        // write all values
        for (Candidate candidate : this) {
            Collection<Double> feautureValues = candidate.getFeatures().values();
            sb.append(StringUtils.join(feautureValues, ";")).append("\n");
        }

        return sb.toString();
    }

    /**
     * Returns a more detailed toString representation.
     * 
     * @return
     */
    public String toLongString() {
        StringBuilder builder = new StringBuilder();
        for (Candidate candidate : this) {
            builder.append(candidate).append("\n");
        }
        builder.append("# of unique candidates : " + size()).append("\n");
        builder.append("# tokens : " + tokenCount);
        return builder.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (Candidate candidate : this) {
            builder.append(candidate.getStemmedValue()).append(", ");
        }
        if (builder.length() > 1) {
            builder.delete(builder.length() - 2, builder.length());
        }
        builder.append("]");
        return builder.toString();
    }

}