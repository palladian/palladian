package ws.palladian.classification.persistence;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import ws.palladian.classification.Categories;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>This class can be used to create, write and read a dictionary index.</p>
 * 
 * @author David Urbansky
 * 
 */
public class DictionaryFileIndex extends DictionaryIndex {

    private static final Logger LOGGER = Logger.getLogger(DictionaryFileIndex.class);
    
    /** The Lucene Version we use. Changing this will likely require a re-indexing of existing indices. */
    protected static final Version LUCENE_VERSION = Version.LUCENE_31;

    private String indexPath = "";
    private IndexWriter indexWriter = null;
    private IndexSearcher indexSearcher = null;
    private NGramAnalyzer analyzer = null;
    // private QueryParser queryParser = null;
    private IndexReader indexReader = null;
    private Directory directory;

    private Categories categories = new Categories();

    public DictionaryFileIndex(String indexPath) {
        this.indexPath = indexPath;
        analyzer = new NGramAnalyzer();
        // queryParser = new QueryParser("word", analyzer);
        try {
            directory = new SimpleFSDirectory(new File(indexPath));
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    @Override
    public void write(String word, CategoryEntries categoryEntries) {

        for (CategoryEntry categoryEntry : categoryEntries) {
            write(word, categoryEntry);
        }

    }

    @Override
    // TODO something wrong here? got
    // "org.apache.lucene.index.MergePolicy$MergeException: org.apache.lucene.index.CorruptIndexException: docs out of order"
    // at some point
    public void update(String word, CategoryEntries categoryEntries) {
        word = word.toLowerCase();
        try {
            if (indexWriter == null) {
                openWriter();
            }
            indexWriter.deleteDocuments(new Term("word", word));
        } catch (CorruptIndexException e) {
            LOGGER.error(word + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(word + ", " + e.getMessage());
        }
        write(word, categoryEntries);
    }

    @Override
    public void update(String word, CategoryEntry categoryEntry) {
        word = word.toLowerCase();
        try {
            if (indexWriter == null) {
                openWriter();
            }
            indexWriter.deleteDocuments(new Term("word", word));
        } catch (CorruptIndexException e) {
            LOGGER.error(word + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(word + ", " + e.getMessage());
        }
        write(word, categoryEntry);
    }

    @Override
    public void write(String word, CategoryEntry categoryEntry) {
        word = word.toLowerCase();

        // make a new, empty document
        Document document = new Document();
        document.add(new Field("word", word, Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field("categoryName", categoryEntry.getCategory().getName(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        // document.add(new Field("categoryPrior", String.valueOf(categoryEntry.getCategory().getPrior()), Field.Store.YES, Field.Index.UN_TOKENIZED));
        document.add(new Field("absoluteRelevance", String.valueOf(categoryEntry.getAbsoluteRelevance()), Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field("relativeRelevance", String.valueOf(categoryEntry.getRelevance()), Field.Store.YES, Field.Index.NOT_ANALYZED));

        try {
            // indexWriter = new IndexWriter(indexPath, new StandardAnalyzer());
            if (indexWriter == null) {
                openWriter();
            }
            indexWriter.addDocument(document);
            // indexWriter.commit();

        } catch (CorruptIndexException e) {
            LOGGER.error(word + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(word + " " + categoryEntry + ", " + e.getMessage());
        }

    }

    @Override
    public CategoryEntries read(String word) {

        word = word.toLowerCase();

        CategoryEntries categoryEntries = new CategoryEntries();

        try {

            if (indexReader == null) {
                openReader();
            }

            BooleanQuery apiQuery = new BooleanQuery();
            apiQuery.add(new TermQuery(new Term("word", word)), null);

            // System.out.println("Searching for: " + query.toString(field));

            // TopDocCollector collector = new TopDocCollector(81);
            TopScoreDocCollector collector = TopScoreDocCollector.create(81, false);
            indexSearcher.search(apiQuery, collector);
            // searcher.search(query, new Filter(), 100);

            ScoreDoc[] hits = collector.topDocs().scoreDocs;

            // if the term applies to more than 80 categories it is not specific
            // enough and is skipped therefore
            if (hits.length > 80)
                return categoryEntries;

            for (int i = 0; i < hits.length; i++) {

                int docId = hits[i].doc;
                Document d = indexSearcher.doc(docId);

                LOGGER.debug("word found: " + d.get("word"));

                String categoryName = d.get("categoryName");
                Category category = categories.getCategoryByName(categoryName);

                if (category == null) {
                    category = new Category(categoryName);
                    // category.setIndexedPrior(Double.valueOf(d.get("categoryPrior")));
                    categories.add(category);
                } else {
                    // category.increaseFrequency();
                }

                if (categoryEntries.getCategoryEntry(d.get("categoryName")) == null) {
                    CategoryEntry ce = new CategoryEntry(categoryEntries, category, Double.valueOf(d.get("absoluteRelevance")));

                    LOGGER.debug("add " + ce);

                    categoryEntries.add(ce);
                }

            }

        } catch (CorruptIndexException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        return categoryEntries;
    }

    @Override
    public void empty() {
        close();
        FileHelper.delete(indexPath, true);
        openWriter();
        close();
        LOGGER.info("deleted the complete index");
    }

    @Override
    public void openWriter() {
        if (indexWriter != null) {
            return;
        }
        try {
            
            // indexWriter = new IndexWriter(directory, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
            
            // changed deprecated constructor; API doc says we need to use a LimitTokenCountAnalyzer
            // for limiting the MaxFieldLength, but I suppose this is not necessary in this case,
            // as the MaxFieldLength was UNLIMITED -- Philipp; 2011-12-08

            IndexWriterConfig conf = new IndexWriterConfig(LUCENE_VERSION, analyzer);
            indexWriter = new IndexWriter(directory, conf);
        } catch (IOException e) {
            LOGGER.error("could not open the index" + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            if (indexWriter != null) {
                try {
                    // indexWriter.optimize();
                    indexWriter.forceMerge(1);
                } catch (OutOfMemoryError e) {
                    LOGGER.error(e.getMessage());
                }
                indexWriter.commit();
                indexWriter.close();
                indexWriter = null;
            }
            if (indexReader != null) {
                indexReader.close();
                indexReader = null;
            }
            LOGGER.debug("indexWriter and indexReader closed");
        } catch (IOException e) {
            LOGGER.error("could not close the index" + e.getMessage());
        }
    }

    @Override
    public boolean openReader() {
        try {
            indexReader = IndexReader.open(directory, true);
            indexSearcher = new IndexSearcher(indexReader);
        } catch (CorruptIndexException e) {
            LOGGER.error(e.getMessage());
            return false;
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return false;
        }

        return true;
    }

    public Categories getCategories() {
        return categories;
    }

    public void setCategories(Categories categories) {
        this.categories = categories;
    }
}

final class NGramAnalyzer extends Analyzer {

    @Override
    public final TokenStream tokenStream(String fieldName, Reader reader) {
        return new LowerCaseTokenizer(DictionaryFileIndex.LUCENE_VERSION, reader);
    }

}