package ws.palladian.helper.io;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class CsvHelperTest {

    // TODO : http://programmers.stackexchange.com/questions/65126/unit-tests-for-a-csv-parser
    @Test
    public void testCsvSplit() {
        String line = "\"2010-06-20\";192171;\"2010-06-20_Three-dead-in-murder-suicide-shooting-at-Southern-California-fast-food-restaurant/4e24a23cccbd61b4e9f945558e98389e0ccf7ea0\";\"http://www.latimes.com/news/local/la-me-del-taco-shoot-20100620,0,2740994.story\";200;\"Gunman targets stepdaughter's family in San Bernardino restaurant; 3 dead\";";
        List<String> split = CsvHelper.splitCsvLine(line, ';');
        // CollectionHelper.print(split);
        assertEquals(6, split.size());
        assertEquals("2010-06-20", split.get(0));
        assertEquals("192171", split.get(1));
        assertEquals(
                "2010-06-20_Three-dead-in-murder-suicide-shooting-at-Southern-California-fast-food-restaurant/4e24a23cccbd61b4e9f945558e98389e0ccf7ea0",
                split.get(2));
        assertEquals("http://www.latimes.com/news/local/la-me-del-taco-shoot-20100620,0,2740994.story", split.get(3));
        assertEquals("200", split.get(4));
        assertEquals("Gunman targets stepdaughter's family in San Bernardino restaurant; 3 dead", split.get(5));

        line = "\"2010-01-01\";146923;\"2010-01-01_Afghanistan--two-kidnapped-French-journalists-believed-to-be-in-good-health/be2bd7f7467ec83d24a92ba72fc90c9c5a576857\";\"http://www.france24.com/en/20100101-france-afghanistan-journalists-kidnapped-alive-good-health-kapisa\";200;\"Abducted French journalists believed to be in good health\";\"2010-01-01\"";
        split = CsvHelper.splitCsvLine(line, ';');
        // CollectionHelper.print(split);
        assertEquals(7, split.size());
        assertEquals("2010-01-01", split.get(6));
    }

}
