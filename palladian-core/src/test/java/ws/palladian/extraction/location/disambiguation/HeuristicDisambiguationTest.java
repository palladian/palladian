package ws.palladian.extraction.location.disambiguation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ws.palladian.extraction.location.LocationType.CITY;
import static ws.palladian.extraction.location.LocationType.CONTINENT;
import static ws.palladian.extraction.location.LocationType.POI;
import static ws.palladian.extraction.location.LocationType.UNIT;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationBuilder;

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
	public void test_chosesNestedLocationOverParent() {
		Location l1 = new LocationBuilder().setId(2968815).setPrimaryName("Paris").setType(UNIT)
				.setAncestorIds("/6295630/6255148/3017382/3012874/").create();
		Location l2 = new LocationBuilder().setId(2988506).setPrimaryName("Paris").setType(UNIT)
				.setAncestorIds("/6295630/6255148/3017382/3012874/2968815/").create();
		Location l3 = new LocationBuilder().setId(6455259).setPrimaryName("Paris").setType(UNIT)
				.setAncestorIds("/6295630/6255148/3017382/3012874/2968815/2988506/").create();
		Location result = HeuristicDisambiguation.selectLocation(Arrays.asList(l1, l2, l3));
		assertEquals(l3, result);
	}

	@Test
	public void test_chosesHigherPopulatedLocation() {
		Location l1 = new LocationBuilder().setId(6942553).setPrimaryName("Paris").setType(CITY).setPopulation(12310l)
				.create();
		Location l2 = new LocationBuilder().setId(2988507).setPrimaryName("Paris").setType(CITY).setPopulation(2190327l)
				.create();
		Location l3 = new LocationBuilder().setId(3703358).setPrimaryName("Paris").setType(CITY).setPopulation(894l)
				.create();
		Location result = HeuristicDisambiguation.selectLocation(Arrays.asList(l1, l2, l3));
		assertEquals(l2, result);
	}

	@Test
	public void test_chosesCityOverNonCity() {
		Location l1 = new LocationBuilder().setId(2968815).setPrimaryName("Paris").setType(UNIT).setPopulation(2165423L)
				.create();
		Location l2 = new LocationBuilder().setId(2988507).setPrimaryName("Paris").setType(CITY).setPopulation(2138551L)
				.create();
		Location l3 = new LocationBuilder().setId(6455259).setPrimaryName("Paris").setType(UNIT).setPopulation(2190327L)
				.create();
		Location result = HeuristicDisambiguation.selectLocation(Arrays.asList(l1, l2, l3));
		assertEquals(l2, result);
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
