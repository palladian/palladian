package tud.iir.helper.shingling;

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
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

/**
 * A ShinglesIndex implementation using Lucene as persistent store.
 * 
 * TODO This is work in progress. This is dirty, messy and much copy+paste at the moment, need to clean this up.
 * 
 * @author Philipp Katz
 * 
 */
public class ShinglesIndexLucene extends ShinglesIndexBaseImpl {

    /** only for debugging purposes. */
    // TODO remove this afterwards
    private static final boolean PERSISTENT = false;

    private IndexWriter writer;
    private Analyzer analyzer;
    private Directory directory;

    public ShinglesIndexLucene() {

        try {

            if (PERSISTENT) {
                directory = new SimpleFSDirectory(new File("shinglesIndex"));

            } else {
                directory = new RAMDirectory();
            }

            // analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);

            /** this analyzer just tokenizes at whitespaces. */
            analyzer = new WhitespaceAnalyzer();

            writer = new IndexWriter(directory, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
            // reader = IndexReader.open(directory, true);
            // searcher = new IndexSearcher(directory, true);

            // searcher = new IndexSearcher(reader);

        } catch (CorruptIndexException e) {
            LOGGER.error(e);
        } catch (LockObtainFailedException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        }

    }

    @Override
    public void addDocument(int documentId, Set<Long> sketch) {

        LOGGER.trace(">addDocument " + documentId + " " + sketch);

        try {

            String sketchString = StringUtils.join(sketch, " ");

            Document doc = new Document();
            doc.add(new Field("docId", String.valueOf(documentId), Field.Store.YES, Field.Index.NOT_ANALYZED));
            doc.add(new Field("sketch", sketchString, Field.Store.YES, Field.Index.ANALYZED));

            writer.addDocument(doc);
            writer.commit();

        } catch (CorruptIndexException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        }

    }

    @Override
    public Set<Integer> getDocumentsForHash(long hash) {

        LOGGER.trace(">getDocumentsForHash " + hash);

        Set<Integer> result = new HashSet<Integer>();

        try {

            Query query = new QueryParser(Version.LUCENE_CURRENT, "sketch", analyzer).parse(String.valueOf(hash));

            final BitSet bitSet = new BitSet();

            IndexSearcher searcher = new IndexSearcher(directory, true);

            searcher.search(query, new Collector() {
                private int docBase;

                @Override
                public void setScorer(Scorer scorer) throws IOException {
                    // NOP.
                }

                @Override
                public void setNextReader(IndexReader reader, int docBase) throws IOException {
                    this.docBase = docBase;
                }

                @Override
                public void collect(int doc) throws IOException {
                    bitSet.set(docBase + doc);
                }

                @Override
                public boolean acceptsDocsOutOfOrder() {
                    return true;
                }
            });
            searcher.close();

            IndexReader reader = IndexReader.open(directory, true);

            for (int i = 0; i < bitSet.size(); i++) {

                if (bitSet.get(i)) {
                    Document document = reader.document(i);
                    String documentId = document.get("docId");
                    result.add(Integer.valueOf(documentId));
                }

            }
            reader.close();

        } catch (ParseException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        return result;
    }

    @Override
    public Set<Long> getSketchForDocument(int documentId) {

        LOGGER.trace(">getSketchFordocument " + documentId);

        Set<Long> result = new HashSet<Long>();

        try {

            Query query = new QueryParser(Version.LUCENE_CURRENT, "docId", analyzer).parse(String.valueOf(documentId));

            final BitSet bitSet = new BitSet();

            IndexSearcher searcher = new IndexSearcher(directory, true);

            searcher.search(query, new Collector() {
                private int docBase;

                @Override
                public void setScorer(Scorer scorer) throws IOException {
                    // NOP.
                }

                @Override
                public void setNextReader(IndexReader reader, int docBase) throws IOException {
                    this.docBase = docBase;
                }

                @Override
                public void collect(int doc) throws IOException {
                    bitSet.set(docBase + doc);
                }

                @Override
                public boolean acceptsDocsOutOfOrder() {
                    return true;
                }
            });
            searcher.close();

            IndexReader reader = IndexReader.open(directory, true);

            for (int i = 0; i < bitSet.size(); i++) {

                if (bitSet.get(i)) {
                    Document document = reader.document(i);
                    String sketch = document.get("sketch");

                    String[] sketchSplit = sketch.split(" ");
                    for (String split : sketchSplit) {
                        result.add(Long.valueOf(split));
                    }
                }

            }
            reader.close();

        } catch (ParseException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        return result;

    }

    @Override
    public int getNumberOfDocuments() {

        try {
            IndexReader reader = IndexReader.open(directory, true);
            int numDocs = reader.numDocs();
            reader.close();
            return numDocs;
        } catch (CorruptIndexException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        return -1;
    }

    @Override
    public Set<Integer> getSimilarDocuments(int documentId) {

        LOGGER.trace(">getSimilarDocuments " + documentId);

        Set<Integer> result = new HashSet<Integer>();

        try {
            // /////BooleanQuery query = new BooleanQuery();

            Query query = new QueryParser(Version.LUCENE_CURRENT, "docId", analyzer).parse(String.valueOf(documentId));

            final BitSet bitSet = new BitSet();

            IndexSearcher searcher = new IndexSearcher(directory, true);

            searcher.search(query, new Collector() {
                private int docBase;

                @Override
                public void setScorer(Scorer scorer) throws IOException {
                    // NOP.
                }

                @Override
                public void setNextReader(IndexReader reader, int docBase) throws IOException {
                    this.docBase = docBase;
                }

                @Override
                public void collect(int doc) throws IOException {
                    bitSet.set(docBase + doc);
                }

                @Override
                public boolean acceptsDocsOutOfOrder() {
                    return true;
                }
            });
            searcher.close();

            IndexReader reader = IndexReader.open(directory, true);

            for (int i = 0; i < bitSet.size(); i++) {

                if (bitSet.get(i)) {
                    Document document = reader.document(i);
                    String sims = document.get("similarities");
                    String[] simssss = sims.split(" ");
                    for (String s : simssss) {
                        result.add(Integer.valueOf(s));
                    }
                }

            }
            reader.close();

        } catch (ParseException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        return result;

    }

    @Override
    public void addDocumentSimilarity(int masterDocumentId, int similarDocumentId) {

        try {

            Query query = new QueryParser(Version.LUCENE_CURRENT, "docId", analyzer).parse(String
                    .valueOf(masterDocumentId));

            int hitsPerPage = 10;
            IndexSearcher searcher = new IndexSearcher(directory, true);
            TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
            searcher.search(query, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;

            Document doc = searcher.doc(hits[0].doc);

            writer.deleteDocuments(new Term("docId", String.valueOf(masterDocumentId)));

            String similarities = doc.get("similarities");
            if (similarities == null) {
                similarities = String.valueOf(similarDocumentId);
            } else {
                similarities = similarities.concat(" " + String.valueOf(similarDocumentId));
            }
            doc.removeField("similarities");
            doc.add(new Field("similarities", similarities, Field.Store.YES, Field.Index.ANALYZED));

            searcher.close();
            writer.addDocument(doc);
            writer.commit();

        } catch (ParseException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        }

    }

    @Override
    public Map<Integer, Set<Integer>> getSimilarDocuments() {

        Map<Integer, Set<Integer>> result = new HashMap<Integer, Set<Integer>>();

        try {
            IndexReader reader = IndexReader.open(directory, true);

            for (int i = 0; i < reader.maxDoc(); i++) {

                if (reader.isDeleted(i)) {
                    continue;
                }

                Document document = reader.document(i);
                String s = document.get("similarities");
                if (s == null) {
                    continue;
                }
                Integer docId = Integer.valueOf(document.get("docId"));
                ;
                String[] s2 = s.split(" ");
                Set<Integer> similarities = new HashSet<Integer>();
                for (String string : s2) {
                    similarities.add(Integer.valueOf(string));
                }
                if (!similarities.isEmpty()) {
                    result.put(docId, similarities);
                }

            }
            reader.close();

        } catch (NumberFormatException e) {
            LOGGER.error(e);
        } catch (CorruptIndexException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        return result;
    }

    @Override
    public void saveIndex() {
        try {

            writer.close();

        } catch (CorruptIndexException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    @Override
    public Map<Integer, Set<Long>> getDocumentsForSketch(Set<Long> sketch) {

        Map<Integer, Set<Long>> result = new HashMap<Integer, Set<Long>>();

        try {
            BooleanQuery q = new BooleanQuery();
            for (Long long1 : sketch) {
                q.add(new TermQuery(new Term("sketch", String.valueOf(long1))), BooleanClause.Occur.SHOULD);
            }

            final BitSet bitSet = new BitSet();

            IndexSearcher searcher = new IndexSearcher(directory, true);

            searcher.search(q, new Collector() {
                private int docBase;

                @Override
                public void setScorer(Scorer scorer) throws IOException {
                    // NOP.
                }

                @Override
                public void setNextReader(IndexReader reader, int docBase) throws IOException {
                    this.docBase = docBase;
                }

                @Override
                public void collect(int doc) throws IOException {
                    bitSet.set(docBase + doc);
                }

                @Override
                public boolean acceptsDocsOutOfOrder() {
                    return true;
                }
            });
            searcher.close();

            IndexReader reader = IndexReader.open(directory, true);

            for (int i = 0; i < bitSet.size(); i++) {

                if (bitSet.get(i)) {
                    Document document = reader.document(i);
                    String skkkktch = document.get("sketch");
                    int docId = Integer.valueOf(document.get("docId"));

                    Set<Long> tmp = new HashSet<Long>();

                    String[] sketchSplit = skkkktch.split(" ");
                    for (String split : sketchSplit) {
                        tmp.add(Long.valueOf(split));
                    }

                    result.put(docId, tmp);
                }

            }
            reader.close();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CorruptIndexException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return result;
        // return super.getDocumentsForSketch(sketch);
    }

}
