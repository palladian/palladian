package ws.palladian.helper;

import java.util.HashSet;

/**
 * Extends {@link java.util.Random} with methods for getting sets of random numbers. <br>
 * For getting different random numbers.
 * 
 * @author Martin Gregor
 * 
 */
public class Random extends java.util.Random {

    /**
	 * 
	 */
    private static final long serialVersionUID = 6050027732267301883L;

    /**
     * Creates a hashset of different integer random numbers.
     * 
     * @param size Size of Set.
     * @param number Exclusive maximal value of random numbers.
     * @return
     */
    public HashSet<Integer> nextIntSet(int size, int number) {
        java.util.Random random = new java.util.Random();
        HashSet<Integer> val = new HashSet<Integer>();
        while (val.size() < size) {
            val.add(random.nextInt(number));
        }
        return val;
    }
}
