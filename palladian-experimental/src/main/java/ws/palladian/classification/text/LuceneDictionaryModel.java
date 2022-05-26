package ws.palladian.classification.text;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import ws.palladian.core.CategoryEntries;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.AbstractIterator2;
import java.util.function.Predicate;

/**
 * <p>
 * A Lucene-based {@link DictionaryModel}. The idea is, to use an in-memory model for building the dictionary (because,
 * permanently updating the Lucene index is terribly slow), and creating the dictionary use
 * {@link #index(DictionaryModel, File)} to transfer the dictionary to the Lucene index.
 * 
 * During prediction, this is approx. 10x slower than an in-memory dictionary.
 * 
 * @author Philipp Katz
 */
@SuppressWarnings("serial")
public final class LuceneDictionaryModel extends AbstractDictionaryModel implements Closeable {

    /**
     * Builder for a {@link LuceneDictionaryModel} which creates an in-memory trie first, which is then written to a
     * Lucene index.
     * 
     * @author Philipp Katz
     */
    public static final class Builder implements DictionaryBuilder {

        private final DictionaryBuilder delegate = new DictionaryTrieModel.Builder();
        private final File directoryPath;

        public Builder(File directoryPath) {
            Validate.notNull(directoryPath, "directoryPath must not be null");
            this.directoryPath = directoryPath;
        }

        @Override
        public DictionaryModel create() {
            return index(delegate.create(), directoryPath);
        }

        @Override
        public DictionaryBuilder setName(String name) {
            delegate.setName(name);
            return this;
        }

        @Override
        public DictionaryBuilder setFeatureSetting(FeatureSetting featureSetting) {
            delegate.setFeatureSetting(featureSetting);
            return this;
        }

        @Override
        public DictionaryBuilder addDocument(Collection<String> terms, String category) {
            delegate.addDocument(terms, category);
            return this;
        }

        @Override
        public DictionaryBuilder addDocument(Collection<String> terms, String category, int weight) {
            delegate.addDocument(terms, category, weight);
            return this;
        }

        @Override
        public DictionaryBuilder setPruningStrategy(Predicate<? super CategoryEntries> strategy) {
            delegate.setPruningStrategy(strategy);
            return this;
        }

        @Override
        public DictionaryBuilder addDictionary(DictionaryModel model) {
            delegate.addDictionary(model);
            return this;
        }

    }

    /** Name for the field containing the term. */
    static final String FIELD_TERM = "term";

    /** Name for the field containing the category. */
    static final String FIELD_TERM_CAT = "termCategory";

    static final String FIELD_DOC_CAT = "docCategory";

    /** Name for the field containing the prior counts. */
    private static final String FIELD_COUNTS = "counts";

    /** Indicating document counts for {@link #FIELD_COUNTS}. */
    private static final String VALUE_DOCUMENT_COUNTS = "documentCounts";

    /** Property name for the number of entries (stored in commit user data). */
    private static final String PROPERTY_NUM_ENTRIES = "numEntries";

    /** Property name for the name of the dictionary (stored in the commit user data). */
    private static final String PROPERTY_NAME = "name";

    /** The Lucene Analyzer. */
    private static final Analyzer ANALYZER = new KeywordAnalyzer();

