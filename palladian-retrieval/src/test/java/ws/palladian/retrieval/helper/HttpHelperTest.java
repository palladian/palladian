package ws.palladian.retrieval.helper;

import static org.junit.Assert.assertEquals;
import static ws.palladian.retrieval.helper.HttpHelper.parseHeaderDate;

import org.junit.Test;

public class HttpHelperTest {
	
	@Test
	public void testParseDate() {
		assertEquals(1279804559000l, parseHeaderDate(false, "Thu, 22 Jul 2010 15:15:59GMT").getTime());
		assertEquals(1310144934000l ,parseHeaderDate(false, "Fri 08 Jul 2011 05:08:54 PM GMT GMT").getTime());
		// System.out.println(HttpHelper.parseHeaderDate(false, "Fri, Jul 08 2011 16:50:05 GMT"));
		// System.out.println(HttpHelper.parseHeaderDate(false, "GMT"));
	}

}
