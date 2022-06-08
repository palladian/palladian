package ws.palladian.classification.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.helper.HashHelper;

/**
 * So, why store those full terms at all? If we're on a memory constraint, we
 * can hash them. The model will no longer be “understandable” when we look at
 * the term/category matrix, but predictions will still work.
 *
 * Additionally, we store the category counts for each term in a long[] -- each
 * long packs the hashed category and the actual count. This saves a
 * considerable amount of memory.
 *
 * I use a MurmurHash for hashing.
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

        /**
         * Mapping hashed term -> (category, count)[] -- each long value in the array
         * packs two ints; the hashed category name and the count.
         */
        private final Int2ObjectMap<long[]> dictionary = new Int2ObjectOpenHashMap<>();

        /** Counter for categories based on documents. */
        private final CountingCategoryEntriesBuilder documentCountBuilder = new CountingCategoryEntriesBuilder();

        /** Counter for categories based on terms. */
        private final CountingCategoryEntriesBuilder termCountBuilder = new CountingCategoryEntriesBuilder();

        /** Stores the mapping from hashed category name to category name. */
        private final Int2ObjectMap<String> hashToCategory = new Int2ObjectOpenHashMap<>();

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
            int categoryHash = hash(category);
            hashToCategory.put(categoryHash, category);
            for (String term : terms) {
                int termHash = hash(term);
                long[] categoryEntries = dictionary.compute(termHash, (k, v) -> v != null ? v : new long[0]);
                // note: one long packs two ints: category hash and count
                int index = binarySearchForPackedCategoryHash(categoryEntries, categoryHash);
                if (index >= 0) { // just increment count of existing entry
                    // value is packed, but we can simply add, as this affects the “base” part
                    categoryEntries[index] += weight;
                } else { // add new entry and ensure to keep sorted
                    long[] newCategoryEntries = new long[categoryEntries.length + 1];
                    int insertPosition = -index - 1;
                    System.arraycopy(categoryEntries, 0, newCategoryEntries, 0, insertPosition);
                    newCategoryEntries[insertPosition] = pack(categoryHash, weight);
                    System.arraycopy(categoryEntries, insertPosition, newCategoryEntries, -index,
                            categoryEntries.length - insertPosition);
                    dictionary.put(termHash, newCategoryEntries);
                }
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
    private transient Int2ObjectMap<long[]> dictionary;
    private transient CategoryEntries documentCounts;
    private transient CategoryEntries termCounts;
    private transient Int2ObjectMap<String> hashToCategory;

    /** Invoked from the {@link Builder}. */
    private HashedDictionaryMapModel(Builder builder) {
        name = builder.name;
        featureSetting = builder.featureSetting;
        dictionary = builder.dictionary;
        documentCounts = builder.documentCountBuilder.create();
        termCounts = builder.termCountBuilder.create();
        hashToCategory = builder.hashToCategory;
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
        Objects.requireNonNull(term, "term must not be null");
        long[] entries = dictionary.get(hash(term));
        if (entries == null) {
            return CategoryEntries.EMPTY;
        }
        CountingCategoryEntriesBuilder builder = new CountingCategoryEntriesBuilder();
        for (long categoryEntry : entries) {
            int currentCategoryHash = unpackCategoryHash(categoryEntry);
            int categoryCount = unpackCategoryCount(categoryEntry);
            String category = hashToCategory.get(currentCategoryHash);
            builder.set(category, categoryCount);
        }
        return builder.create();
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
        return dictionary.values().stream().mapToInt(entry -> entry.length).sum();
    }

    // serialization code

    // Implementation note: in case you make any incompatible changes to the
    // serialization protocol, provide backwards compatibility by using the #VERSION
    // constant. Add a test case for the new version and make sure, deserialization
    // of existing models still works (we keep a serialized form of each version
    // from now on for the tests).

    private void writeObject(ObjectOutputStream out) throws IOException {
        // version (for being able to provide backwards compatibility from now on)
        out.writeInt(VERSION);
        // header; number of categories; [ (categoryName, count) , ...]
        CategoryEntries categories = getDocumentCounts();
        out.writeInt(categories.size());
        for (Category category : categories) {
            out.writeObject(category.getName());
            out.writeInt(category.getCount());
        }
        out.writeInt(getNumUniqTerms());
        for (Entry<long[]> termEntry : dictionary.int2ObjectEntrySet()) {
            out.writeInt(termEntry.getIntKey()); // hashed term
            out.writeInt(termEntry.getValue().length);
            for (long categoryEntry : termEntry.getValue()) {
                out.writeLong(categoryEntry);
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
        // header
        int numCategories = in.readInt();
        hashToCategory = new Int2ObjectOpenHashMap<>(numCategories);
        CountingCategoryEntriesBuilder documentCountBuilder = new CountingCategoryEntriesBuilder();
        for (int i = 0; i < numCategories; i++) {
            String categoryName = (String) in.readObject();
            int categoryCount = in.readInt();
            documentCountBuilder.set(categoryName, categoryCount);
            hashToCategory.put(hash(categoryName), categoryName);
        }
        documentCounts = documentCountBuilder.create();
        int numTerms = in.readInt(); // num. terms -- not stored
        dictionary = new Int2ObjectOpenHashMap<>(numTerms);
        CountingCategoryEntriesBuilder termCountBuilder = new CountingCategoryEntriesBuilder();
        for (int i = 0; i < numTerms; i++) {
            int termHash = in.readInt();
            int numProbabilityEntries = in.readInt();
            long[] categoryEntries = new long[numProbabilityEntries];
            for (int j = 0; j < numProbabilityEntries; j++) {
                long categoryEntry = in.readLong();
                categoryEntries[j] = categoryEntry;
                int currentCategoryHash = unpackCategoryHash(categoryEntry);
                int categoryCount = unpackCategoryCount(categoryEntry);
                String categoryName = hashToCategory.get(currentCategoryHash);
                termCountBuilder.add(categoryName, categoryCount);
            }
            dictionary.put(termHash, categoryEntries);
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
        if (!Objects.equals(name, other.name) || //
                !Objects.equals(featureSetting, other.featureSetting) || //
                !Objects.equals(documentCounts, other.documentCounts) || //
                !Objects.equals(termCounts, other.termCounts) || //
                !Objects.equals(dictionary.keySet(), other.dictionary.keySet())) {
            return false;
        }
        for (Entry<long[]> dictionaryEntry : dictionary.int2ObjectEntrySet()) {
            long[] otherCategoryEntries = other.dictionary.get(dictionaryEntry.getIntKey());
            if (!Objects.deepEquals(dictionaryEntry.getValue(), otherCategoryEntries)) {
                return false;
            }

        }
        return true;
    }

    private static final int hash(String term) {
        return term != null ? HashHelper.murmur32(term.getBytes(), 1) : 0;
    }

    private static final long pack(int categoryHash, int categoryCount) {
        return (long) categoryHash << 32 | categoryCount & 0xffffffffL;
    }

    private static final int unpackCategoryHash(long value) {
        return (int) (value >> 32);
    }

    private static final int unpackCategoryCount(long value) {
        return (int) value;
    }

    // https://stackoverflow.com/a/13306784
    private static final int binarySearchForPackedCategoryHash(long[] packedArr, int val) {
        int low = 0;
        int high = packedArr.length - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = unpackCategoryHash(packedArr[mid]); // unpacking the long
            if (midVal < val) {
                low = mid + 1;
            } else if (midVal > val) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -(low + 1);
    }

}
