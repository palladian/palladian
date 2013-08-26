package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class CoordinateTaggerTest {

    @Test
    public void testLocationTagger() {
        CoordinateTagger tagger = new CoordinateTagger();

        List<LocationAnnotation> annotations = tagger.getAnnotations("40.446195,-79.948862");
        assertEquals(1, annotations.size());
        assertEquals(40.446195, annotations.get(0).getLocation().getLatitude(), 0);
        assertEquals(-79.948862, annotations.get(0).getLocation().getLongitude(), 0);

        annotations = tagger.getAnnotations("40.446195, -79.948862");
        assertEquals(1, annotations.size());
        assertEquals(40.446195, annotations.get(0).getLocation().getLatitude(), 0);
        assertEquals(-79.948862, annotations.get(0).getLocation().getLongitude(), 0);

        annotations = tagger.getAnnotations("40.446195 -79.948862");
        assertEquals(1, annotations.size());
        assertEquals(40.446195, annotations.get(0).getLocation().getLatitude(), 0);
        assertEquals(-79.948862, annotations.get(0).getLocation().getLongitude(), 0);

        annotations = tagger.getAnnotations("40.446195N 79.948862W");
        assertEquals(1, annotations.size());
        assertEquals(40.446195, annotations.get(0).getLocation().getLatitude(), 0);
        assertEquals(-79.948862, annotations.get(0).getLocation().getLongitude(), 0);

        annotations = tagger.getAnnotations("40°26′47″N 079°58′36″W");
        assertEquals(1, annotations.size());
        assertEquals(40.446195, annotations.get(0).getLocation().getLatitude(), 0.05);
        assertEquals(-79.948862, annotations.get(0).getLocation().getLongitude(), 0.05);

        annotations = tagger.getAnnotations("40d 26′ 47″ N 079d 58′ 36″ W");
        assertEquals(1, annotations.size());
        assertEquals(40.446195, annotations.get(0).getLocation().getLatitude(), 0.05);
        assertEquals(-79.948862, annotations.get(0).getLocation().getLongitude(), 0.05);

        annotations = tagger.getAnnotations("40:26:46.302N 079:56:55.903W");
        assertEquals(1, annotations.size());
        assertEquals(40.446195, annotations.get(0).getLocation().getLatitude(), 0.05);
        assertEquals(-79.948862, annotations.get(0).getLocation().getLongitude(), 0.05);

        annotations = tagger.getAnnotations("40° 26.7717, -79° 56.93172");
        assertEquals(1, annotations.size());
        assertEquals(40.446195, annotations.get(0).getLocation().getLatitude(), 0.05);
        assertEquals(-79.948862, annotations.get(0).getLocation().getLongitude(), 0.05);
    }

}
