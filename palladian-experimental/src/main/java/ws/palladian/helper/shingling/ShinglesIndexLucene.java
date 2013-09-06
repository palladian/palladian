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
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
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

    /** The Lucene directory represents the storage on disk. */
    private Directory directory;

    /** The IndexWriter writes the data to the directory. It is kept open for the complete life time of this instance. */
    private IndexWriter writer;

    /** This analyzer just tokenizes at whitespace. */
    private Analyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_31);

    /** This class collects search results. */
    private class ShinglesIndexCollector extends Collector {

        private int docBase;
        private BitSet hits = new BitSet();

        @Override
        public void setScorer(Scorer scorer) throws IOException {
            // NOP.
        }

        @Override
        public void collect(int doc) throws IOException {
            hits.set(docBase + doc);
        }

        @Override
        public void setNextReader(IndexReader reader, int docBase) throws IOException {
            this.docBase = docBase;
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
            writer = new IndexWriter(directory, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);

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

        try {

            // create the query and do the search
            Query query = new QueryParser(Version.LUCENE_31, "sketch", analyzer).parse(String.valueOf(hash));
            IndexSearcher searcher = new IndexSearcher(directory, true);
            ShinglesIndexCollector collector = new ShinglesIndexCollector();
            searcher.search(query, collector);
            searcher.close();

            // retrieve the document information from the index
            BitSet bitSet = collector.getHits();
            IndexReader reader = IndexReader.open(directory, true);

            for (int i = 0; i < bitSet.size(); i++) {
                if (bitSet.get(i)) {
                    Document document = reader.document(i);
                    int documentId = Integer.valueOf(document.get("docId"));
                    documents.add(documentId);
                }
            }
            reader.close();

        } catch (ParseException e) {
            LOGGER.error("", e);
        } catch (IOException e) {
            LOGGER.error("", e);
        }

        return documents;
    }

    @Override
    public Set<Long> getSketchForDocument(int documentId) {

        Set<Long> result = new HashSet<Long>();

        try {

            Query query = new QueryParser(Version.LUCENE_31, "docId", analyzer).parse(String.valueOf(documentId));
            IndexSearcher searcher = new IndexSearcher(directory, true);
            ShinglesIndexCollector collector = new ShinglesIndexCollector();
            searcher.search(query, collector);
            searcher.close();

            BitSet bitSet = collector.getHits();
            IndexReader reader = IndexReader.open(directory, true);

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
            reader.close();

        } catch (ParseException e) {
            LOGGER.error("", e);
        } catch (IOException e) {
            LOGGER.error("", e);
        }

        return result;
    }

    @Override
    public int getNumberOfDocuments() {

        int result = -1;

        try {
            IndexReader reader = IndexReader.open(directory, true);
            result = reader.numDocs();
            reader.close();
        } catch (CorruptIndexException e) {
            LOGGER.error("", e);
        } catch (IOException e) {
            LOGGER.error("", e);
        }

        return result;
    }

    @Override
    public Set<Integer> getSimilarDocuments(int documentId) {

        Set<Integer> result = new HashSet<Integer>();

        try {

            Query query = new QueryParser(Version.LUCENE_31, "docId", analyzer).parse(String.valueOf(documentId));
            IndexSearcher searcher = new IndexSearcher(directory, true);
            ShinglesIndexCollector collector = new ShinglesIndexCollector();
            searcher.search(query, collector);
            searcher.close();

            BitSet bitSet = collector.getHits();
            IndexReader reader = IndexReader.open(directory, true);

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
            reader.close();

        } catch (ParseException e) {
            LOGGER.error("", e);
        } catch (IOException e) {
            LOGGER.error("", e);
        }

        return result;

    }

    @Override
    public void addDocumentSimilarity(int masterDocumentId, int similarDocumentId) {

        try {

            // first, query for the masterDocument by ID
            Query query = new QueryParser(Version.LUCENE_31, "docId", analyzer).parse(String
                    .valueOf(masterDocumentId));
            IndexSearcher searcher = new IndexSearcher(directory, true);
            ShinglesIndexCollector collector = new ShinglesIndexCollector();
            searcher.search(query, collector);
            searcher.close();

            BitSet hits = collector.getHits();
            IndexReader reader = IndexReader.open(directory, true);
            Document doc = null;
            for (int i = 0; i < hits.size(); i++) {
                if (hits.get(i)) {
                    doc = reader.document(i);
                    break;
                }
            }
            reader.close();

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
        }

    }

    @Override
    public Map<Integer, Set<Integer>> getSimilarDocuments() {

        Map<Integer, Set<Integer>> similarDocuments = new HashMap<Integer, Set<Integer>>();

        try {

            IndexReader reader = IndexReader.open(directory, true);

            // we need to iterate through the whole index
            for (int i = 0; i < reader.maxDoc(); i++) {

                if (reader.isDeleted(i)) {
                    continue;
                }

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
        }

        return similarDocuments;
    }

    @Override
    public Map<Integer, Set<Long>> getDocumentsForSketch(Set<Long> sketch) {

        Map<Integer, Set<Long>> documents = new HashMap<Integer, Set<Long>>();

        try {

            // create a boolean OR query with the hashes of the sketch
            BooleanQuery query = new BooleanQuery();
            for (Long hash : sketch) {
                query.add(new TermQuery(new Term("sketch", String.valueOf(hash))), BooleanClause.Occur.SHOULD);
            }

            IndexSearcher searcher = new IndexSearcher(directory, true);
            ShinglesIndexCollector collector = new ShinglesIndexCollector();
            searcher.search(query, collector);
            searcher.close();

            BitSet bitSet = collector.getHits();
            IndexReader reader = IndexReader.open(directory, true);

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
            reader.close();

        } catch (NumberFormatException e) {
            LOGGER.error("", e);
        } catch (CorruptIndexException e) {
            LOGGER.error("", e);
        } catch (IOException e) {
            LOGGER.error("", e);
        }

        return documents;
    }

}
