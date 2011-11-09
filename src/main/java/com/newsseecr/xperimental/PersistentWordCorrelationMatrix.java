package com.newsseecr.xperimental;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.RecordManagerOptions;
import jdbm.btree.BTree;
import jdbm.helper.StringComparator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import ws.palladian.classification.Term;
import ws.palladian.classification.WordCorrelation;
import ws.palladian.classification.WordCorrelationMatrix;
import ws.palladian.extraction.keyphrase.evaluation.DeliciousDatasetReader;
import ws.palladian.extraction.keyphrase.evaluation.DeliciousDatasetReader.DatasetCallback;
import ws.palladian.extraction.keyphrase.evaluation.DeliciousDatasetReader.DatasetEntry;
import ws.palladian.helper.Counter;
import ws.palladian.helper.StopWatch;

@SuppressWarnings("serial")
public class PersistentWordCorrelationMatrix extends WordCorrelationMatrix {

    /** manager for the JDBM data structures. */
    private RecordManager recordManager;

    /** BTree for absolute word correlations (Integer). */
    private BTree absoluteCorrelations;

    /** BTree for relative word correlations (Double). */
    private BTree relativeCorrelations;

    public PersistentWordCorrelationMatrix() {

        try {

            Properties properties = new Properties();
            properties.setProperty(RecordManagerOptions.DISABLE_TRANSACTIONS, "true");

            recordManager = RecordManagerFactory.createRecordManager("wcmIndex", properties);

            absoluteCorrelations = loadOrCreateBTree(recordManager, "absoluteCorrelations", new StringComparator());
            relativeCorrelations = loadOrCreateBTree(recordManager, "relativeWordCorrelations", new StringComparator());

        } catch (IOException e) {
            LOGGER.error(e);
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public void updatePair(String word1, String word2) {

        WordCorrelation correlation = getCorrelation(word1, word2);

        if (correlation == null) {
            correlation = new WordCorrelation(getTerm(word1), getTerm(word2));
            correlation.setAbsoluteCorrelation(1.0);
        } else {
            correlation.increaseAbsoluteCorrelation(1.0);
        }

        try {

            Map<String, Integer> correlations = (Map<String, Integer>) absoluteCorrelations.find(word1);
            if (correlations == null) {
                correlations = new HashMap<String, Integer>();
            }
            correlations.put(word2, (int) correlation.getAbsoluteCorrelation());
            absoluteCorrelations.insert(word1, correlations, true);
            recordManager.commit();

            Map<String, Integer> correlations2 = (Map<String, Integer>) absoluteCorrelations.find(word2);
            if (correlations2 == null) {
                correlations2 = new HashMap<String, Integer>();
            }
            correlations2.put(word1, (int) correlation.getAbsoluteCorrelation());
            absoluteCorrelations.insert(word2, correlations2, true);
            recordManager.commit();

        } catch (IOException e) {
            LOGGER.error(e);
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public WordCorrelation getCorrelation(String word1, String word2) {

        WordCorrelation result = null;

        try {

            Map<String, Integer> correlations = (Map<String, Integer>) absoluteCorrelations.find(word1);

            if (correlations != null && correlations.containsKey(word2)) {

                result = new WordCorrelation(new Term(word1), new Term(word2));
                result.setAbsoluteCorrelation(correlations.get(word2));

            }

            Map<String, Double> relCorrMap = (Map<String, Double>) relativeCorrelations.find(word1);
            if (relCorrMap != null && relCorrMap.containsKey(word2)) {

                result.setRelativeCorrelation(relCorrMap.get(word2));

            }

        } catch (IOException e) {
            LOGGER.error(e);
        }

        return result;

    }

    @SuppressWarnings("unchecked")
    @Override
    public List<WordCorrelation> getCorrelations(String word, int minCooccurrences) {

        List<WordCorrelation> result = new ArrayList<WordCorrelation>();

        try {

            Map<String, Integer> correlations = (Map<String, Integer>) absoluteCorrelations.find(word);

            if (correlations != null) {

                Set<Entry<String, Integer>> entrySet = correlations.entrySet();
                for (Entry<String, Integer> entry : entrySet) {

                    Integer absCorrelation = entry.getValue();
                    if (absCorrelation >= minCooccurrences) {
                        String word2 = entry.getKey();
                        WordCorrelation wc = new WordCorrelation(new Term(word), new Term(word2));
                        wc.setAbsoluteCorrelation(absCorrelation);
                        result.add(wc);
                    }

                }

            }

        } catch (IOException e) {
            LOGGER.error(e);
        }

        return result;

    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<WordCorrelation> getCorrelations() {

        Set<WordCorrelation> result = new HashSet<WordCorrelation>();

        try {

            TupleBrowser tupleBrowser = absoluteCorrelations.browse();
            Tuple tuple = new Tuple();

            while (tupleBrowser.getNext(tuple)) {

                Map<String, Integer> correlations = (Map<String, Integer>) tuple.getValue();
                String word1 = (String) tuple.getKey();

                Set<Entry<String, Integer>> entrySet = correlations.entrySet();

                for (Entry<String, Integer> entry : entrySet) {

                    String word2 = entry.getKey();
                    WordCorrelation wc = new WordCorrelation(new Term(word1), new Term(word2));
                    wc.setAbsoluteCorrelation(entry.getValue());
                    result.add(wc);

                }

            }

        } catch (IOException e) {
            LOGGER.error(e);
        }

        return result;

    }

    @SuppressWarnings("unchecked")
    @Override
    public void makeRelativeScores() {

        // calculate all the row sums for the Terms in advance
        Map<String, Integer> rowSums = new HashMap<String, Integer>();

        try {

            Tuple tuple = new Tuple();
            TupleBrowser tupleBrowser = absoluteCorrelations.browse();

            while (tupleBrowser.getNext(tuple)) {

                Map<String, Integer> absCorrMap = (Map<String, Integer>) tuple.getValue();
                int sum = 0;

                for (Integer absCorr : absCorrMap.values()) {
                    sum += absCorr;
                }

                rowSums.put((String) tuple.getKey(), sum);
            }

        } catch (IOException e) {
            LOGGER.error(e);
        }

        // now, calculate the relative calculations in each row

        try {
            Tuple tuple = new Tuple();
            TupleBrowser tupleBrowser = absoluteCorrelations.browse();

            while (tupleBrowser.getNext(tuple)) {

                Map<String, Integer> absCorrMap = (Map<String, Integer>) tuple.getValue();
                String term1 = (String) tuple.getKey();
                Set<Entry<String, Integer>> entrySet = absCorrMap.entrySet();

                for (Entry<String, Integer> entry : entrySet) {

                    // absolute correlation (frequency of co-occurrence)
                    double absoluteCorrelation = entry.getValue();

                    String term2 = entry.getKey();

                    double sumCorrelation = 0.0;
                    if (term1.equals(term2)) {
                        sumCorrelation = rowSums.get(term1);
                    } else {
                        sumCorrelation = rowSums.get(term1) + rowSums.get(term2) - absoluteCorrelation;
                    }
                    double relativeCorrelation = absoluteCorrelation / sumCorrelation;

                    Map<String, Double> correlations = (Map<String, Double>) relativeCorrelations.find(term1);
                    if (correlations == null) {
                        correlations = new HashMap<String, Double>();
                    }
                    correlations.put(term2, relativeCorrelation);
                    relativeCorrelations.insert(term1, correlations, true);
                    recordManager.commit();

                    Map<String, Double> correlations2 = (Map<String, Double>) relativeCorrelations.find(term2);
                    if (correlations2 == null) {
                        correlations2 = new HashMap<String, Double>();
                    }
                    correlations2.put(term1, relativeCorrelation);
                    relativeCorrelations.insert(term2, correlations2, true);
                    recordManager.commit();

                }

            }
        } catch (IOException e) {
            LOGGER.error(e);

        }

    }

    /**
     * Obtains a BTree used to index objects, or creates it if it does not exist.
     * From: http://www.antonioshome.net/blog/2006/20060224-1.php
     * 
     * @param aRecordManager the database.
     * @param aName the name of the BTree.
     * @param aComparator the Comparator object used to sort the elements of the BTree.
     * @return the BTree with that name.
     * @throws IOException if an I/O error happens.
     */
    private static BTree loadOrCreateBTree(RecordManager aRecordManager, String aName, Comparator<?> aComparator)
            throws IOException {
        // So you can't remember the recordID of the B-Tree? Well, let's
        // try to remember it from its name...
        long recordID = aRecordManager.getNamedObject(aName);
        BTree tree = null;

        if (recordID == 0) {
            LOGGER.debug("create new BTree " + aName);
            // Well, the B-Tree has not been previously stored,
            // so let's create one
            tree = BTree.createInstance(aRecordManager, aComparator);
            // store it with the given name
            aRecordManager.setNamedObject(aName, tree.getRecid());
            // And commit changes
            aRecordManager.commit();
        } else {
            LOGGER.debug("load existing BTree " + aName);
            // Yes, we already created this B-Tree in a previous run,
            // so let's retrieve it from the record manager
            tree = BTree.load(aRecordManager, recordID);
        }
        return tree;
    }

    public static void main(String[] args) {
        
        StopWatch sw = new StopWatch();
        final PersistentWordCorrelationMatrix wcm = new PersistentWordCorrelationMatrix();
        final Counter counter = new Counter();
        DeliciousDatasetReader reader = new DeliciousDatasetReader();
        reader.read(new DatasetCallback() {
            
            @Override
            public void callback(DatasetEntry entry) {
                
                String[] tagArray = entry.getTags().uniqueSet().toArray(new String[0]);

                for (int i = 0; i < tagArray.length; i++) {
                    for (int j = i + 1; j < tagArray.length; j++) {
                        wcm.updatePair(tagArray[i], tagArray[j]);
                    }
                }
                counter.increment();
                System.out.println(counter);
                
            }
        }, 10000);
        System.out.println("trained in " + sw.getElapsedTimeString());
        wcm.makeRelativeScores();
        System.out.println("relatives in " + sw.getElapsedTimeString());
        System.exit(0);
        
        

        PersistentWordCorrelationMatrix m = new PersistentWordCorrelationMatrix();
        m.updatePair("eins", "zwei");
        m.updatePair("eins", "zwei");

        WordCorrelation correlation = m.getCorrelation("eins", "zwei");
        System.out.println(correlation);

        List<WordCorrelation> correlations = m.getCorrelations("eins", 0);
        System.out.println(correlations);

    }

}
