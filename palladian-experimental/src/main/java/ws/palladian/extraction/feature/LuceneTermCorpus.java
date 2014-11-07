package ws.palladian.extraction.feature;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.feature.AbstractTermCorpus;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

// http://stackoverflow.com/questions/19423889/getting-term-counts-in-lucene-4-index
public class LuceneTermCorpus extends AbstractTermCorpus {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneTermCorpus.class);

    /** Field name where the text is stored. */
    static final String FIELD_NAME = "Wordindex";

    /** The Lucene directory instance. */
    private final Directory directory;

    /** Cache the number of docs, once we retrieved it. */
    private Integer numDocs = null;

    public LuceneTermCorpus(Directory directory) {
        this.directory = directory;
    }

    @Override
    public int getCount(String term) {
        StopWatch stopWatch = new StopWatch();
        IndexReader reader = null;
        try {
            reader = DirectoryReader.open(directory);
            String lowerCase = term.toLowerCase();
            String upperCase = StringHelper.upperCaseFirstLetter(lowerCase);
            return reader.docFreq(new Term(FIELD_NAME, lowerCase)) + reader.docFreq(new Term(FIELD_NAME, upperCase));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            FileHelper.close(reader);
            LOGGER.debug("getCount query took {}", stopWatch);
        }
    }

    @Override
    public int getNumDocs() {
        if (numDocs == null) {
            numDocs = fetchNumDocs();
        }
        return numDocs;
    }

    /**
     * Fetch the # of docs from the index. Only performed once, than cached.
     * 
     * @return The # of docs in the index.
     */
    private int fetchNumDocs() {
        IndexReader indexReader = null;
        try {
            indexReader = DirectoryReader.open(directory);
            return indexReader.numDocs();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            FileHelper.close(indexReader);
        }
    }

    @Override
    public int getNumTerms() {
        throw new UnsupportedOperationException("Not supported by " + LuceneTermCorpus.class.getName());
    }

    @Override
    public int getNumUniqueTerms() {
        throw new UnsupportedOperationException("Not supported by " + LuceneTermCorpus.class.getName());
    }

    public static void main(String[] args) throws IOException {
        Directory directory = new SimpleFSDirectory(new File("/Volumes/LaCie500/ClueWeb09"));
        LuceneTermCorpus frequencies = new LuceneTermCorpus(directory);
        System.out.println(frequencies.getIdf("philipp", false));
        System.out.println(frequencies.getIdf("Philipp", false));
        System.out.println(frequencies.getNumDocs());
    }

}
