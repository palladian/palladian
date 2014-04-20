package ws.palladian.helper.math;

import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;

/**
 * Predefined {@link SetSimilarity} implementations.
 * 
 * @author pk
 */
public final class SetSimilarities {

    private SetSimilarities() {
        // no instance.
    }

    /**
     * <a href="http://en.wikipedia.org/wiki/Sørensen–Dice_coefficient">Sørensen-Dice coefficient</a>:
     * 
     * <pre>
     *               2 | A intersect B |
     * Dice(A, B) = ---------------------
     *                  | A | + | B |
     * </pre>
     */
    public static final SetSimilarity DICE = new SetSimilarity() {
        private static final String NAME = "Dice";

        @Override
        public <T> double calculate(Set<T> s1, Set<T> s2) {
            Validate.notNull(s1, "s1 must not be null");
            Validate.notNull(s2, "s2 must not be null");
            if (s1.isEmpty() || s2.isEmpty()) {
                return 0;
            }
            Set<T> intersection = CollectionHelper.intersect(s1, s2);
            if (intersection.isEmpty()) {
                return 0;
            }
            return (double)(2 * intersection.size()) / (s1.size() + s2.size());
        }

        @Override
        public String toString() {
            return NAME;
        };
    };

    /**
     * <a href="http://en.wikipedia.org/wiki/Jaccard_index">Jaccard coefficient</a>:
     * 
     * <pre>
     *                  | A intersect B |
     * Jaccard(A, B) = -------------------
     *                    | A union B |
     * </pre>
     */
    public static final SetSimilarity JACCARD = new SetSimilarity() {
        private static final String NAME = "Jaccard";

        @Override
        public <T> double calculate(Set<T> s1, Set<T> s2) {
            Validate.notNull(s1, "s1 must not be null");
            Validate.notNull(s2, "s2 must not be null");
            if (s1.isEmpty() || s2.isEmpty()) {
                return 0;
            }
            Set<T> intersection = CollectionHelper.intersect(s1, s2);
            if (intersection.isEmpty()) {
                return 0;
            }
            int unionSize = s1.size() + s2.size() - intersection.size();
            return (double) intersection.size() / unionSize;
        }

        @Override
        public String toString() {
            return NAME;
        };
    };

    /**
     * <a href="http://en.wikipedia.org/wiki/Overlap_coefficient">Overlap coefficient</a>:
     * 
     * <pre>
     *                   | A intersect B |
     * Overlap(A, B) = ---------------------
     *                  min( | A |, | B | )
     * </pre>
     */
    public static final SetSimilarity OVERLAP = new SetSimilarity() {
        private static final String NAME = "Overlap";

        @Override
        public <T> double calculate(Set<T> s1, Set<T> s2) {
            Validate.notNull(s1, "s1 must not be null");
            Validate.notNull(s2, "s2 must not be null");
            if (s1.isEmpty() || s2.isEmpty()) {
                return 0;
            }
            Set<T> intersection = CollectionHelper.intersect(s1, s2);
            return (double)intersection.size() / Math.min(s1.size(), s2.size());
        }

        @Override
        public String toString() {
            return NAME;
        };
    };

}
