//package ws.palladian.extraction.location.disambiguation;
//
//import java.util.List;
//
//import org.junit.Test;
//
//import ws.palladian.extraction.location.LocationAnnotation;
//import ws.palladian.extraction.location.LocationSource;
//import ws.palladian.extraction.location.PalladianLocationExtractor;
//import ws.palladian.extraction.location.sources.GeonamesLocationSource;
//import ws.palladian.helper.collection.CollectionHelper;
//
//public class HeuristicDisambiguationTest {
//
//    @Test
//    public void testHeuristicDisambiguationDis() {
//        LocationSource locationSource = GeonamesLocationSource.newCachedLocationSource("qqilihq", true);
//        HeuristicDisambiguation disambiguation = new HeuristicDisambiguation();
//        PalladianLocationExtractor extractor = new PalladianLocationExtractor(locationSource, disambiguation);
//        // List<LocationAnnotation> annotations = extractor.getAnnotations("Paris is a city in Logan County");
//        List<LocationAnnotation> annotations = extractor.getAnnotations("London (also Ronton in Gilbertese) is the principal settlement on the atoll of Kiritimati (also known as Christmas Island) belonging to Kiribati in the Pacific Ocean.[");
//        // List<LocationAnnotation> annotations = extractor.getAnnotations("Paris is a city in Arkansas");
//        // List<LocationAnnotation> annotations = extractor.getAnnotations("Paris is a city in the United States");
//        CollectionHelper.print(annotations);
//        
//        // Springfield is a town in Windsor County, Vermont, United States.
//        // Dresden is an agricultural community in southwestern Ontario, Canada, part of the municipality of Chatham-Kent.
//        // Stuttgart is an unincorporated community in Phillips County, Kansas, United States, founded on February 6, 1888.
//        // London (also Ronton in Gilbertese) is the principal settlement on the atoll of Kiritimati (also known as Christmas Island) belonging to Kiribati in the Pacific Ocean.[
//        
//    }
//
//}
