package ws.palladian.extraction.feature;

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import ws.palladian.helper.io.FileHelper;

public class TroveTermCorpus extends TermCorpus {
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(TroveTermCorpus.class);
    
    private TObjectIntHashMap termMap;

    public TroveTermCorpus() {
        super();
        termMap = new TObjectIntHashMap();
    }
    
    public TroveTermCorpus(String filePath) {
        this();
        load(filePath);
    }

    /*
     * (non-Javadoc)
     * @see com.newsseecr.xperimental.preprocessing.TermCorpus#addTermsFromDocument(java.util.Set)
     */
    @Override
    public void addTermsFromDocument(Set<String> terms) {
        for (String term : terms) {
            //termMap.adjustOrPutValue(term, 1, 1);
            // TODO untested workaround caused by stupid trove version problems
        	int termValue = termMap.get(term);
            if (termValue < 1) {
            	termMap.put(term, 1);
            } else {
            	termMap.adjustValue(term, 1);
            }
        }
        incrementNumDocs();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TroveTermCorpus");
        sb.append(" numDocs=").append(getNumDocs());
        sb.append(" numUniqueTerms=").append(termMap.size());
        return sb.toString();
    }

    @Override
    public void save(String fileName) {
        
        OutputStream outputStream = null;
        PrintWriter printWriter = null;
        
        try {
            outputStream = new GZIPOutputStream(new FileOutputStream(fileName));
            printWriter = new PrintWriter(outputStream);
            
            TObjectIntIterator iterator = termMap.iterator();
            printWriter.println("numDocs" + SEPARATOR + getNumDocs());
            printWriter.println();
            
            while (iterator.hasNext()) {
                iterator.advance();
                String key = (String) iterator.key();
                int value = iterator.value();
                String line = key + SEPARATOR + value;
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
    protected void setDf(String term, int df) {
        termMap.put(term, df);
    }

    @Override
    protected int getTermCount(String term) {
        return termMap.get(term);
    }

}
