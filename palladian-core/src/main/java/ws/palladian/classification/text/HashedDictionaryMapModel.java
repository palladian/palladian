package ws.palladian.classification.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.commons.lang3.Validate;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.helper.HashHelper;
import ws.palladian.helper.collection.CollectionHelper;

/**
 * So, why store those full terms at all? If we're on a memory constraint, we
 * can hash them. The model will no longer be “understandable” when we look at
 * the term/category matrix, but predictions will still work.
 *
 * I use a MurmurHash for hashing the terms.
 *
 * Running this on the “20 Newsgroups” data set, the classification accuracy was
 * exactly the same -- thus obviously no hash collisions.
 *
 * @author Philipp Katz
 */
public class HashedDictionaryMapModel extends AbstractDictionaryModel {

    private static final long serialVersionUID = 1L;

    public static final class Builder implements DictionaryBuilder {

        private String name;

        private FeatureSetting featureSetting;

        /** Mapping hashed term -> category -> count */
        private Int2ObjectMap<ArrayCategoryEntries> dictionary = new Int2ObjectOpenHashMap<>();

        /** Counter for categories based on documents. */
        private final CountingCategoryEntriesBuilder documentCountBuilder = new CountingCategoryEntriesBuilder();

        /** Counter for categories based on terms. */
        private final CountingCategoryEntriesBuilder termCountBuilder = new CountingCategoryEntriesBuilder();

        @Override
        public DictionaryModel create() {
            return new HashedDictionaryMapModel(this);
        }

        @Override
        public DictionaryBuilder setName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public DictionaryBuilder setFeatureSetting(FeatureSetting featureSetting) {
            this.featureSetting = featureSetting;
            return this;
        }

        @Override
        public DictionaryBuilder addDocument(Collection<String> terms, String category) {
            return addDocument(terms, category, 1);
        }

        @Override
        public DictionaryBuilder addDocument(Collection<String> terms, String category, int weight) {
            for (String term : terms) {
                ArrayCategoryEntries categoryEntries = dictionary.compute(hash(term),
                        (termValue, entriesValue) -> entriesValue == null ? new ArrayCategoryEntries() : entriesValue);
                categoryEntries.increment(category, weight);
                termCountBuilder.add(category, weight);
            }
            documentCountBuilder.add(category, weight);
            return this;
        }

        @Override
        public DictionaryBuilder setPruningStrategy(Predicate<? super CategoryEntries> strategy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DictionaryBuilder addDictionary(DictionaryModel model) {
            throw new UnsupportedOperationException();
        }

    }

    private transient String name;
    private transient FeatureSetting featureSetting;
    private transient Int2ObjectMap<ArrayCategoryEntries> dictionary;
    private transient CategoryEntries documentCounts;
    private transient CategoryEntries termCounts;

    /** Invoked from the {@link Builder}. */
    private HashedDictionaryMapModel(Builder builder) {
        this.name = builder.name;
        this.featureSetting = builder.featureSetting;
        this.dictionary = builder.dictionary;
        this.documentCounts = builder.documentCountBuilder.create();
        this.termCounts = builder.termCountBuilder.create();
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
        Validate.notNull(term, "term must not be null");
        CategoryEntries entries = dictionary.get(hash(term));
        return entries != null ? entries : CategoryEntries.EMPTY;

    }

    @Override
    public int getNumUniqTerms() {
        return dictionary.size();
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
        throw new UnsupportedOperationException(
                "Cannot iterate because this implementation only stores hashed values.");
    }

    @Override
    public int getNumEntries() {
        return dictionary.values().stream().mapToInt(CategoryEntries::size).sum();
    }

    // serialization code

    // Implementation note: in case you make any incompatible changes to the
    // serialization protocol, provide backwards compatibility by using the #VERSION
    // constant. Add a test case for the new version and make sure, deserialization
    // of existing models still works (we keep a serialized form of each version
    // from now on for the tests).

