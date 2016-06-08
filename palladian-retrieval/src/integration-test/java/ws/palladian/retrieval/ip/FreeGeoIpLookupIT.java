package ws.palladian.retrieval.ip;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FreeGeoIpLookupIT {
	@Test
	public void testFreeGeoIpLookup() throws IpLookupException {
		FreeGeoIpLookup ipLookup = new FreeGeoIpLookup();
		IpLookupResult result = ipLookup.lookup("80.134.78.133");
		assertEquals("DE", result.getCountryCode());
		assertEquals("Germany", result.getCountryName());
		assertEquals("SN", result.getRegionCode());
		assertEquals("Saxony", result.getRegionName());
		assertEquals("Dresden", result.getCity());
		assertEquals("01097", result.getZipCode());
		assertEquals("Europe/Berlin", result.getTimeZone());
		assertEquals(51.071, result.getCoordinate().getLatitude(), 0.001);
		assertEquals(13.74, result.getCoordinate().getLongitude(), 0.001);
		assertEquals("0", result.getMetroCode());
		// System.out.println(result);
	}

}
