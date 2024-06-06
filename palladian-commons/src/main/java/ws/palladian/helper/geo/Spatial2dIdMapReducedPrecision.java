package ws.palladian.helper.geo;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Find spatially indexed values within bounding boxes. This is an alternative to R-Trees.
 *
 * Using floats instead of doubles to save memory.
 *
 * @author David Urbansky
 */
public class Spatial2dIdMapReducedPrecision implements Serializable {
    private static final long serialVersionUID = 1L;

    private FloatArrayList latValues = new FloatArrayList();
    private List<IdCoordinate> latIds = new ObjectArrayList<>();
    private FloatArrayList lngValues = new FloatArrayList();
    private List<IdCoordinate> lngIds = new ObjectArrayList<>();

    public Set<IdCoordinate> findInBox(float lat1, float lng1, float lat2, float lng2) {
        if (latValues.isEmpty() || lngValues.isEmpty()) {
            return ObjectSets.emptySet();
        }
        //        StopWatch stopWatch = new StopWatch();
        Set<IdCoordinate> latitudeMatches;
        int i1 = CollectionHelper.findIndexBefore(lat1, latValues);
        int i2 = CollectionHelper.findIndexBefore(lat2, latValues);

        //        System.out.println("latitude matches in " + stopWatch.getElapsedTimeStringAndIncrement());
        List<IdCoordinate> subList = latIds.subList(i1, i2);
        //        System.out.println("sublist (" + subList.size() + ") in " + stopWatch.getElapsedTimeStringAndIncrement());
        latitudeMatches = new ObjectOpenHashSet<>(subList);
        //        System.out.println("to set (" + latitudeMatches.size() + ") in " + stopWatch.getElapsedTimeStringAndIncrement());

        Set<IdCoordinate> longitudeMatches;
        i1 = CollectionHelper.findIndexBefore(lng1, lngValues);
        i2 = CollectionHelper.findIndexBefore(lng2, lngValues);
        //        System.out.println("longitude matches in " + stopWatch.getElapsedTimeStringAndIncrement());

        subList = lngIds.subList(i1, i2);
        longitudeMatches = subList.parallelStream().filter(latitudeMatches::contains).collect(Collectors.toSet());
        //        System.out.println("longitude matches stream in " + stopWatch.getElapsedTimeStringAndIncrement());

        //        Set<IdCoordinate> latAndLongMatches = new HashSet<>();
        //        for (IdCoordinate longitudeMatch : subList) {
        //            if (latitudeMatches.contains(longitudeMatch)) {
        //                latAndLongMatches.add(longitudeMatch);
        //            }
        //        }
        //        return latAndLongMatches;

        return longitudeMatches;
    }

    public void put(double lat, double lng, int id) {
        IdCoordinate idCoordinate = new IdCoordinate();
        idCoordinate.setId(id);
        idCoordinate.setCoordinate(new ImmutableGeoCoordinateReducedPrecision((float) GeoUtils.normalizeLatitude(lat), (float) GeoUtils.normalizeLongitude(lng)));

        latIds.add(idCoordinate);
        lngIds.add(idCoordinate);
    }

    public void sort() {
        latIds = latIds.parallelStream().sorted(Comparator.comparingDouble(o -> o.getCoordinate().getLatitude())).collect(Collectors.toList());
        lngIds = lngIds.parallelStream().sorted(Comparator.comparingDouble(o -> o.getCoordinate().getLongitude())).collect(Collectors.toList());
        latValues = new FloatArrayList(latIds.stream().map(c -> (float) c.getCoordinate().getLatitude()).collect(Collectors.toList()));
        lngValues = new FloatArrayList(lngIds.stream().map(c -> (float) c.getCoordinate().getLongitude()).collect(Collectors.toList()));
    }

    /**
     * see http://gis.stackexchange.com/questions/2951/algorithm-for-offsetting-a-latitude-longitude-by-some-amount-of-
     * meters
     */
    public List<IdCoordinate> closestTo(double lat, double lng, int distanceMeters) {
        GeoCoordinate sourceCoordinate = GeoCoordinate.from(lat, lng);

        double[] boundingBox = sourceCoordinate.getBoundingBox(distanceMeters / 1000.);
        List<IdCoordinate> inBox = new ArrayList<>(findInBox((float) boundingBox[0], (float) boundingBox[1], (float) boundingBox[2], (float) boundingBox[3]));

        // now sort them by distance to given coordinate
        inBox = inBox.parallelStream().sorted(
                Comparator.comparingDouble(o -> GeoUtils.approximateDistance(lat, lng, sourceCoordinate.getLatitude(), sourceCoordinate.getLongitude()))).collect(
                Collectors.toList());

        return inBox;
    }

    public static void main(String[] args) {
        Spatial2dIdMapReducedPrecision sm = new Spatial2dIdMapReducedPrecision();

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

        Set<IdCoordinate> valuesBetween = sm.findInBox(50f, 50f, 100f, 100f);
        System.out.println("############ " + valuesBetween.size() + " in " + stopWatch.getElapsedTimeStringAndIncrement());

        valuesBetween = sm.findInBox(20f, 20f, 50f, 50f);
        System.out.println("############ " + valuesBetween.size() + " in " + stopWatch.getElapsedTimeStringAndIncrement());

        valuesBetween = sm.findInBox(0f, 0f, 150f, 150f);
        System.out.println("############ " + valuesBetween.size() + " in " + stopWatch.getElapsedTimeStringAndIncrement());

        valuesBetween = sm.findInBox(120f, 120f, 130f, 130f);
        System.out.println("############ " + valuesBetween.size() + " in " + stopWatch.getElapsedTimeStringAndIncrement());

        List<IdCoordinate> idCoordinates = sm.closestTo(43.4, 28.2, 100);
        System.out.println("found " + idCoordinates.size() + " coordinates within range in " + stopWatch.getElapsedTimeStringAndIncrement());

        // sm.closestTo(50.,60.,70.,80., 1000);

        // valuesBetween = sm.getValuesWithFullScan(20., 20., 50., 50.);
        // System.out.println(valuesBetween.size() + " in " + stopWatch.getElapsedTimeStringAndIncrement());

        System.out.println(ProcessHelper.getFreeMemory());
        System.out.println(ProcessHelper.getHeapUtilization());
    }
}