    private void writeObject(ObjectOutputStream out) throws IOException {
        // map the category names to numeric indices, so that we can use "1" instead of
        // "aVeryLongCategoryName"
        List<Category> sortedCategories = CollectionHelper.newArrayList(getDocumentCounts());
        Collections.sort(sortedCategories, (c1, c2) -> c1.getName().compareTo(c2.getName()));
        Map<String, Integer> categoryIndices = new HashMap<>();
        int idx = 0;
        for (Category category : sortedCategories) {
            categoryIndices.put(category.getName(), idx++);
        }
        // version (for being able to provide backwards compatibility from now on)
        out.writeInt(VERSION);
        // header; number of categories; [ (categoryName, count) , ...]
        out.writeInt(sortedCategories.size());
        for (Category category : sortedCategories) {
            out.writeObject(category.getName());
            out.writeInt(category.getCount());
        }
        // number of terms; list of terms: [ ( term, numProbabilityEntries, [
        // (categoryIdx, count), ... ] ), ... ]
        out.writeInt(getNumUniqTerms());
        for (Entry<? extends CategoryEntries> termEntry : dictionary.int2ObjectEntrySet()) {
            out.writeInt(termEntry.getIntKey()); // hashed term
            CategoryEntries categoryEntries = termEntry.getValue();
            out.writeInt(categoryEntries.size());
            for (Category category : categoryEntries) {
                int categoryIdx = categoryIndices.get(category.getName());
                out.writeInt(categoryIdx);
                out.writeInt(category.getCount());
            }
        }
        // feature setting
        out.writeObject(getFeatureSetting());
        // name
        out.writeObject(getName());

    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // version
        int version = in.readInt();
        if (version != VERSION) {
            throw new IOException("Unsupported version: " + version);
        }
        Map<Integer, String> categoryIndices = new Int2ObjectOpenHashMap<>();
        dictionary = new Int2ObjectOpenHashMap<>();
        // header
        int numCategories = in.readInt();
        CountingCategoryEntriesBuilder documentCountBuilder = new CountingCategoryEntriesBuilder();
        for (int i = 0; i < numCategories; i++) {
            String categoryName = (String) in.readObject();
            int categoryCount = in.readInt();
            documentCountBuilder.set(categoryName, categoryCount);
            categoryIndices.put(i, categoryName);
        }
        documentCounts = documentCountBuilder.create();
        int numTerms = in.readInt(); // num. terms -- not stored
        CountingCategoryEntriesBuilder termCountBuilder = new CountingCategoryEntriesBuilder();
        for (int i = 0; i < numTerms; i++) {
            int termHash = in.readInt();
            int numProbabilityEntries = in.readInt();
            ArrayCategoryEntries entries = new ArrayCategoryEntries();
            dictionary.put(termHash, entries);
            for (int j = 0; j < numProbabilityEntries; j++) {
                int categoryIdx = in.readInt();
                String categoryName = categoryIndices.get(categoryIdx);
                int categoryCount = in.readInt();
                entries.increment(categoryName, categoryCount); // TODO have append method?
                termCountBuilder.add(categoryName, categoryCount);
            }
        }
        termCounts = termCountBuilder.create();
        // feature setting
        featureSetting = (FeatureSetting) in.readObject();
        // name
        name = (String) in.readObject();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        HashedDictionaryMapModel other = (HashedDictionaryMapModel) obj;
        return Objects.equals(name, other.name) && //
                Objects.equals(featureSetting, other.featureSetting) && //
                Objects.equals(dictionary, other.dictionary) && //
                Objects.equals(documentCounts, other.documentCounts) && //
                Objects.equals(termCounts, other.termCounts);
    }

    private static final int hash(String term) {
        byte[] bytes = term.getBytes();
        return term != null ? HashHelper.murmur32(bytes, bytes.length, 1) : 0;
    }

}
