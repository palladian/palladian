package ws.palladian.retrieval.ip;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import static org.hamcrest.CoreMatchers.is;

public class IpStackIpLookupIT {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testIpStackIpLookup() throws IpLookupException {
        IpStackIpLookup ipLookup = new IpStackIpLookup();
        IpLookupResult result = ipLookup.lookup("185.216.33.13");

        collector.checkThat(result.getCountryCode(), is("DE"));
        collector.checkThat(result.getCountryName(), is("Germany"));
        collector.checkThat(result.getRegionCode(), is("HE"));
        collector.checkThat(result.getRegionName(), is("Hesse"));
        collector.checkThat(result.getCity(), is("Frankfurt am Main"));
        collector.checkThat(result.getZipCode(), is("60314"));
        collector.checkThat(result.getCoordinate().getLatitude(), is(50.11370086669922));
        collector.checkThat(result.getCoordinate().getLongitude(), is(8.711899757385254));
    }
}
