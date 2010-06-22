package tud.iir.persistence;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;

import tud.iir.extraction.ExtractionProcessManager;

/**
 * Write and read from the Lucene index.
 * 
 * @author David Urbansky
 */
public class IndexManager {

    private static IndexManager instance = null;
    IndexWriter indexWriter = null;

    private static final String BENCHMARK_SELECTION_PATH = "data/benchmarkSelection/";

    private IndexManager() {
    }

    public static IndexManager getInstance() {
        if (instance == null)
            instance = new IndexManager();
        return instance;
    }

    public String getIndexPath() {
        String set = "";
        if (ExtractionProcessManager.getBenchmarkSet() == ExtractionProcessManager.MICROSOFT_8) {
            set = "microsoft8";
        } else if (ExtractionProcessManager.getBenchmarkSet() == ExtractionProcessManager.YAHOO_8) {
            set = "yahoo8";
        } else if (ExtractionProcessManager.getBenchmarkSet() == ExtractionProcessManager.HAKIA_8) {
            set = "hakia8";
        } else if (ExtractionProcessManager.getBenchmarkSet() == ExtractionProcessManager.GOOGLE_8) {
            set = "google8";
        }
        return BENCHMARK_SELECTION_PATH + ExtractionProcessManager.getBenchmarkType() + "/" + set;
    }

    public void writeIndex(String filename, String url, String resultID) {

        // make a new, empty document
        Document document = new Document();

        document.add(new Field("filename", filename, Field.Store.YES, Field.Index.UN_TOKENIZED));
        document.add(new Field("url", url, Field.Store.YES, Field.Index.UN_TOKENIZED));
        document.add(new Field("resultID", resultID, Field.Store.YES, Field.Index.TOKENIZED));

        try {
            indexWriter = new IndexWriter(getIndexPath(), new StandardAnalyzer());
            indexWriter.addDocument(document);
            indexWriter.optimize();
            indexWriter.close();
        } catch (IOException e) {
            Logger.getRootLogger().error(filename + " " + url, e);
        }

    }

    public void c() throws Exception {
        indexWriter.close();
    }

    public ArrayList<String> getFromIndex(String field, String queryString) {

        ArrayList<String> results = new ArrayList<String>();

        try {
            IndexReader reader = IndexReader.open(getIndexPath());

            Searcher searcher = new IndexSearcher(reader);
            Analyzer analyzer = new StandardAnalyzer();

            QueryParser parser = new QueryParser(field, analyzer);

            Query query = parser.parse(queryString);
            // System.out.println("Searching for: " + query.toString(field));

            Hits hits = searcher.search(query);

            // System.out.println(hits.length() + " total matching documents");

            int end = hits.length();
            for (int i = 0; i < end; i++) {

                Document doc = hits.doc(i);
                String path = doc.get("resultID") + " " + doc.get("filename") + " " + doc.get("url");
                if (path != null) {
                    System.out.println((i + 1) + "." + path);
                    results.add(doc.get("filename"));
                } else {
                    // System.out.println((i + 1) + ". "+ "No path for this document");
                }
            }

            reader.close();

        } catch (CorruptIndexException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (ParseException e) {
            Logger.getRootLogger().error(e.getMessage());
        }

        return results;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        // File f = new File("data/test/australiaFactBook.html");
        // IndexManager.getInstance().writeIndex(f, "wikipedia.org", "australiapopulation1");
        // IndexManager.getInstance().writeIndex("abc", "wikipedia.org", "usa2");
        // IndexManager.getInstance().writeIndex("abcd", "wikipedia2.org", "usa2");
        // IndexManager.getInstance().c();
        ExtractionProcessManager.setBenchmarkSet(ExtractionProcessManager.GOOGLE_8);
        ExtractionProcessManager.setBenchmarkType(ExtractionProcessManager.BENCHMARK_ENTITY_EXTRACTION);
        IndexManager.getInstance().getFromIndex("resultID", "listofactor");
        // CollectionHelper.print(IndexManager.getInstance().getFromIndex("resultID","listofcar"));
        // IndexManager.getInstance().getFromIndex("filename","website22.html");
        // IndexManager.getInstance().getFromIndex("contents","population");
    }
}