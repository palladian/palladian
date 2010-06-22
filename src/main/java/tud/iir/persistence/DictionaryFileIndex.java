package tud.iir.persistence;

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
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocCollector;

import tud.iir.classification.Categories;
import tud.iir.classification.Category;
import tud.iir.classification.CategoryEntries;
import tud.iir.classification.CategoryEntry;
import tud.iir.helper.FileHelper;

/**
 * This class can be used to create, write and read a dictionary index.
 * 
 * @author David Urbansky
 * 
 */
public class DictionaryFileIndex extends DictionaryIndex {

    private static final Logger logger = Logger.getLogger(DictionaryFileIndex.class);

    private String indexPath = "";
    private IndexWriter indexWriter = null;
    private IndexSearcher indexSearcher = null;
    private NGramAnalyzer analyzer = null;
    // private QueryParser queryParser = null;
    private IndexReader indexReader = null;

    private Categories categories = new Categories();

    public DictionaryFileIndex(String indexPath) {
        this.indexPath = indexPath;
        analyzer = new NGramAnalyzer();
        // queryParser = new QueryParser("word", analyzer);
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
            Logger.getRootLogger().error(word + ", " + e.getMessage());
        } catch (IOException e) {
            Logger.getRootLogger().error(word + ", " + e.getMessage());
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
            Logger.getRootLogger().error(word + ", " + e.getMessage());
        } catch (IOException e) {
            Logger.getRootLogger().error(word + ", " + e.getMessage());
        }
        write(word, categoryEntry);
    }

    @Override
    public void write(String word, CategoryEntry categoryEntry) {
        word = word.toLowerCase();

        // make a new, empty document
        Document document = new Document();
        document.add(new Field("word", word, Field.Store.YES, Field.Index.UN_TOKENIZED));
        document.add(new Field("categoryName", categoryEntry.getCategory().getName(), Field.Store.YES, Field.Index.UN_TOKENIZED));
        // document.add(new Field("categoryPrior", String.valueOf(categoryEntry.getCategory().getPrior()), Field.Store.YES, Field.Index.UN_TOKENIZED));
        document.add(new Field("absoluteRelevance", String.valueOf(categoryEntry.getAbsoluteRelevance()), Field.Store.YES, Field.Index.UN_TOKENIZED));
        document.add(new Field("relativeRelevance", String.valueOf(categoryEntry.getRelevance()), Field.Store.YES, Field.Index.UN_TOKENIZED));

        try {
            // indexWriter = new IndexWriter(indexPath, new StandardAnalyzer());
            if (indexWriter == null) {
                openWriter();
            }
            indexWriter.addDocument(document);
            // indexWriter.commit();

        } catch (CorruptIndexException e) {
            Logger.getRootLogger().error(word + ", " + e.getMessage());
        } catch (IOException e) {
            Logger.getRootLogger().error(word + " " + categoryEntry + ", " + e.getMessage());
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

            TopDocCollector collector = new TopDocCollector(81);
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

                logger.debug("word found: " + d.get("word"));

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

                    logger.debug("add " + ce);

                    categoryEntries.add(ce);
                }

            }

        } catch (CorruptIndexException e) {
            Logger.getRootLogger().error(e.getMessage());
        } catch (IOException e) {
            Logger.getRootLogger().error(e.getMessage());
        }

        return categoryEntries;
    }

    @Override
    public void empty() {
        close();
        FileHelper.delete(indexPath, true);
        openWriter();
        close();
        logger.info("deleted the complete index");
    }

    @Override
    public void openWriter() {
        if (indexWriter != null) {
            return;
        }
        try {
            indexWriter = new IndexWriter(indexPath, analyzer, IndexWriter.MaxFieldLength.UNLIMITED);
        } catch (IOException e) {
            logger.error("could not open the index" + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            if (indexWriter != null) {
                try {
                    indexWriter.optimize();
                } catch (OutOfMemoryError e) {
                    logger.error(e.getMessage());
                }
                indexWriter.commit();
                indexWriter.close();
                indexWriter = null;
            }
            if (indexReader != null) {
                indexReader.close();
                indexReader = null;
            }
            logger.debug("indexWriter and indexReader closed");
        } catch (IOException e) {
            logger.error("could not close the index" + e.getMessage());
        }
    }

    @Override
    public boolean openReader() {
        try {
            indexReader = IndexReader.open(indexPath);
            indexSearcher = new IndexSearcher(indexReader);
        } catch (CorruptIndexException e) {
            logger.error(e.getMessage());
            return false;
        } catch (IOException e) {
            logger.error(e.getMessage());
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

class NGramAnalyzer extends Analyzer {

    @Override
    public TokenStream tokenStream(String arg0, Reader arg1) {
        return new LowerCaseTokenizer(arg1);
    }

}