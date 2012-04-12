package ws.palladian.helper.shingling;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.log4j.Logger;

import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;

import com.planetj.math.rabinhash.RabinHashFunction64;

/**
 * Simplified Shingle implementation to detect near-duplicate documents.
 * 
 * All documents added are stored with an ID and their corresponding sketches in an index, to allow lookups for
 * duplicates.
 * 
 * http://www.ida.liu.se/~TDDC03/oldprojects/2005/final-projects/prj10.pdf
 * http://www.cs.princeton.edu/courses/archive/spr05/cos598E/bib/Princeton.pdf
 * http://phpir.com/shingling-near-duplicate-detection
 * http://www.std.org/~msm/common/clustering.html
 * http://isabel-drost.de/projects/tuberlin/imsem2010/dups_paper_2010.pdf
 * http://codingplayground.blogspot.com/2008/06/shingling-and-text-clustering.html
 * 
 * TODO useful preprocessing steps?
 * make lower case, remove punctation, remove duplicate white space?
 * 
 * @author Philipp Katz
 * 
 */
public class Shingles {

    /** class logger. */
    private static final Logger LOGGER = Logger.getLogger(Shingles.class);

    /** the index to store all shingles and mappings between similar documents. */
    private ShinglesIndex index;

    public static final int DEFAULT_N_GRAM_LENGTH = 3;

    public static final int DEFAULT_SKETCH_SIZE = 200;

    public static final float DEFAULT_SIMILARITY_THRESHOLD = 0.1f;

    private int nGramLength = DEFAULT_N_GRAM_LENGTH;

    private int sketchSize = DEFAULT_SKETCH_SIZE;

    private float similarityThreshold = DEFAULT_SIMILARITY_THRESHOLD;

    public Shingles() {
        this.index = new ShinglesIndexJava();
    }

    /**
     * Initalize with a specific {@link ShinglesIndex} implementation.
     * 
     * @param index
     */
    public Shingles(ShinglesIndex index) {
        LOGGER.debug("using " + index);
        this.index = index;
        index.openIndex();
    }

    /**
     * Calculate the sketch for a String, consisting of a subset of size {@link #getSketchSize()} of all hashed word
     * n-grams.
     * 
     * @param string
     * @return Set with hash values.
     */
    protected Set<Long> getSketch(String string) {

        string = preprocess(string);

        // calculate all w-Shingles
        Set<String> shingles = Tokenizer.calculateWordNGrams(string, getnGramLength());

        // calculate all hashes for w-Shingles
        Set<Long> shingleHashes = new HashSet<Long>();
        for (String nGram : shingles) {
            // long hash = nGram.hashCode();
            RabinHashFunction64 hashFunction = RabinHashFunction64.DEFAULT_HASH_FUNCTION;
            long hash = hashFunction.hash(nGram);
            shingleHashes.add(hash);
        }

        // the document's "sketch", consisting of a subset of all hashed w-Shingles
        Set<Long> result = getMinN(shingleHashes, getSketchSize());

        return result;

    }

    protected String preprocess(String string) {
        string = string.toLowerCase();
        string = StringHelper.trim(string);
        return string;
    }

    /**
     * Add a document's content to the shingle collection. A document is uniquely represented by an ID.
     * 
     * @param documentId
     * @param documentContent
     * @return <code>true</code>, if document was similar/dupicate.
     */
    public boolean addDocument(int documentId, String documentContent) {
        Set<Long> sketch = getSketch(documentContent);
        boolean similar = checkSimilarity(documentId, sketch);
        if (!similar) {
            index.addDocument(documentId, sketch);
        }
        return similar;
    }

    int count = 1;

    /**
     * Add a file to the shingle collection.
     * 
     * @param filePath
     * @return <code>true</code>, if document was similar/duplicate.
     */
    public boolean addFile(String filePath) {
        String fileContent = FileHelper.readFileToString(filePath);
        // int documentId = index.getNumberOfDocuments() + 1;
        int documentId = count++;
        return addDocument(documentId, fileContent);
    }

