package ws.palladian.helper;

public final class HashHelper {

    /**
     * Generates 32 bit hash from byte array of the given length and seed.
     *
     * @param data byte array to hash
     * @param seed initial seed value
     * @return 32 bit hash of the given array
     */
    public static int murmur32(byte[] data, int seed) {
        // 'm' and 'r' are mixing constants generated offline.
        // They're not really 'magic', they just happen to work well.
        final int m = 0x5bd1e995;
        final int r = 24;

        // Initialize the hash to a random value
        int h = seed ^ data.length;
        int length4 = data.length / 4;

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
        switch (data.length % 4) {
            case 3:
                h ^= (data[(data.length & ~3) + 2] & 0xff) << 16;
            case 2:
                h ^= (data[(data.length & ~3) + 1] & 0xff) << 8;
            case 1:
                h ^= data[data.length & ~3] & 0xff;
                h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }

    private HashHelper() {
        // no instances
    }

}
