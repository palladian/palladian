package ws.palladian.helper.collection;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.helper.StopWatch;

class CollectionHelperBenchmark {

    private static interface IntersectTester {
        <K> Set<K> intersect(Set<K> setA, Set<K> setB);
    }

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
        final Random random = new Random();
        final int numRuns = 10000;
        List<Pair<Set<Integer>, Set<Integer>>> testSets = CollectionHelper.newArrayList();
        testSets.add(Pair.of(createRandomSet(100000, random), createRandomSet(100000, random)));
        testSets.add(Pair.of(createRandomSet(10000, random), createRandomSet(100000, random)));
        testSets.add(Pair.of(createRandomSet(1000, random), createRandomSet(100000, random)));
        testSets.add(Pair.of(createRandomSet(100, random), createRandomSet(100000, random)));
        testSets.add(Pair.of(createRandomSet(10, random), createRandomSet(100000, random)));
        testSets.add(Pair.of(createRandomSet(100000, random), createRandomSet(10000, random)));
        testSets.add(Pair.of(createRandomSet(100000, random), createRandomSet(1000, random)));
        testSets.add(Pair.of(createRandomSet(100000, random), createRandomSet(100, random)));
        testSets.add(Pair.of(createRandomSet(100000, random), createRandomSet(10, random)));

        System.out.println("old intersect method");
        benchmark(numRuns, testSets, new IntersectTester() {
            @Override
            public <K> Set<K> intersect(Set<K> setA, Set<K> setB) {
                return oldIntersect(setA, setB);
            }
        });
        System.out.println("new intersect method");
        benchmark(numRuns, testSets, new IntersectTester() {
            @Override
            public <K> Set<K> intersect(Set<K> setA, Set<K> setB) {
                return CollectionHelper.intersect(setA, setB);
            }
        });
    }

    private static void benchmark(int numRuns, List<Pair<Set<Integer>, Set<Integer>>> testSets, IntersectTester tester) {
        StopWatch totalStopWatch = new StopWatch();
        for (Pair<Set<Integer>, Set<Integer>> testSet : testSets) {
            StopWatch currentStopWatch = new StopWatch();
            Set<Integer> setA = testSet.getLeft();
            Set<Integer> setB = testSet.getRight();
            for (int i = 0; i < numRuns; i++) {
                if (i % 1000 == 0) {
                    System.out.print('.');
                }
                tester.intersect(setA, setB);
            }
            System.out.print(' ');
            System.out.println("|setA| = " + setA.size() + ", |setB| = " + setB.size() + ": " + currentStopWatch);
        }
        System.out.println("Total time: " + totalStopWatch);
    }

    private static Set<Integer> createRandomSet(int maxSize, Random random) {
        Set<Integer> result = new HashSet<Integer>();
        while (result.size() < maxSize) {
            result.add(random.nextInt(Integer.MAX_VALUE));
        }
        return result;
    }

    public static <T> Set<T> oldIntersect(Set<T> setA, Set<T> setB) {
        HashSet<T> intersection = new HashSet<T>(setA);
        intersection.retainAll(setB);
        return intersection;
    }

    public static void main(String[] args) {
        intersectBenchmark();
    }

}
