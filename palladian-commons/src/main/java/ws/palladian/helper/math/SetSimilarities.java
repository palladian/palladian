package ws.palladian.helper.math;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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

    public static final SetSimilarity DICE = new SetSimilarity() {
        private static final String NAME = "Dice";

        @Override
        public <T> double calculate(Collection<T> c1, Collection<T> c2) {
            Set<T> intersection = CollectionHelper.intersect(new HashSet<T>(c1), new HashSet<T>(c2));
            if (intersection.size() == 0) {
                return 0;
            }
            return (double)(2 * intersection.size()) / (c1.size() + c2.size());
        }

        @Override
        public String toString() {
            return NAME;
        };
    };

    public static final SetSimilarity JACCARD = new SetSimilarity() {
        private static final String NAME = "Jaccard";

        @Override
        public <T> double calculate(Collection<T> c1, Collection<T> c2) {
            Set<T> intersection = CollectionHelper.intersect(new HashSet<T>(c1), new HashSet<T>(c2));
            if (intersection.size() == 0) {
                return 0;
            }
            Set<T> union = new HashSet<T>(c1);
            union.addAll(c2);
            return (double)intersection.size() / union.size();
        }

        @Override
        public String toString() {
            return NAME;
        };
    };

    public static final SetSimilarity OVERLAP = new SetSimilarity() {
        private static final String NAME = "Overlap";

        @Override
        public <T> double calculate(Collection<T> c1, Collection<T> c2) {
            if (c1.size() == 0 || c2.size() == 0) {
                return 0;
            }
            Set<T> intersection = CollectionHelper.intersect(new HashSet<T>(c1), new HashSet<T>(c2));
            return (double)intersection.size() / Math.min(c1.size(), c2.size());
        }

        @Override
        public String toString() {
            return NAME;
        };
    };

}
