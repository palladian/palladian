package ws.palladian.clustering;

import org.apache.commons.math3.util.FastMath;
import org.junit.Test;
import ws.palladian.helper.functional.Distance;
import ws.palladian.helper.nlp.JaroWinklerSimilarity;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class DBSCANTest {
    private final class EuclideanDistance implements Distance<Point> {
        @Override
        public double getDistance(Point i1, Point i2) {
            return Math.sqrt(FastMath.pow(i1.x - i2.x, 2) + FastMath.pow(i1.y - i2.y, 2));
        }
    }

    @Test
    public void testDBSCAN() {
        DBSCAN<Point> dbscan = new DBSCAN<>(1.5, 3, new EuclideanDistance());
        Set<Point> points = new HashSet<>();
        points.add(new Point(1, 1));
        points.add(new Point(2, 1));
        points.add(new Point(2, 2));
        points.add(new Point(4, 3));
        points.add(new Point(5, 3));
        points.add(new Point(4, 4));
        points.add(new Point(1, 5));
        Set<Set<Point>> clusters = dbscan.cluster(points);
        assertEquals(3, clusters.size());
        // CollectionHelper.print(clusters);
    }

    @Test
    public void testDBSCANStrings() {
        Set<String> strings = new HashSet<>();
        strings.add("apple");
        strings.add("aple");
        strings.add("aapple");
        strings.add("apples");
        strings.add("banana");
        strings.add("pea");
        strings.add("peas");
        strings.add("peanut");
        DBSCAN<String> dbscan = new DBSCAN<>(0.1, 2, new JaroWinklerSimilarity());
        Set<Set<String>> clusters = dbscan.cluster(strings);
        assertEquals(4, clusters.size());
        // CollectionHelper.print(clusters);
    }
}
