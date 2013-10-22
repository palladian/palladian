package ws.palladian.extraction.feature;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

/**
 * <p>
 * A corpus with terms from documents. Used typically for IDF calculations.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class MapTermCorpus extends AbstractTermCorpus {

    private static final String SEPARATOR = "#";

    private int numDocs;
    private final CountMap<String> terms;

    /**
     * <p>
     * Create a new, empty {@link MapTermCorpus}.
     * </p>
     */
    public MapTermCorpus() {
        this(CountMap.<String>create(), 0);
    }

    /**
     * <p>
     * Create a new {@link MapTermCorpus} with the specified terms and number of documents.
     * </p>
     * 
     * @param terms The terms to add.
     * @param numDocs The number of documents this corpus contains.
     */
    public MapTermCorpus(CountMap<String> terms, int numDocs) {
        this.numDocs = numDocs;
        this.terms = terms;
    }

    /**
     * <p>
     * Add the terms from the specified document and increment the number of documents counter.
     * </p>
     * 
     * @param terms The terms to add.
     */
    public void addTermsFromDocument(Set<String> terms) {
        this.terms.addAll(terms);
        numDocs++;
    }

    /* (non-Javadoc)
     * @see ws.palladian.extraction.feature.ITermCorpus#getCount(java.lang.String)
     */
    @Override
    public int getCount(String term) {
        return terms.getCount(term);
    }

    /* (non-Javadoc)
     * @see ws.palladian.extraction.feature.ITermCorpus#getNumDocs()
     */
    @Override
    public int getNumDocs() {
        return numDocs;
    }

    /* (non-Javadoc)
     * @see ws.palladian.extraction.feature.ITermCorpus#getNumTerms()
     */
    @Override
    public int getNumTerms() {
        return terms.totalSize();
    }

    /* (non-Javadoc)
     * @see ws.palladian.extraction.feature.ITermCorpus#getNumUniqueTerms()
     */
    @Override
    public int getNumUniqueTerms() {
        return terms.uniqueSize();
    }

    private void setDf(String term, int df) {
        terms.set(term, df);
    }

    public void load(String fileName) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = new GZIPInputStream(new FileInputStream(new File(fileName)));
            FileHelper.performActionOnEveryLine(inputStream, new LineAction() {
                @Override
                public void performAction(String text, int number) {
                    if (number != 0 && number % 100000 == 0) {
                        System.out.println(number);
                    }
                    if (number > 1) {
                        String[] split = text.split(SEPARATOR);
                        if (split.length != 2) {
                            // System.err.println(text);
                            return;
                        }
                        setDf(split[0], Integer.parseInt(split[1]));
                    } else if (text.startsWith("numDocs" + SEPARATOR)) {
                        String[] split = text.split(SEPARATOR);
                        numDocs = Integer.parseInt(split[1]);
                    }
                }
            });
        } finally {
            FileHelper.close(inputStream);
        }
    }

    public void save(File file) throws IOException {
        OutputStream outputStream = null;
        PrintWriter printWriter = null;
        try {
            outputStream = new GZIPOutputStream(new FileOutputStream(file));
            printWriter = new PrintWriter(outputStream);
            printWriter.println("numDocs" + SEPARATOR + getNumDocs());
            printWriter.println();
            for (String term : terms.uniqueItems()) {
                int count = terms.getCount(term);
                String line = term + SEPARATOR + count;
                printWriter.println(line);
            }
        } finally {
            FileHelper.close(printWriter, outputStream);
        }
    }

    /**
     * <p>
     * Reset this {@link MapTermCorpus}, i.e. clear all terms and reset the number of documents to zero.
     * </p>
     */
    public void reset() {
        numDocs = 0;
        terms.clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TermCorpus");
        sb.append(" numDocs=").append(getNumDocs());
        sb.append(" numUniqueTerms=").append(terms.uniqueSize());
        sb.append(" numTerms=").append(terms.totalSize());
        return sb.toString();
    }

}