package ws.palladian.helper.nlp;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class SetSimilarities {
    
    private SetSimilarities() {
        // no instance.
    }
    
    public static final SetSimilarity DICE = new SetSimilarity() {
        public <T> double calculate(Collection<T> c1, Collection<T> c2) {
            Set<T> nGramsCommon = new HashSet<T>(c1);
            nGramsCommon.retainAll(c2);
            return (double)(2 * nGramsCommon.size()) / (c1.size() + c2.size());
        }
    };

    public static final SetSimilarity JACCARD = new SetSimilarity() {
        @Override
        public <T> double calculate(Collection<T> c1, Collection<T> c2) {
            Set<T> intersection = new HashSet<T>(c1);
            intersection.retainAll(c2);
            if (intersection.size() == 0) {
                return 0;
            }
            Set<T> union = new HashSet<T>(c1);
            union.addAll(c2);
            return (double)intersection.size() / union.size();
        }
    };

    public static final SetSimilarity OVERLAP = new SetSimilarity() {
        @Override
        public <T> double calculate(Collection<T> c1, Collection<T> c2) {
            if (c1.size() == 0 || c2.size() == 0) {
                return 0;
            }
            Set<T> intersection = new HashSet<T>(c1);
            intersection.retainAll(c2);
            return (double)intersection.size() / Math.min(c1.size(), c2.size());
        }
    };

}
