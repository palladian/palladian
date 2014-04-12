package ws.palladian.classification.featureselection;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Test;

import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.core.Instance;
import ws.palladian.helper.io.ResourceHelper;

public class InformationGainTest {

    @Test
    public void testInformationGain() throws FileNotFoundException {
        File testFile = ResourceHelper.getResourceFile("/classifier/playData.txt");
        Iterable<Instance> dataset = new CsvDatasetReader(testFile, true);
        InformationGainFormula informationGain = new InformationGainFormula(dataset);
        // values verified with Weka
        assertEquals(0.2467, informationGain.calculateGain("Outlook"), 0.001);
        assertEquals(0.1518, informationGain.calculateGain("Humidity"), 0.001);
        assertEquals(0.0481, informationGain.calculateGain("Windy"), 0.001);
        assertEquals(0.0292, informationGain.calculateGain("Temperature"), 0.001);
    }

}
