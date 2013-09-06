package ws.palladian.helper.shingling;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import ws.palladian.helper.io.FileHelper;

/**
 * A ShinglesIndex implementation using Lucene as persistent store.
 * 
 * At the first glance it seems that the Lucene index has a linear runtime behavior with growing index size, in
 * contrast to the other index implementations which develop quadratically (see diploma thesis
 * "NewsSeecr -- Clustering und Ranking von Nachrichten zu Named Entities aus Newsfeeds", Philipp Katz, 2010) for
 * different performance benchmarks.
 * 
 * 
 * @author Philipp Katz
 */
public class ShinglesIndexLucene extends ShinglesIndexBaseImpl {

    /** The Lucene version used throughout this class. */
    private static final Version LUCENE_VERSION = Version.LUCENE_42;

    /** The Lucene directory represents the storage on disk. */
    private Directory directory;

    /** The IndexWriter writes the data to the directory. It is kept open for the complete life time of this instance. */
    private IndexWriter writer;

    /** This analyzer just tokenizes at whitespace. */
    private final Analyzer analyzer = new WhitespaceAnalyzer(LUCENE_VERSION);

    /** This class collects search results. */
    private static final class ShinglesIndexCollector extends Collector {

        private int docBase;
        private final BitSet hits = new BitSet();

        @Override
        public void setScorer(Scorer scorer) throws IOException {
            // NOP.
        }

        @Override
        public void collect(int doc) throws IOException {
            hits.set(docBase + doc);
        }

        @Override
        public void setNextReader(AtomicReaderContext context) throws IOException {
            this.docBase = context.docBase;
        }

        @Override
        public boolean acceptsDocsOutOfOrder() {
            // order doesn't matter, this way we can achieve a speed up.
            return true;
        }

        public BitSet getHits() {
            return hits;
        }

    }

    @Override
    public void openIndex() {

        try {

            directory = new SimpleFSDirectory(new File(INDEX_FILE_BASE_PATH + getIndexName()));

            // the writer is kept open all the time, it is closed via saveIndex()
            // Lucene 3.x : writer = new IndexWriter(directory, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
            writer = new IndexWriter(directory, new IndexWriterConfig(LUCENE_VERSION, analyzer));

        } catch (CorruptIndexException e) {
            LOGGER.error("", e);
        } catch (LockObtainFailedException e) {
            LOGGER.error("", e);
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }

    @Override
    public void saveIndex() {
        try {
            writer.close();
        } catch (CorruptIndexException e) {
            LOGGER.error("", e);
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }

    @Override
    public void deleteIndex() {

        // make sure, the index is closed.
        saveIndex();

        boolean deleted = FileHelper.delete(INDEX_FILE_BASE_PATH + getIndexName(), true);
        LOGGER.debug("deleted index : " + deleted);
    }

    @Override
    public void addDocument(int documentId, Set<Long> sketch) {

        try {

            // save the sketch as space separated string
            String sketchString = StringUtils.join(sketch, " ");

            Document doc = new Document();
            doc.add(new Field("docId", String.valueOf(documentId), Field.Store.YES, Field.Index.NOT_ANALYZED));
            doc.add(new Field("sketch", sketchString, Field.Store.YES, Field.Index.ANALYZED));

            writer.addDocument(doc);
            writer.commit();

        } catch (CorruptIndexException e) {
            LOGGER.error("", e);
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }

    @Override
    public Set<Integer> getDocumentsForHash(long hash) {

        Set<Integer> documents = new HashSet<Integer>();
        IndexReader reader = null;

        try {

            // create the query and do the search
            Query query = new QueryParser(LUCENE_VERSION, "sketch", analyzer).parse(String.valueOf(hash));
            reader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader);
            ShinglesIndexCollector collector = new ShinglesIndexCollector();
            searcher.search(query, collector);

            // retrieve the document information from the index
            BitSet bitSet = collector.getHits();

            for (int i = 0; i < bitSet.size(); i++) {
                if (bitSet.get(i)) {
                    Document document = reader.document(i);
                    int documentId = Integer.valueOf(document.get("docId"));
                    documents.add(documentId);
                }
            }

        } catch (ParseException e) {
            LOGGER.error("", e);
        } catch (IOException e) {
            LOGGER.error("", e);
        } finally {
            FileHelper.close(reader);
        }

        return documents;
    }

    @Override
    public Set<Long> getSketchForDocument(int documentId) {

        Set<Long> result = new HashSet<Long>();
        IndexReader reader = null;

        try {

            Query query = new QueryParser(LUCENE_VERSION, "docId", analyzer).parse(String.valueOf(documentId));
            reader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader);
            ShinglesIndexCollector collector = new ShinglesIndexCollector();
            searcher.search(query, collector);

            BitSet bitSet = collector.getHits();

            for (int i = 0; i < bitSet.size(); i++) {
                if (bitSet.get(i)) {
                    Document document = reader.document(i);
                    String[] sketchArray = document.get("sketch").split(" ");
                    for (String hash : sketchArray) {
                        result.add(Long.valueOf(hash));
                    }
                    break;
                }
            }

        } catch (ParseException e) {
            LOGGER.error("", e);
        } catch (IOException e) {
            LOGGER.error("", e);
        } finally {
            FileHelper.close(reader);
        }

        return result;
    }

    @Override
    public int getNumberOfDocuments() {

        int result = -1;
        IndexReader reader = null;

        try {
            reader = DirectoryReader.open(directory);
            result = reader.numDocs();
        } catch (CorruptIndexException e) {
            LOGGER.error("", e);
        } catch (IOException e) {
            LOGGER.error("", e);
        } finally {
            FileHelper.close(reader);
        }
        return result;
    }

    @Override
    public Set<Integer> getSimilarDocuments(int documentId) {

        Set<Integer> result = new HashSet<Integer>();
        IndexReader reader = null;

        try {

            Query query = new QueryParser(LUCENE_VERSION, "docId", analyzer).parse(String.valueOf(documentId));
            reader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader);
            ShinglesIndexCollector collector = new ShinglesIndexCollector();
            searcher.search(query, collector);

            BitSet bitSet = collector.getHits();

            for (int i = 0; i < bitSet.size(); i++) {
                if (bitSet.get(i)) {
                    Document document = reader.document(i);
                    String[] similarityArray = document.get("similarities").split(" ");
                    for (String similarity : similarityArray) {
                        result.add(Integer.valueOf(similarity));
                    }
                    break;
                }
            }
        } catch (ParseException e) {
            LOGGER.error("", e);
        } catch (IOException e) {
            LOGGER.error("", e);
        } finally {
            FileHelper.close(reader);
        }

        return result;

    }

