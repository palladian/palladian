package ws.palladian.extraction.location.disambiguation;

import org.junit.Test;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationBuilder;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ws.palladian.extraction.location.LocationType.*;

public class HeuristicDisambiguationTest {

    @Test
    public void test_returnsNullForEmptyCollection() {
        Location result = HeuristicDisambiguation.selectLocation(Collections.emptyList());
        assertNull(result);
    }

    @Test
    public void test_chosesContinentIfPresent() {
        Location l1 = new LocationBuilder().setId(9794830).setPrimaryName("Europe").setType(POI).create();
        Location l2 = new LocationBuilder().setId(6255148).setPrimaryName("Europe").setType(CONTINENT).create();
        Location l3 = new LocationBuilder().setId(9962598).setPrimaryName("Europe").setType(POI).create();
        Location result = HeuristicDisambiguation.selectLocation(Arrays.asList(l1, l2, l3));
        assertEquals(l2, result);
    }

    @Test
    public void test_chosesMostPopulatedLocation() {
        Location l1 = new LocationBuilder().setId(2968815).setPrimaryName("Paris").setType(UNIT).setPopulation(2165423L).setAncestorIds("/6295630/6255148/3017382/3012874/").create();
        Location l2 = new LocationBuilder().setId(2988506).setPrimaryName("Paris").setType(UNIT).setPopulation(4380654L).setAncestorIds("/6295630/6255148/3017382/3012874/2968815/").create();
        Location l3 = new LocationBuilder().setId(6455259).setPrimaryName("Paris").setType(UNIT).setPopulation(2190327L).setAncestorIds("/6295630/6255148/3017382/3012874/2968815/2988506/").create();
        Location result = HeuristicDisambiguation.selectLocation(Arrays.asList(l1, l2, l3));
        assertEquals(l2, result);
    }

    @Test
    public void test_chosesHigherPopulatedLocation() {
        Location l1 = new LocationBuilder().setId(6942553).setPrimaryName("Paris").setType(CITY).setPopulation(12310l).create();
        Location l2 = new LocationBuilder().setId(2988507).setPrimaryName("Paris").setType(CITY).setPopulation(2190327l).create();
        Location l3 = new LocationBuilder().setId(3703358).setPrimaryName("Paris").setType(CITY).setPopulation(894l).create();
        Location result = HeuristicDisambiguation.selectLocation(Arrays.asList(l1, l2, l3));
        assertEquals(l2, result);
    }

    @Test
    public void test_chosesCityOverNonCity() {
        Location l1 = new LocationBuilder().setId(2968815).setPrimaryName("Paris").setType(UNIT).setPopulation(2165423L).create();
        Location l2 = new LocationBuilder().setId(2988507).setPrimaryName("Paris").setType(CITY).setPopulation(2138551L).create();
        Location l3 = new LocationBuilder().setId(6455259).setPrimaryName("Paris").setType(UNIT).setPopulation(2190327L).create();
        Location result = HeuristicDisambiguation.selectLocation(Arrays.asList(l1, l2, l3));
        assertEquals(l2, result);
    }

    @Test
    public void test_chosesCityOverNonCity2() {
        Location l1 = new LocationBuilder().setId(2950159).setPrimaryName("Berlin").setType(CITY).setPopulation(3426354L).setAncestorIds("/6295630/6255148/2921044/2950157/6547383/6547539/").create();
        Location l2 = new LocationBuilder().setId(2950157).setPrimaryName("Land Berlin").setType(UNIT).setPopulation(3442675L).setAncestorIds("/6295630/6255148/2921044/").create();
        Location l3 = new LocationBuilder().setId(6547539).setPrimaryName("Berlin").setType(UNIT).setPopulation(3669491L).setAncestorIds("/6295630/6255148/2921044/2950157/6547383/").create();
        Location result = HeuristicDisambiguation.selectLocation(Arrays.asList(l1, l2, l3));
        assertEquals(l1, result);
    }

    @Test
    public void test_chosesCountryOverLandmark() {
        Location l1 = new LocationBuilder().setId(3562993).setPrimaryName("Cuba").setType(LANDMARK).setPopulation(11167325L).setAncestorIds("/6295630/3562981/6255149/").create();
        Location l2 = new LocationBuilder().setId(3562981).setPrimaryName("Republic of Cuba").setType(COUNTRY).setPopulation(11338138L).setAncestorIds("/6295630/6255149/").create();
        Location result = HeuristicDisambiguation.selectLocation(Arrays.asList(l1, l2));
        assertEquals(l2, result);
    }

    @Test
    public void test_chosesUnitOverLandmark() {
        Location l1 = new LocationBuilder().setId(5855799).setPrimaryName("Island of Hawai‘i").setType(LANDMARK).setPopulation(185079L).setAncestorIds("/6295630/6255149/6252001/5855797/5855765/").create();
        Location l2 = new LocationBuilder().setId(5855765).setPrimaryName("Hawaii County").setType(UNIT).setPopulation(200629L).setAncestorIds("/6295630/6255149/6252001/5855797/").create();
        Location l3 = new LocationBuilder().setId(5855797).setPrimaryName("Hawaii").setType(UNIT).setPopulation(1284220L).setAncestorIds("/6295630/6255149/6252001/").create();
        Location result = HeuristicDisambiguation.selectLocation(Arrays.asList(l1, l2, l3));
        assertEquals(l3, result);
    }

    // “Real world” test cases

    // Paris is a city in Logan County
    // Paris is a city in Arkansas
    // Paris is a city in the United States
    // Springfield is a town in Windsor County, Vermont, United States.
    // Dresden is an agricultural community in southwestern Ontario, Canada, part of the municipality of Chatham-Kent.
    // Stuttgart is an unincorporated community in Phillips County, Kansas, United States, founded on February 6, 1888.
    // London (also Ronton in Gilbertese) is the principal settlement on the atoll of Kiritimati (also known as Christmas Island) belonging to Kiribati in the Pacific Ocean.

}
