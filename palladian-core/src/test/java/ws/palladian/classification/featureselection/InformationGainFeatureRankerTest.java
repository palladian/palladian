package ws.palladian.classification.featureselection;

import static org.junit.Assert.assertEquals;
import static ws.palladian.helper.io.ResourceHelper.getResourceFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.core.Instance;

public class InformationGainFeatureRankerTest {

    private static final double DELTA = 0.001;

    @Test
    public void testInformationGain_playData() throws FileNotFoundException {
        File testFile = getResourceFile("/classifier/playData.txt");
        List<Instance> dataset = new CsvDatasetReader(testFile, true).readAll();
        FeatureRanking result = new InformationGainFeatureRanker().rankFeatures(dataset);
        // values verified with Weka
        assertEquals(0.2467, result.getFeature("Outlook").getScore(), DELTA);
        assertEquals(0.1518, result.getFeature("Humidity").getScore(), DELTA);
        assertEquals(0.0481, result.getFeature("Windy").getScore(), DELTA);
        assertEquals(0.0292, result.getFeature("Temperature").getScore(), DELTA);
    }

    @Test
    public void testInformationGain_wineData() throws FileNotFoundException {
        File testFile = getResourceFile("/classifier/wineData.csv");
        List<Instance> dataset = new CsvDatasetReader(testFile).readAll();
        FeatureRanking result = new InformationGainFeatureRanker().rankFeatures(dataset);
        assertEquals(1.0151, result.getFeature("flavanoids").getScore(), DELTA);
        assertEquals(0.8278, result.getFeature("proline").getScore(), DELTA);
        assertEquals(0.7438, result.getFeature("colorIntensity").getScore(), DELTA);
        assertEquals(0.7221, result.getFeature("od280/od315ofDilutedWines").getScore(), DELTA);
        assertEquals(0.6324, result.getFeature("hue").getScore(), DELTA);
        assertEquals(0.6034, result.getFeature("alcohol").getScore(), DELTA);
        assertEquals(0.5795, result.getFeature("totalPhenols").getScore(), DELTA);
        assertEquals(0.4306, result.getFeature("malicAcid").getScore(), DELTA);
        assertEquals(0.2772, result.getFeature("alcalinityOfAsh").getScore(), DELTA);
        assertEquals(0.2653, result.getFeature("proanthocyanins").getScore(), DELTA);
        assertEquals(0.2614, result.getFeature("magnesium").getScore(), DELTA);
        assertEquals(0.2198, result.getFeature("nonflavanoidPhenols").getScore(), DELTA);
        assertEquals(0.1649, result.getFeature("ash").getScore(), DELTA);
    }

}