    @Override
    public void addDocumentSimilarity(int masterDocumentId, int similarDocumentId) {

        IndexReader reader = null;
        try {

            // first, query for the masterDocument by ID
            Query query = new QueryParser(LUCENE_VERSION, "docId", analyzer).parse(String
                    .valueOf(masterDocumentId));
            reader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader);
            ShinglesIndexCollector collector = new ShinglesIndexCollector();
            searcher.search(query, collector);

            BitSet hits = collector.getHits();
            Document doc = null;
            for (int i = 0; i < hits.size(); i++) {
                if (hits.get(i)) {
                    doc = reader.document(i);
                    break;
                }
            }

            if (doc == null) {
                LOGGER.error("document with id " + masterDocumentId + " not found.");
                return;
            }

            // Lucene index does not allow updating documents,
            // so we have to delete the document from the index and re-add it
            writer.deleteDocuments(new Term("docId", String.valueOf(masterDocumentId)));

            String similarities = doc.get("similarities");
            if (similarities == null) {
                similarities = String.valueOf(similarDocumentId);
            } else {
                similarities = similarities.concat(" ").concat(String.valueOf(similarDocumentId));
            }
            // replace the existing "similarities" field with the new content
            doc.removeField("similarities");
            doc.add(new Field("similarities", similarities, Field.Store.YES, Field.Index.ANALYZED));

            writer.addDocument(doc);
            writer.commit();

        } catch (ParseException e) {
            LOGGER.error("", e);
        } catch (IOException e) {
            LOGGER.error("", e);
        } finally {
            FileHelper.close(reader);
        }

    }

    @Override
    public Map<Integer, Set<Integer>> getSimilarDocuments() {

        Map<Integer, Set<Integer>> similarDocuments = new HashMap<Integer, Set<Integer>>();
        IndexReader reader = null;

        try {

            reader = DirectoryReader.open(directory);

            // we need to iterate through the whole index
            for (int i = 0; i < reader.maxDoc(); i++) {

                // if (reader.isDeleted(i)) {
//                Bits liveDocs = MultiFields.getLiveDocs(reader);
//                if (!liveDocs.get(i)) {
//                    continue;
//                }

                Document document = reader.document(i);
                String similarities = document.get("similarities");

                if (similarities == null) {
                    continue;
                }

                Integer masterDocId = Integer.valueOf(document.get("docId"));
                String[] similaritiesArray = similarities.split(" ");

                if (similaritiesArray.length == 0) {
                    continue;
                }

                Set<Integer> similarDocs = new HashSet<Integer>();
                for (String string : similaritiesArray) {
                    similarDocs.add(Integer.valueOf(string));
                }
                similarDocuments.put(masterDocId, similarDocs);

            }
            reader.close();

        } catch (NumberFormatException e) {
            LOGGER.error("", e);
        } catch (CorruptIndexException e) {
            LOGGER.error("", e);
        } catch (IOException e) {
            LOGGER.error("", e);
        } finally {
            FileHelper.close(reader);
        }

        return similarDocuments;
    }

    @Override
    public Map<Integer, Set<Long>> getDocumentsForSketch(Set<Long> sketch) {

        Map<Integer, Set<Long>> documents = new HashMap<Integer, Set<Long>>();
        IndexReader reader = null;

        try {

            // create a boolean OR query with the hashes of the sketch
            BooleanQuery query = new BooleanQuery();
            for (Long hash : sketch) {
                query.add(new TermQuery(new Term("sketch", String.valueOf(hash))), BooleanClause.Occur.SHOULD);
            }

            reader = DirectoryReader.open(directory);
            IndexSearcher searcher = new IndexSearcher(reader);
            ShinglesIndexCollector collector = new ShinglesIndexCollector();
            searcher.search(query, collector);

            BitSet bitSet = collector.getHits();

            for (int i = 0; i < bitSet.size(); i++) {
                if (bitSet.get(i)) {
                    Document doc = reader.document(i);
                    int docId = Integer.valueOf(doc.get("docId"));
                    String[] docSketchArray = doc.get("sketch").split(" ");

                    Set<Long> docSketch = new HashSet<Long>();
                    for (String hash : docSketchArray) {
                        docSketch.add(Long.valueOf(hash));
                    }

                    documents.put(docId, docSketch);
                }
            }

        } catch (NumberFormatException e) {
            LOGGER.error("", e);
        } catch (CorruptIndexException e) {
            LOGGER.error("", e);
        } catch (IOException e) {
            LOGGER.error("", e);
        } finally {
            FileHelper.close(reader);
        }

        return documents;
    }

}
