package ws.palladian.classification.discretization;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Test;

import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.core.Instance;
import ws.palladian.helper.io.ResourceHelper;

public class DiscretizationTest {

    @Test
    public void testDiscretization_adultData() throws FileNotFoundException {
        File datasetFile = ResourceHelper.getResourceFile("/classifier/adultData.txt");
        Iterable<Instance> dataset = new CsvDatasetReader(datasetFile, false, ";");
        Discretization discretization = new Discretization(dataset);
        System.out.println(discretization);
    }

    @Test
    public void testDiscretization_diabetesData() throws FileNotFoundException {
        File datasetFile = ResourceHelper.getResourceFile("/classifier/diabetes2.csv");
        Iterable<Instance> dataset = new CsvDatasetReader(datasetFile, true, ";");
        Discretization discretization = new Discretization(dataset);
        System.out.println(discretization);
    }

    @Test
    public void testDiscretization_heartData() throws FileNotFoundException {
        File datasetFile = ResourceHelper.getResourceFile("/classifier/saheart.csv");
        Iterable<Instance> dataset = new CsvDatasetReader(datasetFile, true, ",");
        Discretization discretization = new Discretization(dataset);
        System.out.println(discretization);
    }

    @Test
    public void testDiscretization_wineData() throws FileNotFoundException {
        File datasetFile = ResourceHelper.getResourceFile("/classifier/wineData.txt");
        Iterable<Instance> dataset = new CsvDatasetReader(datasetFile, false, ";");
        Discretization discretization = new Discretization(dataset);
        System.out.println(discretization);
    }

}
