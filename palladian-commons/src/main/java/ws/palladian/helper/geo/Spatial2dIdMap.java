package ws.palladian.helper.geo;

import java.util.*;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

/**
 * <p>
 * Find spatially indexed values within bounding boxes. This is an alternative to R-Trees.
 * </p>
 *
 * @author David Urbansky
 */
public class Spatial2dIdMap {

    private DoubleArrayList latValues = new DoubleArrayList();
    private List<IdCoordinate> latIds = new ObjectArrayList<>();
    private DoubleArrayList lngValues = new DoubleArrayList();
    private List<IdCoordinate> lngIds = new ObjectArrayList<>();

    public Set<IdCoordinate> findInBox(double lat1, double lng1, double lat2, double lng2) {

//        StopWatch stopWatch = new StopWatch();

        Set<IdCoordinate> latitudeMatches;
        int i1 = CollectionHelper.findIndexBefore(lat1, latValues);
        int i2 = CollectionHelper.findIndexBefore(lat2, latValues);

//        System.out.println("latitude matches in " + stopWatch.getElapsedTimeStringAndIncrement());
        List<IdCoordinate> subList = latIds.subList(i1, i2);
//        System.out.println("sublist in " + stopWatch.getElapsedTimeStringAndIncrement());
        latitudeMatches = new HashSet<>(subList);
//        System.out.println("to set in " + stopWatch.getElapsedTimeStringAndIncrement());

        Set<IdCoordinate> longitudeMatches;
        i1 = CollectionHelper.findIndexBefore(lng1, lngValues);
        i2 = CollectionHelper.findIndexBefore(lng2, lngValues);
//        System.out.println("longitude matches in " + stopWatch.getElapsedTimeStringAndIncrement());

        subList = lngIds.subList(i1, i2);
        longitudeMatches = subList.stream().filter(latitudeMatches::contains).collect(Collectors.toSet());
//        System.out.println("longitude matches stream in " + stopWatch.getElapsedTimeStringAndIncrement());

        return longitudeMatches;
    }

    public void put(double lat, double lng, int id) {
        IdCoordinate idCoordinate = new IdCoordinate();
        idCoordinate.setId(id);
        idCoordinate.setCoordinate(
                new ImmutableGeoCoordinate(GeoUtils.normalizeLatitude(lat), GeoUtils.normalizeLongitude(lng)));
        // idCoordinate.setCoordinate(new ImmutableGeoCoordinate(lat,lng));

        latIds.add(idCoordinate);
        lngIds.add(idCoordinate);
    }

    public void sort() {
        Collections.sort(latIds,
                (o1, o2) -> Double.compare(o1.getCoordinate().getLatitude(), o2.getCoordinate().getLatitude()));
        Collections.sort(lngIds,
                (o1, o2) -> Double.compare(o1.getCoordinate().getLongitude(), o2.getCoordinate().getLongitude()));
        latValues = new DoubleArrayList(
                latIds.stream().map(c -> c.getCoordinate().getLatitude()).collect(Collectors.toList()));
        lngValues = new DoubleArrayList(
                lngIds.stream().map(c -> c.getCoordinate().getLongitude()).collect(Collectors.toList()));
    }

    /**
     *
     * @see http://gis.stackexchange.com/questions/2951/algorithm-for-offsetting-a-latitude-longitude-by-some-amount-of-
     *      meters
     */
    public List<IdCoordinate> closestTo(double lat, double lng, int distanceMeters) {

        ImmutableGeoCoordinate sourceCoordinate = new ImmutableGeoCoordinate(lat, lng);

        double[] boundingBox = sourceCoordinate.getBoundingBox(distanceMeters / 1000);
        List<IdCoordinate> inBox = new ArrayList<>(findInBox(boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[2]));

        // now sort them by distance to given coordinate
        Collections.sort(inBox,
                (o1, o2) -> Double.compare(GeoUtils.approximateDistance(o1.getCoordinate(), sourceCoordinate),
                        GeoUtils.approximateDistance(o2.getCoordinate(), sourceCoordinate)));

        return inBox;
    }

    public static void main(String[] args) {
        Spatial2dIdMap sm = new Spatial2dIdMap();

        Random random = new Random(123);
        for (int i = 0; i < 2000000; i++) {
            double lat = 180 * random.nextDouble();
            double lon = 180 * random.nextDouble();
            sm.put(lat, lon, i);
        }

        StopWatch stopWatch = new StopWatch();
        sm.sort();
        System.out.println("sorted in " + stopWatch.getElapsedTimeString());
        // 410 422 413 425

        Set<IdCoordinate> valuesBetween = sm.findInBox(50., 50., 100., 100.);
        System.out.println(
                "############ " + valuesBetween.size() + " in " + stopWatch.getElapsedTimeStringAndIncrement());

        valuesBetween = sm.findInBox(20., 20., 50., 50.);
        System.out.println(
                "############ " + valuesBetween.size() + " in " + stopWatch.getElapsedTimeStringAndIncrement());

        valuesBetween = sm.findInBox(0., 0., 150., 150.);
        System.out.println(
                "############ " + valuesBetween.size() + " in " + stopWatch.getElapsedTimeStringAndIncrement());

        valuesBetween = sm.findInBox(120., 120., 130., 130.);
        System.out.println(
                "############ " + valuesBetween.size() + " in " + stopWatch.getElapsedTimeStringAndIncrement());

        List<IdCoordinate> idCoordinates = sm.closestTo(43.4, 28.2, 100);
        System.out.println("found " + idCoordinates.size() + " coordinates within range in "
                + stopWatch.getElapsedTimeStringAndIncrement());

        // sm.closestTo(50.,60.,70.,80., 1000);

        // valuesBetween = sm.getValuesWithFullScan(20., 20., 50., 50.);
        // System.out.println(valuesBetween.size() + " in " + stopWatch.getElapsedTimeStringAndIncrement());

        System.out.println(ProcessHelper.getFreeMemory());
        System.out.println(ProcessHelper.getHeapUtilization());

    }
}