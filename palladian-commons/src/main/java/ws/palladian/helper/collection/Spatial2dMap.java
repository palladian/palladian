package ws.palladian.helper.collection;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.helper.StopWatch;

/**
 * <p>
 * Access objects indexed by a one dimensional numeric index, e.g. get all entries with value > 123.45. Running such
 * range queries on databases is darn slow and can be replaced using this range map.
 * </p>
 *
 * <p>
 * Complexity: O(n)
 * </p>
 *
 * @author David Urbansky
 */
public class Spatial2dMap<K extends Number, V> {

    private List<Pair<Double, V>> lats = new ArrayList<>();
    private List<V> latIds = new ArrayList<>();
    private List<Pair<Double, V>> lons = new ArrayList<>();
    private List<V> lonIds = new ArrayList<>();

    private Map<V, Double[]> idToCoordinate = new HashMap<>();
    /**
     * <p>
     * Get all values within [lowerBound,upperBound].
     * </p>
     *
     * @param lowerBound The minimum number (inclusive).
     * @param upperBound The maximum number (inclusive).
     * @return A list of object within the given range.
     */
    public Set<V> getValuesWithFullScan(double lat1, double lon1, double lat2, double lon2) {
        Set<V> ids = lats.stream().map(Pair::getRight).collect(Collectors.toSet());
        ids.addAll(lons.stream().map(Pair::getRight).collect(Collectors.toSet()));

        Set<V> set = new HashSet<>();

        for (V id : ids) {
            Double[] coords = idToCoordinate.get(id);
            if (coords[0] >= lat1 && coords[0] <= lat2 &&
                    coords[1] >= lon1 && coords[1] <= lon2) {
                set.add(id);
            }
        }

        return set;
    }
    public Set<V> getValuesBetween(double lat1, double lon1, double lat2, double lon2) {

        StopWatch stopWatch = new StopWatch();

        int i1 = 0;
        int i2 = 0;
        Set<V> latitudeMatches;
        for (Pair<Double, V> entry : lats) {
            if (entry.getKey() >= lat1 && i1 == 0) {
                i1 = i2;
            } else if (entry.getKey() > lat2) {
                break;
            }
            i2++;
        }
        System.out.println("latitude matches in " + stopWatch.getElapsedTimeStringAndIncrement());
//        latitudeMatches = lats.subList(i1,i2).stream().map(Pair::getRight).collect(Collectors.toSet());
//        System.out.println("latitude matches stream in " + stopWatch.getElapsedTimeStringAndIncrement());
//        latitudeMatches = new HashSet<>(latIds.subList(i1,i2));
        List<V> subList = latIds.subList(i1, i2);
//        latitudeMatches = CollectionHelper.getSubset(latIds, i1, i2-i1);
        System.out.println("sublist in " + stopWatch.getElapsedTimeStringAndIncrement());
        latitudeMatches = new HashSet<>(subList);
        System.out.println("to set in " + stopWatch.getElapsedTimeStringAndIncrement());

        i1 = 0;
        i2 = 0;
        Set<V> longitudeMatches;
        for (Pair<Double, V> entry : lons) {
            if (entry.getKey() >= lon1 && i1 == 0) {

                i1 = i2;
            } else if (entry.getKey() > lon2) {
                break;
            }
            i2++;
        }
        System.out.println("longitude matches in " + stopWatch.getElapsedTimeStringAndIncrement());

//        subList = lonIds.subList(i1, i2);
//        subList.retainAll(lonIds.subList(i1, i2));
//        System.out.println("sublist retain in " + stopWatch.getElapsedTimeStringAndIncrement());


//        latitudeMatches = CollectionHelper.getSubset(latIds, i1, i2-i1);
//        System.out.println("sublist in " + stopWatch.getElapsedTimeStringAndIncrement());
//        longitudeMatches = new HashSet<>(subList);
//        System.out.println("to set in " + stopWatch.getElapsedTimeStringAndIncrement());
//        longitudeMatches.retainAll(latitudeMatches);
//        System.out.println("retain in " + stopWatch.getElapsedTimeStringAndIncrement());

        longitudeMatches = subList.stream().filter(latitudeMatches::contains).collect(Collectors.toSet());
//        longitudeMatches = lons.subList(i1,i2).stream().filter(p->latitudeMatches.contains(p.getRight())).map(Pair::getRight).collect(Collectors.toSet());
//        longitudeMatches = lons.subList(i1,i2).stream().map(Pair::getRight).collect(Collectors.toSet());
        System.out.println("longitude matches stream in " + stopWatch.getElapsedTimeStringAndIncrement());

        Set<V> intersect = longitudeMatches;
//        Set<V> intersect = CollectionHelper.intersect(latitudeMatches, longitudeMatches);
//        System.out.println("intersection in " + stopWatch.getElapsedTimeStringAndIncrement());

        return intersect;
    }

    public void put(double lat, double lon, V c) {
        lats.add(Pair.of(lat, c));
        lons.add(Pair.of(lon, c));
    }

    public void sort() {
        Collections.sort(lats, (o1, o2) -> Double.compare(o1.getKey(),o2.getKey()));
        Collections.sort(lons, (o1, o2) -> Double.compare(o1.getKey(),o2.getKey()));

        latIds = lats.stream().map(Pair::getRight).collect(Collectors.toList());
        lonIds = lons.stream().map(Pair::getRight).collect(Collectors.toList());
    }

    public static void main(String[] args) {
        Spatial2dMap<Number, Integer> sm = new Spatial2dMap<>();

        Random random = new Random(123);
        for (int i = 0; i < 2000000; i++) {
            double lat = 180 * random.nextDouble();
            double lon = 180 * random.nextDouble();
            sm.put(lat, lon, i);
        }

        StopWatch stopWatch = new StopWatch();
        sm.sort();
        System.out.println("sorted in " + stopWatch.getElapsedTimeString());
        // 300 - 378 - 678 - 501 - 721 505 | 357 440 367 292 269 310 335 366 296

        Set<Integer> valuesBetween = sm.getValuesBetween(50., 50., 100., 100.);
        System.out.println("############ " +valuesBetween.size() + " in " + stopWatch.getElapsedTimeStringAndIncrement());

        valuesBetween = sm.getValuesBetween(20., 20., 50., 50.);
        System.out.println("############ " +valuesBetween.size() + " in " + stopWatch.getElapsedTimeStringAndIncrement());

        valuesBetween = sm.getValuesBetween(0., 0., 150., 150.);
        System.out.println("############ " +valuesBetween.size() + " in " + stopWatch.getElapsedTimeStringAndIncrement());

        valuesBetween = sm.getValuesBetween(120., 120., 130., 130.);
        System.out.println("############ " +valuesBetween.size() + " in " + stopWatch.getElapsedTimeStringAndIncrement());

//        valuesBetween = sm.getValuesWithFullScan(20., 20., 50., 50.);
//        System.out.println(valuesBetween.size() + " in " + stopWatch.getElapsedTimeStringAndIncrement());
    }
}