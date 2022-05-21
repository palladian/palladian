package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class CoordinateTaggerTest {

    @Test
    public void testLocationTagger() {
        CoordinateTagger tagger = CoordinateTagger.INSTANCE;

        List<LocationAnnotation> annotations = tagger.getAnnotations("40.446195,-79.948862");
        assertEquals(1, annotations.size());
        assertEquals(40.446195, annotations.get(0).getLocation().getCoordinate().getLatitude(), 0);
        assertEquals(-79.948862, annotations.get(0).getLocation().getCoordinate().getLongitude(), 0);

        annotations = tagger.getAnnotations("40.446195, -79.948862");
        assertEquals(1, annotations.size());
        assertEquals(40.446195, annotations.get(0).getLocation().getCoordinate().getLatitude(), 0);
        assertEquals(-79.948862, annotations.get(0).getLocation().getCoordinate().getLongitude(), 0);

        annotations = tagger.getAnnotations("40.446195 -79.948862");
        assertEquals(1, annotations.size());
        assertEquals(40.446195, annotations.get(0).getLocation().getCoordinate().getLatitude(), 0);
        assertEquals(-79.948862, annotations.get(0).getLocation().getCoordinate().getLongitude(), 0);

        annotations = tagger.getAnnotations("40.446195N 79.948862W");
        assertEquals(1, annotations.size());
        assertEquals(40.446195, annotations.get(0).getLocation().getCoordinate().getLatitude(), 0);
        assertEquals(-79.948862, annotations.get(0).getLocation().getCoordinate().getLongitude(), 0);

        annotations = tagger.getAnnotations("40°26′47″N 079°58′36″W");
        assertEquals(1, annotations.size());
        assertEquals(40.446195, annotations.get(0).getLocation().getCoordinate().getLatitude(), 0.05);
        assertEquals(-79.948862, annotations.get(0).getLocation().getCoordinate().getLongitude(), 0.05);

        annotations = tagger.getAnnotations("40d 26′ 47″ N 079d 58′ 36″ W");
        assertEquals(1, annotations.size());
        assertEquals(40.446195, annotations.get(0).getLocation().getCoordinate().getLatitude(), 0.05);
        assertEquals(-79.948862, annotations.get(0).getLocation().getCoordinate().getLongitude(), 0.05);

        annotations = tagger.getAnnotations("40:26:46.302N 079:56:55.903W");
        assertEquals(1, annotations.size());
        assertEquals(40.446195, annotations.get(0).getLocation().getCoordinate().getLatitude(), 0.05);
        assertEquals(-79.948862, annotations.get(0).getLocation().getCoordinate().getLongitude(), 0.05);

        annotations = tagger.getAnnotations("40° 26.7717, -79° 56.93172");
        assertEquals(1, annotations.size());
        assertEquals(40.446195, annotations.get(0).getLocation().getCoordinate().getLatitude(), 0.05);
        assertEquals(-79.948862, annotations.get(0).getLocation().getCoordinate().getLongitude(), 0.05);
    }

    @Test
    public void testTagText() {
        CoordinateTagger tagger = CoordinateTagger.INSTANCE;
        String text = "Mast Hill (68°11′S 67°0′W) is a hill 14 metres (46 ft) high at the western end of Stonington Island, Marguerite Bay, on the west side of the Antarctic Peninsula.";
        List<LocationAnnotation> annotations = tagger.getAnnotations(text);
        assertEquals(1, annotations.size());
        assertEquals(-68.183333, annotations.get(0).getLocation().getCoordinate().getLatitude(), 0.05);
        assertEquals(-67, annotations.get(0).getLocation().getCoordinate().getLongitude(), 0.05);
        // CollectionHelper.print(annotations);

        text = "The cost of living index was listed as 121.4, 21.4 points above the U.S. average.";
        annotations = tagger.getAnnotations(text);
        assertEquals(0, annotations.size());
    }
    
    @Test
    public void testGeoURI() {
        // https://en.wikipedia.org/wiki/Geo_URI_scheme
        String geoUri = "geo:37.786971,-122.399677 and geo:37.786971,-122.399677;u=35";
        List<LocationAnnotation> annotations = CoordinateTagger.INSTANCE.getAnnotations(geoUri);

        assertEquals(2, annotations.size());
        assertEquals(4, annotations.get(0).getStartPosition());
        assertEquals(25, annotations.get(0).getEndPosition());
        assertEquals(37.786971, annotations.get(0).getLocation().getCoordinate().getLatitude(), 0.05);
        assertEquals(-122.399677, annotations.get(0).getLocation().getCoordinate().getLongitude(), 0.05);
        
        assertEquals(34, annotations.get(1).getStartPosition());
        assertEquals(55, annotations.get(1).getEndPosition());
        assertEquals(37.786971, annotations.get(1).getLocation().getCoordinate().getLatitude(), 0.05);
        assertEquals(-122.399677, annotations.get(1).getLocation().getCoordinate().getLongitude(), 0.05);

    }

}
