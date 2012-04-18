package ws.palladian.extraction.keyphrase.extractors;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.io.FileUtils;
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

import scala.actors.threadpool.Arrays;
import ws.palladian.extraction.keyphrase.Keyphrase;
import ws.palladian.extraction.keyphrase.KeyphraseExtractor;
import ws.palladian.extraction.keyphrase.temp.Dataset2;
import ws.palladian.extraction.keyphrase.temp.DatasetHelper;
import ws.palladian.extraction.keyphrase.temp.DatasetItem;
import ws.palladian.helper.collection.BagHelper;

public class SimExtractor extends KeyphraseExtractor {

    private final int NUM_SIMILAR_DOCS = 10;

    private /*final*/ Directory directory;

    public SimExtractor() {
        directory = new RAMDirectory();
    }

    @Override
    public boolean needsTraining() {
        return true;
    }

    @Override
    public void train(String inputText, Set<String> keyphrases) {
        //Analyzer analyzer = new ShingleAnalyzerWrapper(Version.LUCENE_35, 2, 4);
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
        //Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_35);
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
        // System.out.println("extracting " + inputText);
        List<Keyphrase> ret = new ArrayList<Keyphrase>();
        IndexReader reader = null;
        IndexSearcher searcher = null;
        try {
            reader = IndexReader.open(directory, true);
            searcher = new IndexSearcher(reader);
            MoreLikeThis moreLikeThis = new MoreLikeThis(reader);
            moreLikeThis.setFieldNames(new String[] {"text"});
            Query query = moreLikeThis.like(new StringReader(inputText));
            TopDocs searchResult = searcher.search(query, NUM_SIMILAR_DOCS);
            Bag<String> retrievedKeyphrases = new HashBag<String>();
            for (int i = 0; i < searchResult.scoreDocs.length; i++) {
                ScoreDoc scoreDoc = searchResult.scoreDocs[i];
                Document document = searcher.doc(scoreDoc.doc);
                String keyphrases = document.get("keyphrases");
                if (keyphrases != null && keyphrases.length() > 0) {
                    String[] split = keyphrases.split("###");
                    retrievedKeyphrases.addAll(Arrays.asList(split));
                }
            }
            Bag<String> topKeyphrases = BagHelper.getHighest(retrievedKeyphrases, 10);
            for (String keyphraseValue : topKeyphrases.uniqueSet()) {
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
    
    public static void main(String[] args) throws IOException {
        Dataset2 dataset = DatasetHelper.loadDataset(new File("/Users/pk/Desktop/temp/citeulike180index.txt"), "#");
        Iterator<Dataset2[]> foldIterator = DatasetHelper.crossValidate(dataset, 2);
        Dataset2[] fold = foldIterator.next();
        SimExtractor simExtractor = new SimExtractor();
        simExtractor.train(fold[0]);
        Dataset2 test = fold[1];
        for (DatasetItem datasetItem : test) {
            List<Keyphrase> extract = simExtractor.extract(FileUtils.readFileToString(datasetItem.getFile()));
            System.out.println(extract);
            System.out.println("----");
            System.out.println(Arrays.asList(datasetItem.getCategories()));
            System.out.println();
            break;
        }
    }

}
