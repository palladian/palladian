package ws.palladian.kaggle.fisheries.utils.hash;

import java.math.BigInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class HashUtil {

    private static final int[] one_bits = new int[]{0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4};

    private HashUtil() {
    }

    static String toHex(CharSequence bitString, int length) {
        String padding = Stream.generate(() -> "0").limit(length).collect(Collectors.joining());
        String hex = new BigInteger(bitString.toString(), 2).toString(16);
        return padding.substring(hex.length()) + hex;
    }

    /**
     * Calculate the Hamming distance for two hashes in hex format.
     *
     * @param hash1 First hash.
     * @param hash2 Second hash.
     * @return Hamming distance.
     */
    public static int hammingDistance(String hash1, String hash2) {
        int distance = 0;
        for (int i = 0; i < hash1.length(); i++) {
            int n1 = Integer.parseInt(hash1.substring(i, i + 1), 16);
            int n2 = Integer.parseInt(hash2.substring(i, i + 1), 16);
            distance += one_bits[n1 ^ n2];
        }
        return distance;
    }

}
