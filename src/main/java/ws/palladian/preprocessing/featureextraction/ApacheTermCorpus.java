package ws.palladian.preprocessing.featureextraction;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.collections15.bag.HashBag;
import org.apache.log4j.Logger;

import ws.palladian.helper.FileHelper;

public class ApacheTermCorpus extends TermCorpus {
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ApacheTermCorpus.class);

    private HashBag<String> terms;

    public ApacheTermCorpus() {
        terms = new HashBag<String>();
    }

    @Override
    public void addTermsFromDocument(Set<String> terms) {
        this.terms.addAll(terms);
        incrementNumDocs();
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

//    public static TermCorpus deserialize(String filePath) {
//        return (ApacheTermCorpus) FileHelper.deserialize(filePath);
//    }

    @Override
    protected void setDf(String term, int df) {
        this.terms.remove(term, this.terms.getCount(term));
        this.terms.add(term, df);
    }

    @Override
    public void save(String fileName) {
        OutputStream outputStream = null;
        PrintWriter printWriter = null;
        
        try {
            outputStream = new GZIPOutputStream(new FileOutputStream(fileName));
            printWriter = new PrintWriter(outputStream);
            
            printWriter.println("numDocs" + SEPARATOR + getNumDocs());
            printWriter.println();
            
            for (String term : this.terms.uniqueSet()) {
                int count = this.terms.getCount(term);
                String line = term+ SEPARATOR + count;
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

    @Override
    protected int getTermCount(String term) {
        return this.terms.getCount(term);
    }

}
