package ws.palladian.helper.collection;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import java.util.Collection;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Bloom filter implementation. A <a href="http://en.wikipedia.org/wiki/Bloom_filter">Bloom filter</a> is a
 * probabilistic data structure which allows membership queries, similar to a Set. The difference, to a full-blown Set
 * is, that when checking for membership of an element, you get on of the two answers: <i>potentially in set</i> or for
 * <i>sure not in set</i> (the false positive rate can tuned by modifying the filter's parameters, which has of course
 * impact on the space requirements). Bloom filters can be used e.g. for blocking unnecessary disk hits, because the
 * <i>for sure not in set</i> candidates can be filtered out in advance, where the space requirements of a bloom filter
 * are considerably smaller than a set.
 * </p>
 * 
 * @author pk
 * 
 * @param <T> Type of the items in this Bloom filter.
 * @see <a href="http://pages.cs.wisc.edu/~cao/papers/summary-cache/node9.html">Bloom Filters as Summaries</a>
 * @see <a href="http://pages.cs.wisc.edu/~cao/papers/summary-cache/node8.html">Bloom Filters - the math</a>
 * @see <a href="http://corte.si/%2Fposts/code/bloom-filter-rules-of-thumb/index.html">3 Rules of thumb for Bloom
 *      Filters</a>
 * @see <a href="https://github.com/tnm/murmurhash-java">murmurhash-java (used hashing function)</a>
 * @see <a href="http://spyced.blogspot.de/2009/01/all-you-ever-wanted-to-know-about.html">All you ever wanted to know
 *      about writing bloom filters</a>
 * @see <a
 *      href="http://highlyscalable.wordpress.com/2012/05/01/probabilistic-structures-web-analytics-data-mining/">Probabilistic
 *      Data Structures for Web Analytics and Data Mining</a>
 * @see <a href="http://matthias.vallentin.net/blog/2011/06/a-garden-variety-of-bloom-filters/">A Garden Variety of
 *      Bloom Filters</a>
 * @see <a href="http://www.michaelnielsen.org/ddi/why-bloom-filters-work-the-way-they-do/">Why Bloom filters work the
 *      way they do </a>
 */
public class BloomFilter<T> implements Filter<T>, Serializable {

    private static final long serialVersionUID = 1L;

    private final int vectorSize;

    private final BitSet bitVector;

    private final double falsePositiveProbability;

    private final int numHashFunctions;

    private int numAddedItems;

    /**
     * <p>
     * Create a new Bloom filter providing a specific false positive rate on a set with the maximum number of elements.
     * </p>
     * 
     * @param fpProb The accepted false positive probability, must be in range [0,1].
     * @param numElements The expected number of elements, greater zero.
     */
    public BloomFilter(double fpProb, int numElements) {
        this(numElements, (int)Math.ceil(numElements * Math.log(1 / fpProb) / Math.pow(Math.log(2), 2)));
    }

    /**
     * <p>
     * Create a new Bloom filter for the given number of elements with the provided size for the bit vector.
     * </p>
     * 
     * @param numElements The expected number of elements, greater zero.
     * @param vectorSize Size of the bit vector, greater zero.
     */
    public BloomFilter(int numElements, int vectorSize) {
        Validate.isTrue(numElements > 0, "numElements must be greater zero");
        Validate.isTrue(vectorSize > 0, "vectorSize must be greater zero");
        this.vectorSize = vectorSize;
        this.bitVector = new BitSet(vectorSize);
        this.numHashFunctions = (int)Math.ceil(vectorSize / numElements * Math.log(2));
        this.falsePositiveProbability = Math.pow(2, -(vectorSize * Math.log(2)) / numElements);
    }

    /**
     * @return The probability for false positives.
     */
    public double getFalsePositiveProbability() {
        return falsePositiveProbability;
    }

    /**
     * @return The number of hash functions.
     */
    public int getNumHashFunctions() {
        return numHashFunctions;
    }

    /**
     * @return The size of the bit vector.
     */
    public int getVectorSize() {
        return vectorSize;
    }

    /**
     * @return The number of items which have been added to this bloom filter (each duplicate is counted).
     */
    public int getNumAddedItems() {
        return numAddedItems;
    }