    /**
     * <p>
     * Create a Lucene index from a given {@link DictionaryModel} instance.
     * 
     * @param dictionary The dictionary model to index.
     * @param directoryPath The path where to store the Lucene index.
     * @return The {@link LuceneDictionaryModel}.
     * @throws IOException In case something goes wrong.
     */
    public static LuceneDictionaryModel index(DictionaryModel dictionary, File directoryPath) {
        Validate.notNull(dictionary, "dictionary must not be null");
        Validate.notNull(directoryPath, "directoryPath must not be null");
        if (directoryPath.exists()) {
            throw new IllegalStateException("Path '" + directoryPath
                    + " already exists. Delete first or pick different path.");
        }
        try (FSDirectory directory = FSDirectory.open(directoryPath.toPath());
                IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(ANALYZER))) {
            ProgressMonitor progressMonitor = new ProgressMonitor();
            progressMonitor.startTask("Writing Lucene dict.", dictionary.getNumUniqTerms());

            StringField docCountsField = new StringField(FIELD_COUNTS, VALUE_DOCUMENT_COUNTS, Store.NO);
            writer.addDocument(new CategoryEntriesDoc(FIELD_DOC_CAT, dictionary.getDocumentCounts(), docCountsField));

            int numEntries = 0;
            for (DictionaryEntry entry : dictionary) {
                CategoryEntries categoryEntries = entry.getCategoryEntries();
                StringField termField = new StringField(FIELD_TERM, entry.getTerm(), Store.YES);
                writer.addDocument(new CategoryEntriesDoc(FIELD_TERM_CAT, categoryEntries, termField));
                progressMonitor.increment();
                numEntries += categoryEntries.size();
            }

            Map<String, String> commitUserData = new HashMap<>();
            if (dictionary.getFeatureSetting() != null) {
                commitUserData.putAll(dictionary.getFeatureSetting().toMap());
            }
            if (dictionary.getName() != null) {
                commitUserData.put(PROPERTY_NAME, dictionary.getName());
            }
            commitUserData.put(PROPERTY_NUM_ENTRIES, String.valueOf(numEntries));

            writer.setLiveCommitData(commitUserData.entrySet());
            writer.commit();
            writer.close();
            return new LuceneDictionaryModel(directory);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private final IndexSearcher searcher;

    private final DirectoryReader reader;

    private final FeatureSetting featureSetting;

    private final String name;

    private final CategoryEntries documentCounts;

    private final CategoryEntries termCounts;

    private final int numUniqueTerms;

    private final int numEntries;

    public LuceneDictionaryModel(File directoryPath) throws IOException {
        this(FSDirectory.open(directoryPath.toPath()));
    }

    public LuceneDictionaryModel(Directory directory) {
        Validate.notNull(directory, "directory must not be null");
        try {
            this.reader = DirectoryReader.open(directory);
            this.searcher = new IndexSearcher(reader);
            Map<String, String> commitUserData = reader.getIndexCommit().getUserData();
            this.featureSetting = getFeatureSetting(commitUserData);
            this.name = commitUserData.get(PROPERTY_NAME);
            this.documentCounts = getCategoryEntries(new TermQuery(new Term(FIELD_COUNTS, VALUE_DOCUMENT_COUNTS)),
                    FIELD_DOC_CAT);
            this.termCounts = fetchTermCounts();
            this.numUniqueTerms = (int)MultiTerms.getTerms(reader, FIELD_TERM).size();
            this.numEntries = Integer.parseInt(commitUserData.get(PROPERTY_NUM_ENTRIES));
        } catch (IOException e) {
            throw new IllegalStateException("Error while accessing the directory", e);
        }
    }

    private CategoryEntries fetchTermCounts() throws IOException {
        Terms terms = MultiTerms.getTerms(reader, FIELD_TERM_CAT);
        return getCategoryEntries(terms);
    }

    private FeatureSetting getFeatureSetting(Map<String, String> commitUserData) {
        if (commitUserData.get(FeatureSetting.PROPERTY_TEXT_FEATURE_TYPE) != null) {
            return new FeatureSetting(commitUserData);
        }
        return null;
    }

    /**
     * Get {@link CategoryEntries} for the given {@link Query}.
     * 
     * @param query The query for retrieving the desired {@link Document}.
     * @return The {@link CategoryEntries} with counts from the term vector.
     * @throws IOException In case something goes wrong.
     */
    private CategoryEntries getCategoryEntries(Query query, String fieldName) throws IOException {
        TopDocs topDocsResult = searcher.search(query, 1);
        if (topDocsResult.totalHits.value > 0) {
            int docId = topDocsResult.scoreDocs[0].doc;
            return getCategoryEntries(reader.getTermVector(docId, fieldName));
        }
        return CategoryEntries.EMPTY;
    }

    /**
     * Get {@link CategoryEntries} for a Lucene term vector.
     * 
     * @param terms The term vector.
     * @return The {@link CategoryEntries} with counts from the term vector.
     * @throws IOException In case something goes wrong.
     */
    private CategoryEntries getCategoryEntries(Terms terms) throws IOException {
        TermsEnum termsEnum = terms.iterator();
        CountingCategoryEntriesBuilder builder = new CountingCategoryEntriesBuilder();
        BytesRef bytesRef;
        while ((bytesRef = termsEnum.next()) != null) {
            long categoryCount = termsEnum.totalTermFreq();
            String categoryName = bytesRef.utf8ToString();
            builder.add(categoryName, (int)categoryCount);
        }
        return builder.create();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public FeatureSetting getFeatureSetting() {
        return featureSetting;
    }

    @Override
    public CategoryEntries getCategoryEntries(String term) {
        try {
            return getCategoryEntries(new TermQuery(new Term(FIELD_TERM, term)), FIELD_TERM_CAT);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int getNumUniqTerms() {
        return numUniqueTerms;
    }

    @Override
    public CategoryEntries getDocumentCounts() {
        return documentCounts;
    }

    @Override
    public CategoryEntries getTermCounts() {
        return termCounts;
    }

    @Override
    public Iterator<DictionaryEntry> iterator() {
        return new AbstractIterator2<DictionaryEntry>() {
            int idx = -1;

            @Override
            protected DictionaryEntry getNext() {
                if (idx >= reader.maxDoc()) {
                    return finished();
                }
                try {
                    for (idx++; idx < reader.maxDoc(); idx++) {
                        String term = reader.document(idx).get(FIELD_TERM);
                        if (term != null) {
                            Terms termVector = reader.getTermVector(idx, FIELD_TERM_CAT);
                            return new ImmutableDictionaryEntry(term, getCategoryEntries(termVector));
                        }
                    }
                    return finished();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    @Override
    public int getNumEntries() {
        return numEntries;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

}
