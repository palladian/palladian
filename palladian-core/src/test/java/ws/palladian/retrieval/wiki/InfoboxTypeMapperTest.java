package ws.palladian.retrieval.wiki;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ws.palladian.extraction.location.LocationType.COUNTRY;
import static ws.palladian.retrieval.wiki.InfoboxTypeMapper.getConLLType;
import static ws.palladian.retrieval.wiki.InfoboxTypeMapper.getLocationType;

import org.junit.Test;

public class InfoboxTypeMapperTest {

    @Test
    public void testInfoboxTypeMapper() {
        assertNull(getConLLType("doesNotExist"));
        assertNull(getLocationType("doesNotExist"));
        assertEquals("LOC", getConLLType("country"));
        assertEquals(COUNTRY, getLocationType("country"));
    }

}
