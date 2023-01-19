package ws.palladian.helper.io;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DelimitedStringHelperTest {

    @Test
    public void testSplit() {
        String line = "'a','b','c'";
        List<String> split = DelimitedStringHelper.splitLine(line, ',', '\'');
        assertEquals(Arrays.asList("a", "b", "c"), split);
    }

    @Test
    public void testSplitWithQuotes() {
        String line = "'a','b,c','d'";
        List<String> split = DelimitedStringHelper.splitLine(line, ',', '\'');
        assertEquals(Arrays.asList("a", "b,c", "d"), split);
    }

    @Test
    public void testSplitUnbalanced() {
        String line = "'a','b','c";
        List<String> split = DelimitedStringHelper.splitLine(line, ',', '\'');
        assertNull(split);
    }

    @Test
    public void testSplitDoubleEscaped() {
        String line = "'test ''in quotes''','test'";
        List<String> split = DelimitedStringHelper.splitLine(line, ',', '\'', true);
        assertEquals(Arrays.asList("test 'in quotes'", "test"), split);
    }

}
