package ws.palladian.helper.shingling;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * From http://it.toolbox.com/wiki/index.php/Perform_bitwise_permutation_using_Java
 * 
 * ***** currently not in use ***********
 * 
 * @author Philipp Katz
 *
 */
public class BitPermutations {

    private static final int permBlock[] = { 44, 10, 39, 5, 22, 6, 52, 28, 41, 49, 59, 53, 54, 12, 45, 19, 33, 1, 34,
            15, 56, 61, 14, 31, 62, 9, 64, 60, 29, 17, 20, 3, 23, 40, 24, 46, 25, 27, 43, 48, 58, 37, 38, 47, 35, 50,
            51, 16, 57, 36, 2, 21, 4, 55, 30, 8, 26, 32, 18, 13, 42, 7, 11, 63 };

    public static long perm(long b) {
        long sbytes = 0;
        for (int i = 0; i < permBlock.length; i++) {
            int shifts = permBlock[i];
            // using a mask of intialized with one
            long mask = 1;

            mask = (mask << (Long.SIZE - shifts));
            long bit = (mask & b);
            if (bit != 0) { // if the bit is one
                mask = 1;
                mask = (mask << (Long.SIZE - i - 1));
                sbytes = (sbytes | mask);
            }
        }

        return sbytes;
    }

    public static void showBits(long l) {
        StringBuilder result = new StringBuilder(80);
        long mask = 0x8000000000000000l;
        for (int i = 1; i <= Long.SIZE; i++) {
            mask = 0x01l << (Long.SIZE - i);
            byte bit = (byte) ((mask & l) != 0 ? 1 : 0);
            result.append(bit);
            if (i % 4 == 0) {
                result.append(" ");
            }
        }
        result.append("\n");
        System.out.println(result);
    }

    public static void main(String[] args) {

        showBits(12345L);
        System.out.println(Long.toBinaryString(12345L));
        System.out.println(Long.toBinaryString(Long.rotateLeft(12345L, 1)));
        System.exit(0);

        SortedMap<Long, Long> lm = new TreeMap<Long, Long>();

        for (long l = 10000L; l < 10010L; l++) {
            long perm = perm(l);
            lm.put(perm, l);
            // System.out.println(l + " -> " + perm);
        }

        Iterator<Entry<Long, Long>> it = lm.entrySet().iterator();

        while (it.hasNext()) {
            Entry<Long, Long> next = it.next();
            System.out.println(next.getKey() + " " + next.getValue());
        }
        // CollectionHelper.sortBy

    }

}
