package ws.palladian.extraction.feature;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;

import ws.palladian.extraction.feature.TermCorpus;
import ws.palladian.helper.io.FileHelper;

public class LuceneTermCorpusTest {

    private static Directory getSample() {
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_42, new StandardAnalyzer(Version.LUCENE_42));
        Directory directory = new RAMDirectory();
        IndexWriter writer = null;
        try {
            writer = new IndexWriter(directory, config);
            writer.addDocument(createDoc("the quick brown fox"));
            writer.addDocument(createDoc("the fast red car"));
            writer.addDocument(createDoc("the lazy black cat"));
            writer.addDocument(createDoc("at night, all cats are grey"));
            writer.addDocument(createDoc("the brown fox sees a red and a black cat"));
        } catch (IOException e) {
            throw new IllegalStateException();
        } finally {
            FileHelper.close(writer);
        }
        return directory;
    }

    private static Document createDoc(String text) {
        Document doc = new Document();
        doc.add(new TextField(LuceneTermCorpus.FIELD_NAME, text, Field.Store.YES));
        return doc;
    }

    @Test
    public void testLuceneTermCorpus() {
        Directory directory = getSample();
        TermCorpus corpus = new LuceneTermCorpus(directory);
        assertEquals(5, corpus.getNumDocs());
        assertEquals(5., corpus.getIdf("quick", false), 0);
        assertEquals(2.5, corpus.getIdf("brown", false), 0);
    }

}
