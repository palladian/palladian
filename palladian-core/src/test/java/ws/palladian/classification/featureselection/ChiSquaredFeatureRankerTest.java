package ws.palladian.classification.featureselection;

import static org.junit.Assert.assertEquals;
import static ws.palladian.helper.io.ResourceHelper.getResourceFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.core.Instance;
import ws.palladian.helper.ProgressMonitor;

public class ChiSquaredFeatureRankerTest {

    private static final double DELTA = 0.001;

    @Test
    public void testInformationGain_wineData_average() throws FileNotFoundException {
        File testFile = getResourceFile("/classifier/wineData.csv");
        List<Instance> dataset = new CsvDatasetReader(testFile).readAll();
        FeatureRanking result = new ChiSquaredFeatureRanker(new AverageMergingStrategy()).rankFeatures(dataset,
                new ProgressMonitor());
        // CollectionHelper.print(result.getAll());
        assertEquals(125.9867, result.getFeature("od280/od315ofDilutedWines").getScore(), DELTA);
        assertEquals(128.5980, result.getFeature("colorIntensity").getScore(), DELTA);
        assertEquals(124.7087, result.getFeature("proline").getScore(), DELTA);
        assertEquals(160.0524, result.getFeature("flavanoids").getScore(), DELTA);
        assertEquals(105.8876, result.getFeature("alcohol").getScore(), DELTA);
        assertEquals(95.2166, result.getFeature("hue").getScore(), DELTA);
        assertEquals(97.2884, result.getFeature("totalPhenols").getScore(), DELTA);
        assertEquals(62.9867, result.getFeature("alcalinityOfAsh").getScore(), DELTA);
        assertEquals(79.7351, result.getFeature("malicAcid").getScore(), DELTA);
        assertEquals(56.2174, result.getFeature("magnesium").getScore(), DELTA);
        assertEquals(60.1322, result.getFeature("proanthocyanins").getScore(), DELTA);
        assertEquals(35.6311, result.getFeature("ash").getScore(), DELTA);
        assertEquals(46.9911, result.getFeature("nonflavanoidPhenols").getScore(), DELTA);
    }

    @Test
    public void testInformationGain_wineData_roundRobin() throws FileNotFoundException {
        File testFile = getResourceFile("/classifier/wineData.csv");
        List<Instance> dataset = new CsvDatasetReader(testFile).readAll();
        FeatureRanking result = new ChiSquaredFeatureRanker(new RoundRobinMergingStrategy()).rankFeatures(dataset,
                new ProgressMonitor());
        // CollectionHelper.print(result.getAll());
        assertEquals(37, result.getFeature("od280/od315ofDilutedWines").getScore(), DELTA);
        assertEquals(37, result.getFeature("colorIntensity").getScore(), DELTA);
        assertEquals(37, result.getFeature("proline").getScore(), DELTA);
        assertEquals(36, result.getFeature("flavanoids").getScore(), DELTA);
        assertEquals(36, result.getFeature("alcohol").getScore(), DELTA);
        assertEquals(35, result.getFeature("hue").getScore(), DELTA);
        assertEquals(35, result.getFeature("totalPhenols").getScore(), DELTA);
        assertEquals(33, result.getFeature("alcalinityOfAsh").getScore(), DELTA);
        assertEquals(32, result.getFeature("malicAcid").getScore(), DELTA);
        assertEquals(32, result.getFeature("magnesium").getScore(), DELTA);
        assertEquals(30, result.getFeature("proanthocyanins").getScore(), DELTA);
        assertEquals(28, result.getFeature("ash").getScore(), DELTA);
        assertEquals(23, result.getFeature("nonflavanoidPhenols").getScore(), DELTA);
    }

}
