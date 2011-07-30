package ws.palladian.retrieval.search.local;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import ws.palladian.helper.Cache;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * The query processor's job is to query the index and return a ranked list of matching documents. Its output must be a
 * ranked list of answers to the given query.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class QueryProcessor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(QueryProcessor.class);

    /** The Lucene index searcher instance. */
    private IndexSearcher indexSearcher = null;

    /** The Lucene index reader instance. */
    private IndexReader indexReader = null;

    /** The Lucene directory instance. */
    private Directory directory;

    public QueryProcessor(String indexPath) {

        try {
            directory = new SimpleFSDirectory(new File(indexPath));
        } catch (IOException e) {
            LOGGER.error(e);
        }

    }

    /**
     * Try to open the index reader and searcher.
     * 
     * @return <tt>True</tt>, if everything worked, <tt>false</tt> otherwise.
     */
    private boolean openReader() {

        try {

            indexReader = (IndexReader) Cache.getInstance().getDataObject(directory.getLockID() + "indexReader");
            indexSearcher = (IndexSearcher) Cache.getInstance().getDataObject(directory.getLockID() + "indexSearcher");

            if (indexReader == null || indexSearcher == null) {

                indexReader = IndexReader.open(directory, true);
                indexSearcher = new IndexSearcher(indexReader);

                // the reader and searcher for this index into the cache since opening takes a while
                Cache.getInstance().putDataObject(directory.getLockID() + "indexReader", indexReader);
                Cache.getInstance().putDataObject(directory.getLockID() + "indexSearcher", indexSearcher);

            }

        } catch (CorruptIndexException e) {
            LOGGER.error(e.getMessage());
            return false;
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * <p>
     * Query the ClueWeb09 index and return a list of ranked documents matching the query. A {@link ScoredDocument}
     * contains the exact same information as if the index was queries using "Luke".
     * </p>
     * 
     * @param queryString The string to query.
     * @param resultCount The number of expected documents.
     * @return A ranked list of documents matching the query.
     * @throws CorruptIndexException
     * @throws IOException
     */
    public List<ScoredDocument> queryIndex(String queryString, int resultCount, boolean exact)
            throws CorruptIndexException,
            IOException {

        List<ScoredDocument> rankedDocuments = new ArrayList<ScoredDocument>();

        if (indexReader == null) {
            openReader();
        }

        if (exact) {
            queryString = "\"" + queryString + "\"";
        }

        Query query = null;
        try {
            query = new QueryParser(Version.LUCENE_30, "Wordindex", new SimpleAnalyzer()).parse(queryString);
        } catch (ParseException e) {
            LOGGER.error(e.getMessage());
        }

        TopScoreDocCollector collector = TopScoreDocCollector.create(resultCount, false);
        // indexSearcher.search(new TermQuery(new Term("WARC-TREC-ID", queryString)), collector);
        indexSearcher.search(query, collector);

        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        for (int i = 0; i < hits.length; i++) {

            int docId = hits[i].doc;
            Document d = indexSearcher.doc(docId);

            ScoredDocument scoredDocument = new ScoredDocument(i + 1, hits[i].score, d.get("WARC-TREC-ID"),
                    d.get("Wordindex"));

            rankedDocuments.add(scoredDocument);

            // List<Fieldable> fields = d.getFields();
            // for (Fieldable fieldable : fields) {
            // System.out.println(fieldable.toString());
            // }
        }

        return rankedDocuments;
    }

    public static void main(String[] args) throws CorruptIndexException, IOException {
        QueryProcessor queryProcessor = new QueryProcessor("H:\\PalladianData\\Datasets\\ClueWeb09");
        List<ScoredDocument> results = queryProcessor.queryIndex("click on the pumpkin", 20, false);
        List<ScoredDocument> results2 = queryProcessor.queryIndex("click on the pumpkin", 20, true);
        // List<ScoredDocument> results = queryProcessor.queryIndex("clueweb09-en0005-71-29394", 20);
        CollectionHelper.print(results);
        CollectionHelper.print(results2);
    }

}