    /**
     * Adds multiple documents from one file to the shingle collection. Each document is on its own line. Line number is
     * the document's ID.
     * 
     * @param filePath
     * @return list of document IDs which already have similar/identical documents in the collection.
     */
    public Collection<Integer> addDocumentsFromFile(String filePath) {
        final Collection<Integer> duplicateDocumentIds = new HashSet<Integer>();
        FileHelper.performActionOnEveryLine(filePath, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                boolean duplicate = addDocument(lineNumber, line);
                if (duplicate) {
                    duplicateDocumentIds.add(lineNumber);
                }
            }
        });
        return duplicateDocumentIds;
    }

    /**
     * Check similarity for a document represented by its sketch with all documents in the index.
     * 
     * @param documentId
     * @param sketch
     * @return <code>true</code>, if document was similar/duplicate.
     */
    private boolean checkSimilarity(int documentId, Set<Long> sketch) {

        // System.out.println("------ " + documentId + " -----");

        boolean result = false;
        StringBuilder debugMessage = new StringBuilder();
        debugMessage.append("doc:" + documentId + ":");

        // /////////////
        // TODO some implementations are faster using the old implementation, some with the "speed up"
        // move parts of this code to the appropriate index implementations
        // ///////////////

        // //////////////////////// old implementation, slow ////////////////////
        // get all documents we need to check, i.e. all documents which contain one of the hashes

        // Map<Integer, Set<Long>> documentsToCheck = index.getDocumentsForSketch(sketch);
        // determine all similar/identical documents by calculating the Jaccard distance
//        Set<Integer> similarDocuments = new HashSet<Integer>();
//        Iterator<Entry<Integer, Set<Long>>> iterator = documentsToCheck.entrySet().iterator();
//        while (iterator.hasNext()) {
//            Entry<Integer, Set<Long>> document = iterator.next();
//            // don't count the current document itself
//            if (document.getKey() == documentId) {
//                continue;
//            }
//            float distance = jaccardDistance(document.getValue(), sketch);
//            if (distance == 0) {
//                // identical document
//                debugMessage.append(" id:" + document.getKey());
//                similarDocuments.add(document.getKey());
//            } else if (distance < getSimilarityThreshold()) {
//                // similar document
//                debugMessage.append(" sim(" + distance + "):" + document.getKey());
//                similarDocuments.add(document.getKey());
//            }
//        }

        // //////////////////////// try to speed up /////////////////////////

         Bag<Integer> matchingDocs = new HashBag<Integer>();
         for (long hash : sketch) {
         Set<Integer> docsForHash = index.getDocumentsForHash(hash);
         matchingDocs.addAll(docsForHash);
         }
         // similarity candidates are in the Bag, which counts the number of matching hashes
         Set<Integer> similarDocs = new HashSet<Integer>();
         for (int curDocId : matchingDocs.uniqueSet()) {
         if (1 - (float) matchingDocs.getCount(curDocId) / sketch.size() < similarityThreshold) {
         similarDocs.add(curDocId);
         }
         }

        // determine all similar/identical documents by calculating the Jaccard distance
        Set<Integer> similarDocuments2 = new HashSet<Integer>();

        for (int simDocId : similarDocs) {

            Set<Long> simSketch = index.getSketchForDocument(simDocId);

            // don't count the current document itself
            if (simDocId == documentId) {
                continue;
            }
            float distance = jaccardDistance(simSketch, sketch);
            if (distance == 0) {
                // identical document
                debugMessage.append(" id:" + simDocId);
                similarDocuments2.add(simDocId);
            } else if (distance < getSimilarityThreshold()) {
                // similar document
                debugMessage.append(" sim(" + distance + "):" + simDocId);
                similarDocuments2.add(simDocId);
            }

            // TODO we could break the loop, when we found a similarity?
        }

        // if we found similar documents, add the similarity relation to the index;
        // we treat the document with the lowest ID as "master" document which references
        // all similar/identical documents
        if (similarDocuments2.size() > 0) {
            LOGGER.debug(debugMessage);
            index.addDocumentSimilarity(Collections.min(similarDocuments2), documentId);
            result = true;
        } else {
            debugMessage.append(" looks unique.");
            LOGGER.debug(debugMessage);
        }
        return result;
    }

    /**
     * Get a map with similar documents. E.g. [1 -> 5, 6, 10]
     * 
     * @return
     */
    public Map<Integer, Set<Integer>> getSimilarDocuments() {
        return index.getSimilarDocuments();
    }

    public String getSimilarityReport() {
        StringBuilder report = new StringBuilder();
        report.append("---------- similar documents -----------\n");
        Map<Integer, Set<Integer>> similarDocuments = getSimilarDocuments();
        Iterator<Entry<Integer, Set<Integer>>> iterator = similarDocuments.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Integer, Set<Integer>> similarDocs = iterator.next();
            report.append(similarDocs.getKey() + " : " + similarDocs.getValue() + "\n");
        }
        report.append("----------------------------------------\n");
        report.append("# of total documents " + index.getNumberOfDocuments());
        return report.toString();
    }

    public int getnGramLength() {
        return nGramLength;
    }

    /**
     * Set length of shingles/n-grams.
     * 
     * @param nGramLength
     */
    public void setnGramLength(int shingleLength) {
        this.nGramLength = shingleLength;
    }

    public int getSketchSize() {
        return sketchSize;
    }

    /**
     * Set size of the sketch,
     * 
     * @param sketchSize
     */
    public void setSketchSize(int sketchSize) {
        this.sketchSize = sketchSize;
    }

    public float getSimilarityThreshold() {
        return similarityThreshold;
    }

    /**
     * Set threshold when two documents are considered "near duplicates".
     * 
     * @param similarityThreshold
     */
    public void setSimilarityThreshold(float similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * Calculate Jaccard distance. The bigger the result, the more dissimilar are the two sets.
     * http://en.wikipedia.org/wiki/Jaccard_index
     * 
     * @param s1
     * @param s2
     * @return value between inclusive 0 and 1. Bigger value means more dissimilar.
     */
    public static float jaccardDistance(Set<?> s1, Set<?> s2) {
        Set<Object> setUnion = new HashSet<Object>();
        setUnion.addAll(s1);
        setUnion.addAll(s2);

        Set<Object> setIntersection = new HashSet<Object>();
        for (Object term : setUnion) {
            if (s1.contains(term) && s2.contains(term)) {
                setIntersection.add(term);
            }
        }

        int union = setUnion.size();
        int intersection = setIntersection.size();

        float jacDist = (float) (union - intersection) / union;
        LOGGER.trace("union:" + union + " intersection:" + intersection + " jacDist:" + jacDist);
        return jacDist;
    }

    /**
     * Returns the "minimum" n items of the specified set, which are determined via their
     * {@link Comparable#compareTo(Object)} methods.
     * 
     * @param <T>
     * @param set the input Set.
     * @param n the number of mimimum elements to return.
     * @return the n minimum elements of the set.
     */
    public static <T extends Object & Comparable<T>> Set<T> getMinN(Set<T> set, int n) {

        // create a shallow copy of input as we will do modifications
        Set<T> temp = new TreeSet<T>(set);

        Set<T> result = new TreeSet<T>();

        // get n "smallest" items
        for (int i = 0; i < Math.min(set.size(), n); i++) {
            T min = Collections.min(temp);
            result.add(min);
            temp.remove(min);
        }

        return result;
    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) throws Exception {

        /*
         * // List<String> test = Collections.emptyList();
         * // test.iterator();
         * // System.exit(0);
         * // ShinglesIndexTracer tracer = new ShinglesIndexTracer(new ShinglesIndexJDBM());
         * // ShinglesIndexTracer tracer = new ShinglesIndexTracer(new ShinglesIndexJava());
         * ShinglesIndexTracer tracer = new ShinglesIndexTracer(new ShinglesIndexH2());
         * // ShinglesIndexTracer tracer = new ShinglesIndexTracer(new ShinglesIndexWB());
         * Shingles s = new Shingles(tracer);
         * s.addDocumentsFromFile("data/test_entries_10000.txt");
         * System.out.println(tracer.getTraceResult());
         * System.exit(0);
         */
        // ///////////////////////////////////////////////////

        CommandLineParser parser = new BasicParser();

        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("input").withDescription("input file").isRequired().hasArg()
                .withArgName("fileName").create());
        options.addOption(OptionBuilder.withLongOpt("output").withDescription("output file").isRequired().hasArg()
                .withArgName("fileName").create());

        try {

            if (args.length < 1) {
                // no arguments given, print usage help in catch clause.
                throw new ParseException(null);
            }

            CommandLine cmd = parser.parse(options, args);

            String inputFile = cmd.getOptionValue("input");
            final String outputFile = cmd.getOptionValue("output");
            if (FileHelper.fileExists(outputFile)) {
                LOGGER.info("file " + outputFile + " exists. deleting.");
            }

            final Shingles shingles = new Shingles();
            final int[] dupCount = new int[] { 0 };
            StopWatch stopWatch = new StopWatch();

            FileHelper.performActionOnEveryLine(inputFile, new LineAction() {
                @Override
                public void performAction(String line, int lineNumber) {
                    boolean dup = shingles.addDocument(lineNumber, line);
                    if (!dup) {
                        FileHelper.appendFile(outputFile, line + "\n");
                    } else {
                        dupCount[0]++;
                    }
                }
            });

            LOGGER.info("wrote result to: " + outputFile);
            LOGGER.info("number of (near)duplicates: " + dupCount[0]);
            LOGGER.info("elapsed time for de-duplication: " + stopWatch.getElapsedTimeString());

        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Shingles [options]", options);
        }

        // StopWatch stopWatch = new StopWatch();
        // Shingles s = new Shingles();
        // // Shingles s = new Shingles(new ShinglesIndexH2());
        // s.setShingleLength(3);
        // s.setSimilarityThreshold(0.1f);
        // s.setSketchSize(100);
        //
        // // s.addFile("data/shingleTest/cluster1");
        // // s.addFile("data/shingleTest/cluster2");
        // // s.addFile("data/shingleTest/cluster3");
        // // s.addFile("data/shingleTest/cluster3_a");
        // // s.addFile("data/shingleTest/cluster4");
        // // s.addFile("data/shingleTest/cluster4_a");
        // // s.addFile("data/shingleTest/cluster4_b");
        // // s.addFile("data/shingleTest/cluster4_b~");
        // // s.addFile("data/shingleTest/cluster5");
        // // s.addFile("data/shingleTest/cluster5_a");
        //
        // // s.addDocumentsFromFile("data/test_entries_1000.txt");
        // s.addDocumentsFromFile("data/tag_training__NEW__.txt");
        //
        // System.out.println("checked in " + stopWatch.getElapsedTimeString());
        // System.out.println(s.getSimilarityReport());

    }

    /**
     * 
     * @see ws.palladian.helper.shingling.ShinglesIndex#saveIndex()
     */
    public void saveIndex() {
        index.saveIndex();
    }

}
