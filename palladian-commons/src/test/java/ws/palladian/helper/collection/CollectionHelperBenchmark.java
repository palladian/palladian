package ws.palladian.helper.collection;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.helper.StopWatch;

/**
 * Micro benchmark for Set operations.
 * 
 * @author pk
 */
class CollectionHelperBenchmark {

    private static interface Tester {
        <K> Set<K> execute(Set<K> setA, Set<K> setB);
    }
    
    private static final Random RANDOM = new Random();

    /**
     * old intersect method
     * .......... |setA| = 100000, |setB| = 100000: 2m:49s:738ms
     * .......... |setA| = 10000, |setB| = 100000: 17s:859ms
     * .......... |setA| = 1000, |setB| = 100000: 1s:457ms
     * .......... |setA| = 100, |setB| = 100000: 125ms
     * .......... |setA| = 10, |setB| = 100000: 15ms
     * .......... |setA| = 100000, |setB| = 10000: 2m:36s:381ms
     * .......... |setA| = 100000, |setB| = 1000: 2m:38s:888ms
     * .......... |setA| = 100000, |setB| = 100: 2m:31s:712ms
     * .......... |setA| = 100000, |setB| = 10: 2m:43s:716ms
     * Total time: 13m:39s:891ms
     * 
     * new intersect method
     * .......... |setA| = 100000, |setB| = 100000: 52s:6ms
     * .......... |setA| = 10000, |setB| = 100000: 4s:678ms
     * .......... |setA| = 1000, |setB| = 100000: 378ms
     * .......... |setA| = 100, |setB| = 100000: 31ms
     * .......... |setA| = 10, |setB| = 100000: 3ms
     * .......... |setA| = 100000, |setB| = 10000: 4s:436ms
     * .......... |setA| = 100000, |setB| = 1000: 354ms
     * .......... |setA| = 100000, |setB| = 100: 28ms
     * .......... |setA| = 100000, |setB| = 10: 3ms
     * Total time: 1m:1s:917ms
     */
    public static void intersectBenchmark() {
        final int numRuns = 10000;
        List<Pair<Set<Integer>, Set<Integer>>> testSets = CollectionHelper.newArrayList();
        testSets.add(Pair.of(createRandomSet(100000), createRandomSet(100000)));
        testSets.add(Pair.of(createRandomSet(10000), createRandomSet(100000)));
        testSets.add(Pair.of(createRandomSet(1000), createRandomSet(100000)));
        testSets.add(Pair.of(createRandomSet(100), createRandomSet(100000)));
        testSets.add(Pair.of(createRandomSet(10), createRandomSet(100000)));
        testSets.add(Pair.of(createRandomSet(1), createRandomSet(100000)));
        testSets.add(Pair.of(Collections.<Integer> emptySet(), createRandomSet(100000)));
        testSets.add(Pair.of(createRandomSet(100000), createRandomSet(10000)));
        testSets.add(Pair.of(createRandomSet(100000), createRandomSet(1000)));
        testSets.add(Pair.of(createRandomSet(100000), createRandomSet(100)));
        testSets.add(Pair.of(createRandomSet(100000), createRandomSet(10)));
        testSets.add(Pair.of(createRandomSet(100000), createRandomSet(1)));
        testSets.add(Pair.of(createRandomSet(100000), Collections.<Integer> emptySet()));

        System.out.println("old intersect method");
        benchmark(numRuns, testSets, new Tester() {
            @Override
            public <K> Set<K> execute(Set<K> setA, Set<K> setB) {
                HashSet<K> intersection = new HashSet<K>(setA);
                intersection.retainAll(setB);
                return intersection;
            }
        });
        System.out.println("new intersect method");
        benchmark(numRuns, testSets, new Tester() {
            @Override
            public <K> Set<K> execute(Set<K> setA, Set<K> setB) {
                return CollectionHelper.intersect(setA, setB);
            }
        });
        System.out.println("---------------------------------------");
        System.out.println("old union method");
        benchmark(numRuns, testSets, new Tester() {
            @Override
            public <K> Set<K> execute(Set<K> setA, Set<K> setB) {
                HashSet<K> union = new HashSet<K>(setA);
                union.addAll(setB);
                return union;
            }
        });
        System.out.println("new union method");
        benchmark(numRuns, testSets, new Tester() {
            @Override
            public <K> Set<K> execute(Set<K> setA, Set<K> setB) {
                boolean swap = setA.size() > setB.size();
                Set<K> largerSet = swap ? setA : setB;
                Set<K> smallerSet = swap ? setB : setA;
                Set<K> union = new HashSet<K>(largerSet);
                union.addAll(smallerSet);
                return union;
            }
        });
        System.out.println("new union method ver. 2");
        benchmark(numRuns, testSets, new Tester() {
            @Override
            public <K> Set<K> execute(Set<K> setA, Set<K> setB) {
                int maxSize = Math.max(setA.size(), setB.size());
                Set<K> union = new HashSet<K>(maxSize);
                union.addAll(setA);
                union.addAll(setB);
                return union;
            }
        });
        System.out.println("new union method ver. 3");
        benchmark(numRuns, testSets, new Tester() {
            @Override
            public <K> Set<K> execute(Set<K> setA, Set<K> setB) {
                Set<K> union = new HashSet<K>(setA.size() + setB.size()); // this is a "worst case" size estimate
                union.addAll(setA);
                union.addAll(setB);
                return union;
            }
        });
    }

    private static void benchmark(int numRuns, List<Pair<Set<Integer>, Set<Integer>>> testSets, Tester tester) {
        StopWatch totalStopWatch = new StopWatch();
        for (Pair<Set<Integer>, Set<Integer>> testSet : testSets) {
            StopWatch currentStopWatch = new StopWatch();
            Set<Integer> setA = testSet.getLeft();
            Set<Integer> setB = testSet.getRight();
            for (int i = 0; i < numRuns; i++) {
                if (i % 1000 == 0) {
                    System.out.print('.');
                }
                tester.execute(setA, setB);
            }
            System.out.print(' ');
            System.out.println("|setA| = " + setA.size() + ", |setB| = " + setB.size() + ": " + currentStopWatch);
        }
        System.out.println("Total time: " + totalStopWatch);
    }

    private static Set<Integer> createRandomSet(int maxSize) {
        Set<Integer> result = new HashSet<Integer>();
        while (result.size() < maxSize) {
            result.add(RANDOM.nextInt(Integer.MAX_VALUE));
        }
        return result;
    }

    public static void main(String[] args) {
        intersectBenchmark();
    }

}