    /**
     * Generates 32 bit hash from byte array of the given length and seed.
     * 
     * @param data byte array to hash
     * @param length length of the array to hash
     * @param seed initial seed value
     * @return 32 bit hash of the given array
     */
    private static int murmur32(final byte[] data, int length, int seed) {
        // 'm' and 'r' are mixing constants generated offline.
        // They're not really 'magic', they just happen to work well.
        final int m = 0x5bd1e995;
        final int r = 24;

        // Initialize the hash to a random value
        int h = seed ^ length;
        int length4 = length / 4;

        for (int i = 0; i < length4; i++) {
            final int i4 = i * 4;
            int k = (data[i4 + 0] & 0xff) + ((data[i4 + 1] & 0xff) << 8) + ((data[i4 + 2] & 0xff) << 16)
                    + ((data[i4 + 3] & 0xff) << 24);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
        }

        // Handle the last few bytes of the input array
        switch (length % 4) {
            case 3:
                h ^= (data[(length & ~3) + 2] & 0xff) << 16;
            case 2:
                h ^= (data[(length & ~3) + 1] & 0xff) << 8;
            case 1:
                h ^= data[length & ~3] & 0xff;
                h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }

    @Override
    public boolean accept(T item) {
        if (item == null) {
            return false;
        }
        BitSet itemBitVector = createBitVector(item, vectorSize, numHashFunctions);
        return containsAll(bitVector, itemBitVector);
    }

    /**
     * Check, whether the first given {@link BitSet} contains all values from the second given BitSet.
     * 
     * @param s1 The first {@link BitSet}, not <code>null</code>.
     * @param s2 The second {@link BitSet}, not <code>null</code>.
     * @return <code>true</code> in case all enabled bits in the second bit set are also enabled in the first bit set
     *         (ie. s2 \in s1).
     */
    static boolean containsAll(BitSet s1, BitSet s2) {
        for (int i = s2.nextSetBit(0); i >= 0; i = s2.nextSetBit(i + 1)) {
            if (!s1.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Add an item to this {@link BloomFilter}.
     * 
     * @param item The item to add, not <code>null</code>.
     */
    public void add(T item) {
        Validate.notNull(item, "item must not be null");
        BitSet itemBitVector = createBitVector(item, vectorSize, numHashFunctions);
        bitVector.or(itemBitVector);
        numAddedItems++;
    }

    /**
     * Adds a collection of items to this {@link BloomFilter}.
     * 
     * @param items The items to add, not <code>null</code>.
     */
    public void addAll(Collection<? extends T> items) {
        Validate.notNull(items, "items must not be null");
        for (T item : items) {
            add(item);
        }
    }

    /**
     * Convert the given object to a byte array, taken from its {@link Object#toString()} representation.
     * 
     * @param item The item to convert, not <code>null</code>.
     * @return The byte array, representing the Object's string value.
     */
    private static byte[] getBytes(Object item) {
        try {
            return item.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding not supported.");
        }
    }

    /**
     * Create a {@link BitSet} from the given item.
     * 
     * @param item The item to convert to a BitSet, not <code>null</code>.
     * @param vectorSize Size of the created bit vector (>= 1).
     * @param numHashFunctions The number of hash functions to apply (>=1).
     * @return The bit vector for the given object.
     */
    private static BitSet createBitVector(Object item, int vectorSize, int numHashFunctions) {
        BitSet bitVector = new BitSet(vectorSize);
        byte[] bytes = getBytes(item);
        for (int i = 0; i < numHashFunctions; i++) {
            int hash = murmur32(bytes, bytes.length, i);
            // shift the modulus, so that we do not get any negative values
            int modHash = (hash % vectorSize + vectorSize) % vectorSize;
            bitVector.set(modHash);
        }
        return bitVector;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BloomFilter [vectorSize=");
        builder.append(vectorSize);
        builder.append(", fpProbability=");
        builder.append(falsePositiveProbability);
        builder.append(", hashFunctions=");
        builder.append(numHashFunctions);
        builder.append(", addedItems=");
        builder.append(numAddedItems);
        builder.append("]");
        return builder.toString();
    }

}
