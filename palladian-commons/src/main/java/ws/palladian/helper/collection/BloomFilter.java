package ws.palladian.helper.collection;

import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import java.util.Collection;

import org.apache.commons.lang3.Validate;

// http://stackoverflow.com/questions/658439/how-many-hash-functions-does-my-bloom-filter-need
// http://corte.si/%2Fposts/code/bloom-filter-rules-of-thumb/index.html

// http://pages.cs.wisc.edu/~cao/papers/summary-cache/node8.html
public class BloomFilter<T> implements Filter<T> {

    private final int vectorSize;
    private final BitSet bitVector;
    private final double falsePositiveProbability;
    private final double numHashFunctions;
    private int numAddedItems;

    /**
     * @param fpProb The accepted false positive probability.
     * @param numElements The expected number of elements.
     */
    public BloomFilter(double fpProb, int numElements) {
        this(numElements, (int)Math.ceil(numElements * Math.log(1 / fpProb) / Math.pow(Math.log(2), 2)));
    }

    /**
     * @param numElements The expected number of elements.
     * @param vectorSize Size of the bit vector.
     */
    public BloomFilter(int numElements, int vectorSize) {
        this.vectorSize = vectorSize;
        this.bitVector = new BitSet(vectorSize);
        this.numHashFunctions = vectorSize / numElements * Math.log(2);
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
    public double getNumHashFunctions() {
        return numHashFunctions;
    }

    /**
     * @return The size of the bit vector.
     */
    public int getVectorSize() {
        return vectorSize;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BloomFilter [falsePositiveProbability=");
        builder.append(getFalsePositiveProbability());
        builder.append(", numHashFunctions=");
        builder.append(getNumHashFunctions());
        builder.append(", vectorSize=");
        builder.append(getVectorSize());
        builder.append("]");
        return builder.toString();
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
                h ^= (data[length & ~3] & 0xff);
                h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }

    @Override
    public boolean accept(T item) {
        if (item ==null)return false;
//        byte[] bytes = getBytes(item);
//        for (int i = 0; i < Math.ceil(getNumHashFunctions()); i++) {
//            int hash = murmur32(bytes, bytes.length, i);
//            if (!bitVector.get(trim(hash))) {
//                return false;
//            }
//        }
//        return true;
//        System.out.println("accept > " + item);
        BitSet itemBitVector = createBitVector(item, vectorSize, (int) Math.ceil(numHashFunctions));
        
//        System.out.println(itemBitVector);
//        System.out.println("* " + bitVector);
        
//        itemBitVector.
//        boolean result = itemBitVector.intersects(bitVector);
//        System.out.println("< accept " + result);
//        return result;
        return containsAll(bitVector, itemBitVector);
    }
    
    static boolean containsAll(BitSet s1, BitSet s2) {
        for (int i = s2.nextSetBit(0); i >= 0; i = s2.nextSetBit(i + 1)) {
            if (!s1.get(i)) {
                return false;
            }
        }
        return true;
    }

    public void add(T item) {
        Validate.notNull(item, "item must not be null");
//        byte[] bytes = getBytes(item);
//        for (int i = 0; i < Math.ceil(getNumHashFunctions()); i++) {
//            int hash = murmur32(bytes, bytes.length, i);
//            bitVector.set(trim(hash));
//        }
        BitSet itemBitVector = createBitVector(item, vectorSize, (int) Math.ceil(numHashFunctions));
        bitVector.or(itemBitVector);
        numAddedItems++;
    }

    public void addAll(Collection<? extends T> items) {
        Validate.notNull(items, "items must not be null");
        for (T item : items) {
            add(item);
        }
    }

//    private int trim(int hash) {
//        return ((hash % vectorSize) + vectorSize) % vectorSize;
//    }

    private static byte[] getBytes(Object item) {
        try {
            return item.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding not supported.");
        }
    }
    
    private static BitSet createBitVector(Object item, int vectorSize, int numHashFunctions) {
        BitSet bitVector = new BitSet(vectorSize);
        byte[] bytes = getBytes(item);
        for (int i = 0; i < numHashFunctions; i++) {
            int hash = murmur32(bytes, bytes.length, i);
            int modHash = ((hash % vectorSize) + vectorSize) % vectorSize;
            bitVector.set(modHash);
        }
        return bitVector;
    }

}
