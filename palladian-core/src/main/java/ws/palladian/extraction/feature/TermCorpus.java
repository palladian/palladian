package ws.palladian.extraction.feature;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Set;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.log4j.Logger;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

public class TermCorpus {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(TermCorpus.class);
    private static final String SEPARATOR = "#";

    private int numDocs;
    private final Bag<String> terms;

    public TermCorpus() {
        this(new HashBag<String>(), 0);
    }
    
    public TermCorpus(Bag<String> terms, int numDocs) {
        this.numDocs = numDocs;
        this.terms = terms;
    }

    public void addTermsFromDocument(Set<String> terms) {
        this.terms.addAll(terms);
        numDocs++;
    }

    public int getCount(String term) {
        return this.terms.getCount(term);
    }

    public double getDf(String term) {
        int termCount = getCount(term);
        // add 1; prevent division by zero
        double documentFrequency = Math.log10((double) getNumDocs() / (termCount + 1));
        return documentFrequency;
    }

    public int getNumDocs() {
        return numDocs;
    }

    private void setDf(String term, int df) {
        this.terms.remove(term, this.terms.getCount(term));
        this.terms.add(term, df);
    }

    public void load(String fileName) {
        FileHelper.performActionOnEveryLine(fileName, new LineAction() {
            @Override
            public void performAction(String text, int number) {
                if (number % 100000 == 0) {
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
    }

    public void save(String fileName) {
        OutputStream outputStream = null;
        PrintWriter printWriter = null;

        try {
            outputStream = /* new GZIPOutputStream( */new FileOutputStream(fileName)/* ) */;
            printWriter = new PrintWriter(outputStream);

            printWriter.println("numDocs" + SEPARATOR + getNumDocs());
            printWriter.println();

            for (String term : this.terms.uniqueSet()) {
                int count = this.terms.getCount(term);
                String line = term + SEPARATOR + count;
                printWriter.println(line);
            }

        } catch (FileNotFoundException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        } finally {
            FileHelper.close(outputStream, printWriter);
        }
    }
    
    public void reset() {
        this.numDocs = 0;
        this.terms.clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ApacheTermCorpus");
        sb.append(" numDocs=").append(getNumDocs());
        sb.append(" numUniqueTerms=").append(terms.uniqueSet().size());
        sb.append(" numTerms=").append(terms.size());
        return sb.toString();
    }

    public static void main(String[] args) {
        TermCorpus termCorpus = new TermCorpus();
        termCorpus.load("/Users/pk/Desktop/corpus.txt");
        System.out.println(termCorpus);
    }

}