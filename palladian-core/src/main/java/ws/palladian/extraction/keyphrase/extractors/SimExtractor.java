package ws.palladian.extraction.keyphrase.extractors;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import ws.palladian.extraction.keyphrase.Keyphrase;
import ws.palladian.extraction.keyphrase.KeyphraseExtractor;
import ws.palladian.helper.collection.CountMap;

public class SimExtractor extends KeyphraseExtractor {

    private final int NUM_SIMILAR_DOCS = 10;

    private Directory directory;

    public SimExtractor() {
        directory = new RAMDirectory();
    }

    @Override
    public boolean needsTraining() {
        return true;
    }

    @Override
    public void train(String inputText, Set<String> keyphrases) {
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_35, analyzer);
        IndexWriter indexWriter = null;
        try {
            indexWriter = new IndexWriter(directory, indexWriterConfig);
            Document document = new Document();
            document.add(new Field("text", inputText, Field.Store.NO, Field.Index.ANALYZED));
            document.add(new Field("keyphrases", StringUtils.join(keyphrases, "###"), Field.Store.YES,
                    Field.Index.NOT_ANALYZED));
            indexWriter.addDocument(document);
            indexWriter.commit();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            IOUtils.closeQuietly(indexWriter);
        }
    }

    @Override
    public List<Keyphrase> extract(String inputText) {
        List<Keyphrase> ret = new ArrayList<Keyphrase>();
        IndexReader reader = null;
        IndexSearcher searcher = null;
        try {
            reader = IndexReader.open(directory, true);
            searcher = new IndexSearcher(reader);
            MoreLikeThis moreLikeThis = new MoreLikeThis(reader);
            moreLikeThis.setFieldNames(new String[] {"text"});
            Query query = moreLikeThis.like(new StringReader(inputText), "text");
            TopDocs searchResult = searcher.search(query, NUM_SIMILAR_DOCS);
            CountMap<String> retrievedKeyphrases = CountMap.create();
            for (int i = 0; i < searchResult.scoreDocs.length; i++) {
                ScoreDoc scoreDoc = searchResult.scoreDocs[i];
                Document document = searcher.doc(scoreDoc.doc);
                String keyphrases = document.get("keyphrases");
                if (keyphrases != null && keyphrases.length() > 0) {
                    String[] split = keyphrases.split("###");
                    retrievedKeyphrases.addAll(Arrays.asList(split));
                }
            }
            CountMap<String> topKeyphrases = retrievedKeyphrases.getHighest(getKeyphraseCount());
            for (String keyphraseValue : topKeyphrases.uniqueItems()) {
                ret.add(new Keyphrase(keyphraseValue));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            IOUtils.closeQuietly(searcher);
            IOUtils.closeQuietly(reader);
        }
        return ret;
    }

    @Override
    public String getExtractorName() {
        return "LuceneSimSearcher";
    }

    @Override
    public void reset() {
        directory = new RAMDirectory();
    }

}
