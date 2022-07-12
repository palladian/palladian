package ws.palladian.helper.collection;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import java.util.Collection;

import org.apache.commons.lang3.Validate;
import org.apache.commons.math3.util.FastMath;

import ws.palladian.helper.HashHelper;

import java.util.function.Predicate;

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
 * @author Philipp Katz
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
public class BloomFilter<T> implements Predicate<T>, Serializable {

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
        this(numElements, (int)Math.ceil(numElements * FastMath.log(1 / fpProb) / FastMath.pow(FastMath.log(2), 2)));
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
        this.numHashFunctions = (int)Math.ceil(vectorSize / numElements * FastMath.log(2));
        this.falsePositiveProbability = FastMath.pow(2, -(vectorSize * FastMath.log(2)) / numElements);
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

    @Override
    public boolean test(T item) {
        if (item == null) {
            return false;
        }
        int[] hashes = createHashes(item, vectorSize, numHashFunctions);
        for (int hash : hashes) {
            if (!bitVector.get(hash)) {
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
        int[] hashes = createHashes(item, vectorSize, numHashFunctions);
        for (int hash : hashes) {
            bitVector.set(hash);
        }
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
     * Create hashes for the given item.
     * 
     * @param item The item to convert to a BitSet, not <code>null</code>.
     * @param vectorSize Size of the created bit vector (>= 1).
     * @param numHashFunctions The number of hash functions to apply (>=1).
     * @return An array with hashes.
     */
    private static int[] createHashes(Object item, int vectorSize, int numHashes) {
        byte[] bytes = getBytes(item);
        int[] hashes = new int[numHashes];
        for (int i = 0; i < numHashes; i++) {
            int hash = HashHelper.murmur32(bytes, bytes.length, i);
            // shift the modulus, so that we do not get any negative values
            int modHash = (hash % vectorSize + vectorSize) % vectorSize;
            hashes[i] = modHash;
        }
        return hashes;
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
