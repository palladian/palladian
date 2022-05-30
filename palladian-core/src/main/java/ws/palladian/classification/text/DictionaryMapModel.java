package ws.palladian.classification.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.lang3.Validate;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import ws.palladian.core.CategoryEntries;

public class DictionaryMapModel extends AbstractDictionaryModel {

	private static final long serialVersionUID = 1L;

	public static final class Builder implements DictionaryBuilder {
		private String name;
		private FeatureSetting featureSetting;
		/** Mapping term -> category -> count */
		private Object2ObjectMap<String, Object2IntMap<String>> dictionary = new Object2ObjectOpenHashMap<>();
		/** Counter for categories based on documents. */
		private final CountingCategoryEntriesBuilder documentCountBuilder = new CountingCategoryEntriesBuilder();
		/** Counter for categories based on terms. */
		private final CountingCategoryEntriesBuilder termCountBuilder = new CountingCategoryEntriesBuilder();

		@Override
		public DictionaryModel create() {
			// convert from mapping `category -> term -> count`
			// to mapping `term -> category -> count`
			return new DictionaryMapModel(this);
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
				dictionary.compute(term, (termValue, categoryCounts) -> {
					if (categoryCounts == null) {
						categoryCounts = new Object2IntOpenHashMap<>(1);
					}
					categoryCounts.compute(category,
							(categoryKey, countValue) -> countValue == null ? weight : countValue + weight);
					return categoryCounts;
				});
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
	private transient Object2ObjectMap<String, Object2IntMap<String>> dictionary;
	private transient CategoryEntries documentCounts;
	private transient CategoryEntries termCounts;

	/** Invoked from the {@link Builder}. */
	private DictionaryMapModel(Builder builder) {
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
		Object2IntMap<String> entries = dictionary.get(term);
		return entries != null ? new CountingCategoryEntriesBuilder(entries).create() : CategoryEntries.EMPTY;
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
		return dictionary.entrySet() //
				.stream() //
				.<DictionaryEntry>map(entry -> new ImmutableDictionaryEntry(entry.getKey(),
						new CountingCategoryEntriesBuilder(entry.getValue()).create())) //
				.iterator();
	}

	// serialization code

	// Implementation note: in case you make any incompatible changes to the
	// serialization protocol, provide backwards
	// compatibility by using the #VERSION constant. Add a test case for the new
	// version and make sure, deserialization
	// of existing models still works (we keep a serialized form of each version
	// from now on for the tests).

	private void writeObject(ObjectOutputStream out) throws IOException {
		writeObject_(out);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		// version
		int version = in.readInt();
		if (version != VERSION) {
			throw new IOException("Unsupported version: " + version);
		}
		Map<Integer, String> categoryIndices = new Int2ObjectOpenHashMap<>();
		dictionary = new Object2ObjectOpenHashMap<>();
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
			String term = (String) in.readObject();
			int numProbabilityEntries = in.readInt();
			Object2IntMap<String> entries = new Object2IntOpenHashMap<>(numProbabilityEntries);
			dictionary.put(term, entries);
			for (int j = 0; j < numProbabilityEntries; j++) {
				int categoryIdx = in.readInt();
				String categoryName = categoryIndices.get(categoryIdx);
				int categoryCount = in.readInt();
				entries.put(categoryName, categoryCount);
				termCountBuilder.add(categoryName, categoryCount);
			}
		}
		termCounts = termCountBuilder.create();
		// feature setting
		featureSetting = (FeatureSetting) in.readObject();
		// name
		name = (String) in.readObject();
	}

}
