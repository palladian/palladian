package ws.palladian.helper.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.Validate;

public final class ReservoirSampler {

    private static final Random RANDOM = new Random();

    private ReservoirSampler() {
        // utility class
    }

    public static <T> Collection<T> sample(Iterable<T> input, int k) {
        return sample(input.iterator(), k);
    }

    public static <T> Collection<T> sample(Iterator<T> input, int k) {
        Validate.notNull(input, "input must not be null");
        Validate.isTrue(k >= 0, "k must be greater/equal zero");
        List<T> sample = new ArrayList<T>(k);
        for (int i = 0; i < k; i++) {
            if (input.hasNext()) {
                sample.add(input.next());
            } else {
                break;
            }
        }

        int i = k + 1;
        while (input.hasNext()) {
            T item = input.next();
            int j = RANDOM.nextInt(i++) + 1;
            if (j < k) {
                sample.set(j, item);
            }
        }
        return sample;
    }

}
